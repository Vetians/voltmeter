<?php
error_reporting(0);
ob_clean();

require_once __DIR__ . '/../config/database.php';

$uploadDir = __DIR__ . '/../uploads/photos/';

if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!isset($_FILES['photo'])) {
        http_response_code(400);
        echo json_encode(['success' => false, 'photo_path' => '', 'message' => 'File photo tidak ditemukan']);
        exit;
    }

    $file = $_FILES['photo'];
    $customerId = $_POST['customer_id'] ?? 'unknown';

    if ($file['size'] > 5 * 1024 * 1024) {
        http_response_code(400);
        echo json_encode(['success' => false, 'photo_path' => '', 'message' => 'Ukuran file terlalu besar (maks 5MB)']);
        exit;
    }

    if ($file['error'] !== UPLOAD_ERR_OK) {
        http_response_code(400);
        echo json_encode(['success' => false, 'photo_path' => '', 'message' => 'Upload error: ' . $file['error']]);
        exit;
    }

    $filename = $customerId . "_" . date('Ymd_His') . ".jpg";
    $destPath = $uploadDir . $filename;

    if (move_uploaded_file($file['tmp_name'], $destPath)) {
        echo json_encode([
            'success' => true,
            'photo_path' => $filename,
            'message' => 'Upload berhasil'
        ]);
    } else {
        http_response_code(500);
        echo json_encode(['success' => false, 'photo_path' => '', 'message' => 'Gagal menyimpan file']);
    }

} else if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $filename = $_GET['filename'] ?? '';
    $filePath = $uploadDir . basename($filename);

    if ($filename && file_exists($filePath)) {
        $mimeType = mime_content_type($filePath);
        header("Content-Type: " . $mimeType);
        readfile($filePath);
    } else {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'Foto tidak ditemukan']);
    }

} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method tidak diizinkan']);
}
