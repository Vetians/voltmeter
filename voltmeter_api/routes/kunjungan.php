<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

require_once __DIR__ . '/../config/database.php';

$db = getDB();

if (isset($_GET['statistik']) && $_GET['statistik'] == 1) {
    
    $bulan = isset($_GET['bulan']) ? (int)$_GET['bulan'] : null;
    $tahun = isset($_GET['tahun']) ? (int)$_GET['tahun'] : null;
    
    $params = [];
    $sql_filter = "";

    if ($bulan !== null && $tahun !== null) {
        $sql_filter = " AND MONTH(record_date) = ? AND YEAR(record_date) = ?";
        $params = [$bulan, $tahun];
    } elseif ($tahun !== null) {
        $sql_filter = " AND YEAR(record_date) = ?";
        $params = [$tahun];
    }

    function getStat($db, $status, $sql_filter, $params) {
        $query = "SELECT COUNT(*) as total FROM meter_records WHERE 1=1";
        $exec_params = [];
        
        if ($status !== null) {
            $query .= " AND visit_status = ?";
            $exec_params[] = $status;
        }
        
        $query .= $sql_filter;
        $exec_params = array_merge($exec_params, $params);
        
        $stmt = $db->prepare($query);
        $stmt->execute($exec_params);
        return (int) $stmt->fetchColumn();
    }

    function getStatVerification($db, $vStatus, $sql_filter, $params) {
        $query = "SELECT COUNT(*) as total FROM meter_records WHERE verification_status = ?";
        $exec_params = [$vStatus];
        
        $query .= $sql_filter;
        $exec_params = array_merge($exec_params, $params);
        
        $stmt = $db->prepare($query);
        $stmt->execute($exec_params);
        return (int) $stmt->fetchColumn();
    }

    echo json_encode([
        "total_pelanggan"   => (int) $db->query("SELECT COUNT(*) FROM customers")->fetchColumn(),
        "total_meteran"     => (int) $db->query("SELECT COUNT(*) FROM meters")->fetchColumn(),
        "total_kunjungan"  => getStat($db, null, $sql_filter, $params),
        "total_verified"   => getStatVerification($db, 'VERIFIED', $sql_filter, $params),
        "total_unverified" => getStatVerification($db, 'PENDING', $sql_filter, $params)
    ]);

} else {
    http_response_code(400);
    echo json_encode(["message" => "Parameter statistik=1 diperlukan."]);
}
?>
