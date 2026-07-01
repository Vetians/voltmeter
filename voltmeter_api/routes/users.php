<?php
require_once __DIR__ . '/../config/database.php';

$db = getDB();

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'POST') {
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

} else if ($method === 'PUT') {
    $data = json_decode(file_get_contents("php://input"));

    if (empty($data->id) || empty($data->name) || empty($data->role)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID, name, and role are required."]);
        exit();
    }

    try {
        if (!empty($data->password)) {
            $hashedPassword = password_hash($data->password, PASSWORD_DEFAULT);
            $stmt = $db->prepare("UPDATE users SET full_name = ?, role = ?, password = ? WHERE id = ?");
            $stmt->execute([$data->name, $data->role, $hashedPassword, $data->id]);
        } else {
            $stmt = $db->prepare("UPDATE users SET full_name = ?, role = ? WHERE id = ?");
            $stmt->execute([$data->name, $data->role, $data->id]);
        }
        echo json_encode(["success" => true, "message" => "User updated successfully"]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} else if ($method === 'DELETE') {
    $id = $_GET['id'] ?? null;

    if (empty($id)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID harus diisi"]);
        exit();
    }

    try {
        $stmt = $db->prepare("DELETE FROM users WHERE id = ?");
        $stmt->execute([$id]);
        echo json_encode(["success" => true, "message" => "User berhasil dihapus"]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} else {
    $stmt = $db->prepare("SELECT id, user_id, full_name AS name, username, role FROM users");
    $stmt->execute();
    echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));
}
