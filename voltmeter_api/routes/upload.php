<?php
require_once __DIR__ . '/../config/database.php';

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");

$uploadDir = __DIR__ . '/../uploads/photos/';

if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!isset($_FILES['photo'])) {
        http_response_code(400);
        echo json_encode(['error' => 'File photo tidak ditemukan']);
        exit;
    }

    $file = $_FILES['photo'];
    $customerId = $_POST['customer_id'] ?? 'unknown';
    
    // Validasi ukuran max 5MB
    if ($file['size'] > 5 * 1024 * 1024) {
        http_response_code(400);
        echo json_encode(['error' => 'Ukuran file terlalu besar (maks 5MB)']);
        exit;
    }

    // Auto naming
    $filename = $customerId . "_" . date('Ymd_His') . ".jpg";
    $destPath = $uploadDir . $filename;

    if (move_uploaded_file($file['tmp_name'], $destPath)) {
        http_response_code(201);
        echo json_encode([
            'success' => true,
            'photo_path' => $filename,
            'message' => 'Upload berhasil'
        ]);
    } else {
        http_response_code(500);
        echo json_encode(['error' => 'Gagal menyimpan file']);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Serve image
    $filename = $_GET['filename'] ?? '';
    $filePath = $uploadDir . basename($filename);

    if ($filename && file_exists($filePath)) {
        $mimeType = mime_content_type($filePath);
        header("Content-Type: " . $mimeType);
        header("Content-Length: " . filesize($filePath));
        readfile($filePath);
    } else {
        http_response_code(404);
        echo json_encode(['error' => 'Foto tidak ditemukan']);
    }
} else {
    http_response_code(405);
    echo json_encode(['error' => 'Method tidak diizinkan']);
}
