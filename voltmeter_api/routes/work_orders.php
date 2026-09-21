<?php
require_once __DIR__ . '/../config/database.php';

$db = getDB();

$currentMonth = (int) date('m');
$currentYear = (int) date('Y');

// Get active work orders for current month
$stmt = $db->prepare("
    SELECT wo.*, c.customer_id, c.name, c.address, c.power_va, c.tariff, 
           c.last_month_usage, c.last_meter_reading, c.latitude, c.longitude
    FROM work_orders wo
    JOIN customers c ON c.work_order_id = wo.work_order_id
    WHERE wo.month = ? AND wo.year = ? AND wo.status = 'active'
");
$stmt->execute([$currentMonth, $currentYear]);
$rows = $stmt->fetchAll();

// Fallback: if no work orders for current month, get the most recent active work orders
if (empty($rows)) {
    $stmtFallback = $db->prepare("
        SELECT wo.*, c.customer_id, c.name, c.address, c.power_va, c.tariff, 
               c.last_month_usage, c.last_meter_reading, c.latitude, c.longitude
        FROM work_orders wo
        JOIN customers c ON c.work_order_id = wo.work_order_id
        WHERE wo.status = 'active'
        ORDER BY wo.year DESC, wo.month DESC
    ");
    $stmtFallback->execute();
    $rows = $stmtFallback->fetchAll();
}

// Group by work_order_id
$workOrders = [];
foreach ($rows as $row) {
    $woId = $row['work_order_id'];
    if (!isset($workOrders[$woId])) {
        $workOrders[$woId] = [
            'work_order_id' => $woId,
            'customers' => []
        ];
    }
    
    // Get meters for this customer
    $stmtMeter = $db->prepare("SELECT * FROM meters WHERE customer_id = ? ORDER BY meter_index ASC");
    $stmtMeter->execute([$row['customer_id']]);
    $meters = $stmtMeter->fetchAll();
    
    $metersData = [];
    foreach ($meters as $m) {
        // Get monthly verification status for THIS specific meter
        $stmtMeterStatus = $db->prepare("
            SELECT verification_status FROM meter_records 
            WHERE customer_id = ? AND meter_number = ? AND MONTH(record_date) = ? AND YEAR(record_date) = ?
            ORDER BY created_at DESC LIMIT 1
        ");
        $stmtMeterStatus->execute([$row['customer_id'], $m['meter_number'], $currentMonth, $currentYear]);
        $meterStatusRow = $stmtMeterStatus->fetch();
        $meterMonthlyStatus = $meterStatusRow ? $meterStatusRow['verification_status'] : null;

        $metersData[] = [
            'meter_number' => $m['meter_number'],
            'last_reading' => (float) $m['last_reading'],
            'monthly_status' => $meterMonthlyStatus
        ];
    }

    // Compute overall customer status from meters
    $customerMonthlyStatus = null;
    foreach ($metersData as $md) {
        if ($md['monthly_status'] === 'PENDING') {
            $customerMonthlyStatus = 'PENDING';
            break;
        } elseif ($md['monthly_status'] === 'VERIFIED') {
            $customerMonthlyStatus = 'VERIFIED';
        } elseif ($md['monthly_status'] === 'REJECTED' && $customerMonthlyStatus !== 'VERIFIED') {
            $customerMonthlyStatus = 'REJECTED';
        }
    }
    
    $workOrders[$woId]['customers'][] = [
        'customer_id' => $row['customer_id'],
        'name' => $row['name'],
        'address' => $row['address'],
        'power_va' => (int) $row['power_va'],
        'tariff' => $row['tariff'],
        'last_month_usage' => (float) $row['last_month_usage'],
        'last_meter_reading' => (float) $row['last_meter_reading'],
        'meters' => $metersData,
        'latitude' => (float) $row['latitude'],
        'longitude' => (float) $row['longitude'],
        'monthly_status' => $customerMonthlyStatus
    ];
}

echo json_encode(array_values($workOrders));
