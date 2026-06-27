<?php
header('Content-Type: application/json');
require_once __DIR__ . '/../config/database.php';

$db = getDB();
$verified = isset($_GET['verified']) ? (int)$_GET['verified'] : 0;
$recordedBy = $_GET['recorded_by'] ?? null;

$sql = "
    SELECT mr.*, c.name AS customer_name, c.address AS customer_address
    FROM meter_records mr
    JOIN customers c ON c.customer_id = mr.customer_id
    WHERE mr.is_verified = ?
";
$params = [$verified];

if ($recordedBy) {
    $sql .= " AND mr.recorded_by = ?";
    $params[] = $recordedBy;
}

$sql .= " ORDER BY mr.record_date DESC, mr.record_time DESC";

$stmt = $db->prepare($sql);
$stmt->execute($params);
echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));
