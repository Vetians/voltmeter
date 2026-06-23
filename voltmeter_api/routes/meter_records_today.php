<?php
require_once __DIR__ . '/../config/database.php';

$db = getDB();
$today = date('Y-m-d');

$stmt = $db->prepare("SELECT * FROM meter_records WHERE record_date = ? ORDER BY record_time DESC");
$stmt->execute([$today]);
$records = $stmt->fetchAll();

echo json_encode($records);
