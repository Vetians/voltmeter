<?php
require_once __DIR__ . '/../config/database.php';

$db = getDB();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);
    
    $stmt = $db->prepare("INSERT INTO users (user_id, username, password, full_name, role) VALUES (?, ?, ?, ?, ?)");
    $stmt->execute([
        'USR' . date('YmdHis'),
        $input['username'],
        password_hash($input['password'], PASSWORD_DEFAULT),
        $input['name'],
        $input['role'] ?? 'surveyor'
    ]);
    
    http_response_code(201);
    echo json_encode(['success' => true]);
} else {
    $stmt = $db->prepare("SELECT id, user_id, full_name AS name, username, role FROM users");
    $stmt->execute();
    echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));
}
