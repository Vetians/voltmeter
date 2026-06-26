<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

header('Content-Type: application/json');

require_once __DIR__ . '/../config/database.php';

$db = getDB();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);

    $recordId = 'REC-' . date('YmdHis') . '-' . strtoupper(substr(md5(uniqid()), 0, 6));

    $previousReading = (float)($input['previous_reading'] ?? 0);
    $currentReading = (float)($input['current_reading'] ?? 0);
    $usageKwh = $currentReading - $previousReading;

    $stmt = $db->prepare("
    INSERT INTO meter_records
    (
    record_id,
    customer_id,
    meter_number,
    previous_reading,
    current_reading,
    usage_kwh,
    record_date,
    record_time,
    visit_status,
    photo_path,
    latitude,
    longitude,
    notes,
    recorded_by
    )
    VALUES
    (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    ");

    $stmt->execute([
        $recordId,
        $input['customer_id'] ?? '',
        $input['meter_number'] ?? '',
        $previousReading,
        $currentReading,
        $usageKwh,
        $input['record_date'] ?? date('Y-m-d'),
        $input['record_time'] ?? date('H:i:s'),
        $input['visit_status'] ?? 'TERBACA_NORMAL',
        $input['photo_path'] ?? '',
        $input['latitude'] ?? 0,
        $input['longitude'] ?? 0,
        $input['notes'] ?? '',
        $input['recorded_by'] ?? ''
    ]);

    if (!empty($input['meter_number']) && !empty($input['customer_id'])) {
        $stmt2 = $db->prepare("UPDATE meters SET last_reading = ? WHERE customer_id = ? AND meter_number = ?");
        $stmt2->execute([$currentReading, $input['customer_id'], $input['meter_number']]);
    }

    echo json_encode(['success' => true, 'record_id' => $recordId]);

} else {
    $customerId = $_GET['customer_id'] ?? null;

    if ($customerId) {
        $stmt = $db->prepare("SELECT * FROM meter_records WHERE customer_id = ? ORDER BY record_date DESC, record_time DESC LIMIT 50");
        $stmt->execute([$customerId]);
    } else {
        $stmt = $db->prepare("SELECT * FROM meter_records ORDER BY record_date DESC, record_time DESC LIMIT 50");
        $stmt->execute();
    }

    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode($records);
}
