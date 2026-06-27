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
    $verifiedBy = $input['verified_by'] ?? '';

    $stmt = $db->prepare("UPDATE meter_records SET is_verified = 1, verified_by = ? WHERE record_id = ?");
    $stmt->execute([$verifiedBy, $recordId]);

    if ($stmt->rowCount() > 0) {
        echo json_encode(["success" => true, "message" => "Pekerjaan berhasil diverifikasi"]);
    } else {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Record tidak ditemukan"]);
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
