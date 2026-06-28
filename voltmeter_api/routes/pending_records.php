<?php
header('Content-Type: application/json');
require_once __DIR__ . '/../config/database.php';

$db = getDB();
$recordedBy = $_GET['recorded_by'] ?? null;

// Support both old 'verified' param (0=PENDING,1=VERIFIED,2=REJECTED) and new 'status' param
$statusParam = $_GET['status'] ?? null;
$verified = isset($_GET['verified']) ? (int)$_GET['verified'] : null;

if ($statusParam) {
    $statusValue = $statusParam;
} elseif ($verified !== null) {
    $map = [0 => 'PENDING', 1 => 'VERIFIED', 2 => 'REJECTED'];
    $statusValue = $map[$verified] ?? 'PENDING';
} else {
    $statusValue = 'PENDING';
}

$sql = "
    SELECT mr.*, c.name AS customer_name, c.address AS customer_address
    FROM meter_records mr
    JOIN customers c ON c.customer_id = mr.customer_id
    WHERE mr.verification_status = ?
";
$params = [$statusValue];

if ($recordedBy) {
    $sql .= " AND mr.recorded_by = ?";
    $params[] = $recordedBy;
}

$sql .= " ORDER BY mr.record_date DESC, mr.record_time DESC";

$stmt = $db->prepare($sql);
$stmt->execute($params);
echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));
