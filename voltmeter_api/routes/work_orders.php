<?php
require_once __DIR__ . '/../config/database.php';

$db = getDB();

$currentMonth = (int) date('m');
$currentYear = (int) date('Y');

// Get active work orders
$stmt = $db->prepare("
    SELECT wo.*, c.customer_id, c.name, c.address, c.power_va, c.tariff, 
           c.last_month_usage, c.last_meter_reading, c.latitude, c.longitude
    FROM work_orders wo
    JOIN customers c ON c.work_order_id = wo.work_order_id
    WHERE wo.month = ? AND wo.year = ? AND wo.status = 'active'
");
$stmt->execute([$currentMonth, $currentYear]);
$rows = $stmt->fetchAll();

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
        $metersData[] = [
            'meter_number' => $m['meter_number'],
            'last_reading' => (float) $m['last_reading']
        ];
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
        'longitude' => (float) $row['longitude']
    ];
}

echo json_encode(array_values($workOrders));
