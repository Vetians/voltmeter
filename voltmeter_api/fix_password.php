<?php
require_once __DIR__ . '/config/database.php';

$db = getDB();

// Generate hash yang benar untuk password 'admin123'
$hash = password_hash('admin123', PASSWORD_DEFAULT);

// Update semua user dengan password baru
$stmt = $db->prepare("UPDATE users SET password = ?");
$stmt->execute([$hash]);

echo "<h2>Password Berhasil Diupdate!</h2>";
echo "<p>Password untuk semua user sekarang: <b>admin123</b></p>";
echo "<p>Hash: <code>$hash</code></p>";
echo "<br>";
echo "<h3>Coba login sekarang:</h3>";
echo "<p>Username: surveyor1</p>";
echo "<p>Password: admin123</p>";
echo "<br>";
echo "<a href='routes/login.php'>Test Login (GET)</a>";
