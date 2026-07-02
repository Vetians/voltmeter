<?php
require_once __DIR__ . '/../config/database.php';

$db = getDB();

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"));

    if (
        empty($data->customer_id) || empty($data->name) || empty($data->address) ||
        empty($data->power_va) || empty($data->tariff) ||
        empty($data->meters) || !is_array($data->meters) || count($data->meters) === 0
    ) {
        http_response_code(400);
        echo json_encode(["message" => "Data tidak lengkap."]);
        exit();
    }

    $customerId = $data->customer_id;
    $meterNumber = $data->meters[0]->meter_number;

    $workOrderId = $data->work_order_id ?? '';
    if (empty($workOrderId)) {
        $currentMonth = (int) date('m');
        $currentYear = (int) date('Y');
        $stmtWo = $db->prepare("SELECT work_order_id FROM work_orders WHERE month = ? AND year = ? AND status = 'active' LIMIT 1");
        $stmtWo->execute([$currentMonth, $currentYear]);
        $wo = $stmtWo->fetch();
        if ($wo) {
            $workOrderId = $wo['work_order_id'];
        } else {
            $stmtWoFallback = $db->prepare("SELECT work_order_id FROM work_orders WHERE status = 'active' ORDER BY year DESC, month DESC LIMIT 1");
            $stmtWoFallback->execute();
            $woFallback = $stmtWoFallback->fetch();
            if ($woFallback) {
                $workOrderId = $woFallback['work_order_id'];
            } else {
                http_response_code(400);
                echo json_encode(["message" => "Gagal: Tidak ada Work Order aktif."]);
                exit();
            }
        }
    }

    try {
        $db->beginTransaction();

        $stmtCustomer = $db->prepare("INSERT INTO customers (customer_id, work_order_id, name, address, power_va, tariff, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        $lat = isset($data->latitude) ? $data->latitude : 0.0;
        $lng = isset($data->longitude) ? $data->longitude : 0.0;

        $stmtCustomer->execute([
            $customerId,
            $workOrderId,
            $data->name,
            $data->address,
            $data->power_va,
            $data->tariff,
            $lat,
            $lng
        ]);

        $stmtMeter = $db->prepare("INSERT INTO meters (meter_number, customer_id, last_reading) VALUES (?, ?, ?)");
        $lastReading = isset($data->meters[0]->last_reading) ? $data->meters[0]->last_reading : 0;
        $stmtMeter->execute([$meterNumber, $customerId, $lastReading]);

        $db->commit();
        http_response_code(201);
        echo json_encode(["success" => true, "message" => "Customer berhasil ditambahkan"]);
    } catch (PDOException $e) {
        $db->rollBack();
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} else if ($method === 'PUT') {
    $data = json_decode(file_get_contents("php://input"));

    if (empty($data->customer_id)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "customer_id harus diisi"]);
        exit();
    }

    try {
        $stmt = $db->prepare("UPDATE customers SET name = ?, address = ?, power_va = ?, tariff = ?, latitude = ?, longitude = ? WHERE customer_id = ?");
        $stmt->execute([
            $data->name ?? '',
            $data->address ?? '',
            $data->power_va ?? 0,
            $data->tariff ?? '',
            $data->latitude ?? 0.0,
            $data->longitude ?? 0.0,
            $data->customer_id
        ]);
        echo json_encode(["success" => true, "message" => "Customer berhasil diperbarui"]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} else if ($method === 'DELETE') {
    $customerId = $_GET['customer_id'] ?? null;

    if (empty($customerId)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "customer_id harus diisi"]);
        exit();
    }

    try {
        $db->beginTransaction();
        $stmt = $db->prepare("DELETE FROM customers WHERE customer_id = ?");
        $stmt->execute([$customerId]);
        $db->commit();
        echo json_encode(["success" => true, "message" => "Customer berhasil dihapus"]);
    } catch (PDOException $e) {
        $db->rollBack();
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} else {
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

    // Fallback: if no customers for current month, get from most recent active work orders
    if (empty($customers)) {
        $stmtFallback = $db->prepare("
            SELECT c.*, wo.work_order_id
            FROM customers c
            JOIN work_orders wo ON wo.work_order_id = c.work_order_id
            WHERE wo.status = 'active'
            ORDER BY wo.year DESC, wo.month DESC
        ");
        $stmtFallback->execute();
        $customers = $stmtFallback->fetchAll();
    }

    $result = [];
    foreach ($customers as $customer) {
        $stmtMeter = $db->prepare("SELECT * FROM meters WHERE customer_id = ? ORDER BY meter_index ASC");
        $stmtMeter->execute([$customer['customer_id']]);
        $meters = $stmtMeter->fetchAll();

        $metersData = [];
        foreach ($meters as $m) {
            $stmtMeterStatus = $db->prepare("
                SELECT verification_status FROM meter_records 
                WHERE customer_id = ? AND meter_number = ? AND MONTH(record_date) = ? AND YEAR(record_date) = ?
                ORDER BY created_at DESC LIMIT 1
            ");
            $stmtMeterStatus->execute([$customer['customer_id'], $m['meter_number'], $currentMonth, $currentYear]);
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
            'longitude' => (float) $customer['longitude'],
            'monthly_status' => $customerMonthlyStatus
        ];
    }
    echo json_encode($result);
}
