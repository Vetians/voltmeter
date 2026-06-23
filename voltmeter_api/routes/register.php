<?php
require_once __DIR__ . '/../config/database.php';

$input = json_decode(file_get_contents('php://input'), true);

$name = $input['name'] ?? '';
$username = $input['username'] ?? '';
$password = $input['password'] ?? '';
$role = $input['role'] ?? 'surveyor';

if (empty($name) || empty($username) || empty($password)) {
    http_response_code(400);
    echo json_encode(['error' => 'Nama, username, dan password harus diisi']);
    exit;
}

$db = getDB();

// Cek username sudah ada atau belum
$stmt = $db->prepare("SELECT id FROM users WHERE username = ?");
$stmt->execute([$username]);
if ($stmt->fetch()) {
    http_response_code(409);
    echo json_encode(['error' => 'Username sudah digunakan']);
    exit;
}

// Generate user_id baru
$userId = 'USR' . date('YmdHis');

// Insert user baru dengan password hash
$stmt = $db->prepare("INSERT INTO users (user_id, username, password, full_name, role) VALUES (?, ?, ?, ?, ?)");
$stmt->execute([
    $userId,
    $username,
    password_hash($password, PASSWORD_DEFAULT),
    $name,
    $role
]);

http_response_code(201);
echo json_encode([
    'success' => true,
    'message' => 'User berhasil dibuat',
    'user_id' => $userId
]);
