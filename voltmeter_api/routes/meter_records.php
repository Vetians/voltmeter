<?php
// error_reporting(E_ALL);
// ini_set('display_errors', 1);

header('Content-Type: application/json');

require_once __DIR__ . '/../config/database.php';

try {

    $db = getDB();

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {

        $input = json_decode(file_get_contents('php://input'), true);

        if (!$input) {
            throw new Exception("Request JSON tidak valid.");
        }

        $recordId = 'REC-' . date('YmdHis') . '-' . strtoupper(substr(md5(uniqid()), 0, 6));

        $previousReading = (float)($input['previous_reading'] ?? 0);
        $currentReading  = (float)($input['current_reading'] ?? 0);
        $usageKwh        = $currentReading - $previousReading;

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
            (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

            $stmt2 = $db->prepare("
                UPDATE meters
                SET last_reading = ?
                WHERE customer_id = ? AND meter_number = ?
            ");

            $stmt2->execute([
                $currentReading,
                $input['customer_id'],
                $input['meter_number']
            ]);
        }

        http_response_code(201);

        echo json_encode([
            "success" => true,
            "message" => "Data meter berhasil disimpan.",
            "record_id" => $recordId
        ]);

    } else {

        $customerId = $_GET['customer_id'] ?? null;

        if ($customerId) {

            $stmt = $db->prepare("
                SELECT *
                FROM meter_records
                WHERE customer_id = ?
                ORDER BY record_date DESC, record_time DESC
                LIMIT 50
            ");

            $stmt->execute([$customerId]);

        } else {

            $stmt = $db->prepare("
                SELECT *
                FROM meter_records
                ORDER BY record_date DESC, record_time DESC
                LIMIT 50
            ");

            $stmt->execute();
        }

        echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));
    }

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "error_type" => "Database Error",
        "message" => $e->getMessage()
    ]);

} catch (Exception $e) {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "error_type" => "Application Error",
        "message" => $e->getMessage()
    ]);

} catch (Throwable $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "error_type" => "Unexpected Error",
        "message" => $e->getMessage()
    ]);
}