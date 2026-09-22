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

    if (!in_array($status, ['VERIFIED', 'REJECTED'], true)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Status verifikasi tidak valid."]);
        exit;
    }

    try {
        // Update database
        $query = "UPDATE meter_records SET verification_status = :status, verification_note = :note WHERE record_id = :id AND verification_status = 'PENDING'";
        $stmt = $db->prepare($query);
        
        $stmt->bindParam(':status', $status);
        $stmt->bindParam(':note', $note);
        $stmt->bindParam(':id', $record_id);

        if ($stmt->execute() && $stmt->rowCount() === 1) {
            // Stand terakhir hanya boleh maju jika record disetujui. Untuk
            // penolakan, kembalikan ke previous_reading agar input ulang memakai
            // stand bulan lalu yang benar (juga memperbaiki data lama yang sempat maju).
            $recordStmt = $db->prepare("SELECT customer_id, meter_number, previous_reading, current_reading FROM meter_records WHERE record_id = ?");
            $recordStmt->execute([$record_id]);
            $record = $recordStmt->fetch(PDO::FETCH_ASSOC);
            if ($record && !empty($record['meter_number'])) {
                $reading = $status === 'VERIFIED' ? $record['current_reading'] : $record['previous_reading'];
                $meterStmt = $db->prepare("UPDATE meters SET last_reading = ? WHERE customer_id = ? AND meter_number = ?");
                $meterStmt->execute([$reading, $record['customer_id'], $record['meter_number']]);
            }
            http_response_code(200);
            echo json_encode([
                "success" => true,
                "message" => "Status verifikasi berhasil diperbarui."
            ]);
        } else {
            http_response_code(409);
            echo json_encode([
                "success" => false,
                "message" => "Record tidak ditemukan atau sudah diproses."
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
