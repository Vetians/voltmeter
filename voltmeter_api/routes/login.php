<?php
require_once __DIR__ . '/../config/database.php';

$input = json_decode(file_get_contents('php://input'), true);
$username = $input['username'] ?? '';
$password = $input['password'] ?? '';

if (empty($username) || empty($password)) {
    http_response_code(400);
    echo json_encode(['error' => 'Username dan password harus diisi']);
    exit;
}

$db = getDB();
$stmt = $db->prepare("SELECT * FROM users WHERE username = ?");
$stmt->execute([$username]);
$user = $stmt->fetch();

if (!$user || !password_verify($password, $user['password'])) {
    http_response_code(401);
    echo json_encode(['error' => 'Username atau password salah']);
    exit;
}

echo json_encode([
    'id' => $user['id'],
    'user_id' => $user['user_id'],
    'name' => $user['full_name'],
    'username' => $user['username'],
    'role' => $user['role'],
    'token' => 'token_' . $user['user_id'] . '_' . time()
]);
