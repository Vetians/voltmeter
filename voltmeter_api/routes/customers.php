<?php
require_once __DIR__ . '/../config/database.php';

$db = getDB();

$currentMonth = (int) date('m');
$currentYear = (int) date('Y');

$stmt = $db->prepare("
    SELECT c.*, wo.work_order_id
    FROM customers c
    JOIN work_orders wo ON wo.work_order_id = c.work_order_id
    WHERE wo.month = ? AND wo.year = ? AND wo.status = 'active'
");
$stmt->execute([$currentMonth, $currentYear]);
$customers = $stmt->fetchAll();

$result = [];
foreach ($customers as $customer) {
    $stmtMeter = $db->prepare("SELECT * FROM meters WHERE customer_id = ? ORDER BY meter_index ASC");
    $stmtMeter->execute([$customer['customer_id']]);
    $meters = $stmtMeter->fetchAll();
    
    $metersData = [];
    foreach ($meters as $m) {
        $metersData[] = [
            'meter_number' => $m['meter_number'],
            'last_reading' => (float) $m['last_reading']
        ];
    }
    
    $result[] = [
        'customer_id' => $customer['customer_id'],
        'name' => $customer['name'],
        'address' => $customer['address'],
        'power_va' => (int) $customer['power_va'],
        'tariff' => $customer['tariff'],
        'last_month_usage' => (float) $customer['last_month_usage'],
        'last_meter_reading' => (float) $customer['last_meter_reading'],
        'meters' => $metersData,
        'latitude' => (float) $customer['latitude'],
        'longitude' => (float) $customer['longitude']
    ];
}

echo json_encode($result);
