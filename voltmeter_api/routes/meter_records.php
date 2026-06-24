<?php
require_once __DIR__ . '/../config/database.php';

$db = getDB();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);
    
    $recordId = 'REC-' . date('YmdHis') . '-' . strtoupper(substr(md5(uniqid()), 0, 6));
    
    $previousReading = $input['previous_reading'] ?? 0;
    $currentReading = $input['current_reading'];
    $usageKwh = $currentReading - $previousReading;
    
    $stmt = $db->prepare("
        INSERT INTO meter_records 
        (record_id, customer_id, meter_number, previous_reading, current_reading, usage_kwh,
         record_date, record_time, visit_status, photo_path, latitude, longitude, notes)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ");
    
    $stmt->execute([
        $recordId,
        $input['customer_id'],
        $input['meter_number'] ?? '',
        $previousReading,
        $currentReading,
        $usageKwh,
        $input['record_date'] ?? date('Y-m-d'),
        $input['record_time'] ?? date('H:i:s'),
        $input['visit_status'] ?? 'TERBACA_NORMAL',
        $input['photo_path'] ?? null,
        $input['latitude'] ?? 0,
        $input['longitude'] ?? 0,
        $input['notes'] ?? ''
    ]);
    
    // Update meter last reading
    if (!empty($input['meter_number'])) {
        $stmt = $db->prepare("UPDATE meters SET last_reading = ? WHERE customer_id = ? AND meter_number = ?");
        $stmt->execute([$currentReading, $input['customer_id'], $input['meter_number']]);
    }
    
    http_response_code(201);
    echo json_encode(['success' => true, 'record_id' => $recordId]);
    
} else {
    // GET - List all records
    $stmt = $db->prepare("SELECT * FROM meter_records ORDER BY record_date DESC, record_time DESC LIMIT 50");
    $stmt->execute();
    $records = $stmt->fetchAll();
    echo json_encode($records);
}
