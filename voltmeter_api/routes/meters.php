<?php
require_once __DIR__ . '/../config/database.php';

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS");
header("Content-Type: application/json");

$db = getDB();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);
    
    $customerId = $input['customer_id'] ?? null;
    $meterNumber = $input['meter_number'] ?? null;
    
    if (!$customerId || !$meterNumber) {
        http_response_code(400);
        echo json_encode(['success' => false, 'error' => 'customer_id dan meter_number wajib diisi']);
        exit;
    }
    
    $stmt = $db->prepare("INSERT INTO meters (customer_id, meter_number, last_reading) VALUES (?, ?, ?)");
    $stmt->execute([$customerId, $meterNumber, 0]);
    
    http_response_code(201);
    echo json_encode(['success' => true]);
    
} else if ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
    $meterNumber = $_GET['meter_number'] ?? null;
    
    if (!$meterNumber) {
        http_response_code(400);
        echo json_encode(['success' => false, 'error' => 'Parameter meter_number wajib diisi']);
        exit;
    }
    
    $stmt = $db->prepare("DELETE FROM meters WHERE meter_number = ?");
    $stmt->execute([$meterNumber]);
    
    echo json_encode(['success' => true]);
    
} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'error' => 'Method tidak diizinkan']);
}
