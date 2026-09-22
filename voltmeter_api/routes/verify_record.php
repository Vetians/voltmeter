<?php
header('Content-Type: application/json');
require_once __DIR__ . '/../config/database.php';

try {
    $db = getDB();
    $input = json_decode(file_get_contents('php://input'), true);

    if (!$input || empty($input['record_id'])) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "record_id diperlukan"]);
        exit;
    }

    $recordId = $input['record_id'];
    $status = $input['status'] ?? 'VERIFIED';
    $note = $input['note'] ?? null;

    if (!in_array($status, ['VERIFIED', 'REJECTED'], true)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Status verifikasi tidak valid"]);
        exit;
    }

    $stmt = $db->prepare("UPDATE meter_records SET verification_status = ?, verification_note = ? WHERE record_id = ? AND verification_status = 'PENDING'");
    $stmt->execute([$status, $note, $recordId]);

    if ($stmt->rowCount() > 0) {
        $recordStmt = $db->prepare("SELECT customer_id, meter_number, previous_reading, current_reading FROM meter_records WHERE record_id = ?");
        $recordStmt->execute([$recordId]);
        $record = $recordStmt->fetch(PDO::FETCH_ASSOC);
        if ($record && !empty($record['meter_number'])) {
            $reading = $status === 'VERIFIED' ? $record['current_reading'] : $record['previous_reading'];
            $meterStmt = $db->prepare("UPDATE meters SET last_reading = ? WHERE customer_id = ? AND meter_number = ?");
            $meterStmt->execute([$reading, $record['customer_id'], $record['meter_number']]);
        }
        echo json_encode(["success" => true, "message" => "Status verifikasi berhasil diperbarui"]);
    } else {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Record tidak ditemukan"]);
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
