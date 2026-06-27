<?php
// Izinkan akses API dari mana saja
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once __DIR__ . '/../config/database.php';

// Pastikan request adalah POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["message" => "Method Not Allowed"]);
    exit();
}

$db = getDB();
$data = json_decode(file_get_contents("php://input"));

// Validasi input
if (!empty($data->record_id) && !empty($data->status)) {
    
    $record_id = $data->record_id;
    $status = $data->status; // 'VERIFIED' atau 'REJECTED'
    $note = isset($data->note) ? $data->note : null;

    try {
        // Update database
        $query = "UPDATE meter_records SET verification_status = :status, verification_note = :note WHERE record_id = :id";
        $stmt = $db->prepare($query);
        
        $stmt->bindParam(':status', $status);
        $stmt->bindParam(':note', $note);
        $stmt->bindParam(':id', $record_id);

        if ($stmt->execute()) {
            http_response_code(200);
            echo json_encode([
                "success" => true,
                "message" => "Status verifikasi berhasil diperbarui."
            ]);
        } else {
            http_response_code(503);
            echo json_encode([
                "success" => false,
                "message" => "Gagal memperbarui status verifikasi."
            ]);
        }
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            "success" => false,
            "message" => "Database Error: " . $e->getMessage()
        ]);
    }
} else {
    http_response_code(400);
    echo json_encode([
        "success" => false,
        "message" => "Data tidak lengkap. record_id dan status wajib diisi."
    ]);
}
?>