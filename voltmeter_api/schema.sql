-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 27, 2026 at 06:43 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `voltmeter_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `customers`
--

CREATE TABLE `customers` (
  `id` int(11) NOT NULL,
  `customer_id` varchar(50) NOT NULL,
  `work_order_id` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `address` text NOT NULL,
  `power_va` int(11) NOT NULL,
  `tariff` varchar(20) NOT NULL,
  `last_month_usage` double DEFAULT 0,
  `last_meter_reading` double DEFAULT 0,
  `latitude` double DEFAULT 0,
  `longitude` double DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `customers`
--

INSERT INTO `customers` (`id`, `customer_id`, `work_order_id`, `name`, `address`, `power_va`, `tariff`, `last_month_usage`, `last_meter_reading`, `latitude`, `longitude`, `created_at`) VALUES
(1, '534210001', 'WO-202606-001', 'Ahmad Fauzi', 'Jl. Merdeka No. 10, Jakarta Selatan', 1300, 'R1', 120.5, 45230.5, -6.2615, 106.8106, '2026-06-26 10:57:45'),
(2, '534210002', 'WO-202606-001', 'Dewi Lestari', 'Jl. Sudirman No. 25, Jakarta Pusat', 2200, 'R1', 185.2, 67890, -6.2088, 106.8264, '2026-06-26 10:57:45'),
(3, '534210003', 'WO-202606-001', 'Rizki Pratama', 'Jl. Gatot Subroto No. 5, Bandung', 3500, 'R2', 250, 89000, -6.9175, 107.6191, '2026-06-26 10:57:45');

-- --------------------------------------------------------

--
-- Table structure for table `meters`
--

CREATE TABLE `meters` (
  `id` int(11) NOT NULL,
  `customer_id` varchar(50) NOT NULL,
  `meter_number` varchar(50) NOT NULL,
  `meter_index` int(11) DEFAULT 0,
  `last_reading` double DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `meters`
--

INSERT INTO `meters` (`id`, `customer_id`, `meter_number`, `meter_index`, `last_reading`, `created_at`) VALUES
(1, '534210001', 'ML-001-2024', 0, 90820, '2026-06-26 10:57:45'),
(2, '534210002', 'ML-002-A', 0, 34500, '2026-06-26 10:57:45'),
(3, '534210002', 'ML-002-B', 1, 33390, '2026-06-26 10:57:45'),
(4, '534210003', 'ML-003-2024', 0, 89000, '2026-06-26 10:57:45');

-- --------------------------------------------------------

--
-- Table structure for table `meter_records`
--

CREATE TABLE `meter_records` (
  `id` int(11) NOT NULL,
  `record_id` varchar(50) NOT NULL,
  `customer_id` varchar(50) NOT NULL,
  `meter_number` varchar(50) NOT NULL,
  `previous_reading` double DEFAULT 0,
  `current_reading` double NOT NULL,
  `usage_kwh` double DEFAULT 0,
  `record_date` date NOT NULL,
  `record_time` time NOT NULL,
  `visit_status` enum('TERBACA_NORMAL','RUMAH_KOSONG','HALANGAN') DEFAULT 'TERBACA_NORMAL',
  `photo_path` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT 0,
  `longitude` double DEFAULT 0,
  `notes` text DEFAULT NULL,
  `recorded_by` varchar(50) DEFAULT '',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `verification_status` enum('PENDING','VERIFIED','REJECTED') DEFAULT 'PENDING',
  `verification_note` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `meter_records`
--

INSERT INTO `meter_records` (`id`, `record_id`, `customer_id`, `meter_number`, `previous_reading`, `current_reading`, `usage_kwh`, `record_date`, `record_time`, `visit_status`, `photo_path`, `latitude`, `longitude`, `notes`, `recorded_by`, `created_at`, `verification_status`, `verification_note`) VALUES
(1, 'REC-20260626131029-AEA852', '534210001', 'ML-001-2024', 45230.5, 0, -45230.5, '2026-06-26', '18:10:29', 'TERBACA_NORMAL', '534210001_20260626_131029.jpg', -6.1724242, 106.7885788, '', 'USR002', '2026-06-26 11:10:29', 'VERIFIED', NULL),
(2, 'REC-20260627170337-52581B', '534210001', 'ML-001-2024', 0, 0, 0, '2026-06-27', '22:03:36', 'TERBACA_NORMAL', '534210001_20260627_170337.jpg', -6.1708137, 106.7866716, 'Mantap ey', 'USR002', '2026-06-27 15:03:37', 'REJECTED', 'alamat nya ngk jelas jir'),
(3, 'REC-20260627171659-08C7C6', '534210001', 'ML-001-2024', 0, 200000, 200000, '2026-06-27', '22:16:55', 'TERBACA_NORMAL', '534210001_20260627_171655.jpg', -6.1706644, 106.7866214, 'mantap ey', 'USR002', '2026-06-27 15:16:59', 'VERIFIED', NULL),
(4, 'REC-20260627175803-6F4E8A', '534210001', 'ML-001-2024', 200000, 90820, -109180, '2026-06-27', '22:58:02', 'RUMAH_KOSONG', '534210001_20260627_175803.jpg', -6.1706266, 106.7865932, 'gegege', 'USR002', '2026-06-27 15:58:03', 'VERIFIED', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `role` enum('surveyor','admin') DEFAULT 'surveyor',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `user_id`, `username`, `password`, `full_name`, `role`, `created_at`) VALUES
(1, 'USR001', 'admin', '$2y$10$vMGdkg9gzzS4a1w/FWwan.Ywb16/rJ96zo7VxSFOzPOKUWvhaQfr.', 'Administrator', 'admin', '2026-06-26 10:57:45'),
(2, 'USR002', 'surveyor1', '$2y$10$vMGdkg9gzzS4a1w/FWwan.Ywb16/rJ96zo7VxSFOzPOKUWvhaQfr.', 'Budi Santoso', 'surveyor', '2026-06-26 10:57:45'),
(3, 'USR003', 'surveyor2', '$2y$10$vMGdkg9gzzS4a1w/FWwan.Ywb16/rJ96zo7VxSFOzPOKUWvhaQfr.', 'Siti Rahayu', 'surveyor', '2026-06-26 10:57:45'),
(4, 'USR20260626132843', 'gege', '$2y$10$J3b10sCqREvxhRIh/WUvo.S26ZD/mkC8UVAcpq0M03cLDvH5KPlIa', 'dapid', 'surveyor', '2026-06-26 11:28:43');

-- --------------------------------------------------------

--
-- Table structure for table `work_orders`
--

CREATE TABLE `work_orders` (
  `id` int(11) NOT NULL,
  `work_order_id` varchar(50) NOT NULL,
  `month` int(11) NOT NULL,
  `year` int(11) NOT NULL,
  `assigned_to` varchar(50) NOT NULL,
  `status` enum('active','completed') DEFAULT 'active',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `work_orders`
--

INSERT INTO `work_orders` (`id`, `work_order_id`, `month`, `year`, `assigned_to`, `status`, `created_at`) VALUES
(1, 'WO-202606-001', 6, 2026, 'USR002', 'active', '2026-06-26 10:57:45');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `customers`
--
ALTER TABLE `customers`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `customer_id` (`customer_id`),
  ADD KEY `work_order_id` (`work_order_id`);

--
-- Indexes for table `meters`
--
ALTER TABLE `meters`
  ADD PRIMARY KEY (`id`),
  ADD KEY `customer_id` (`customer_id`);

--
-- Indexes for table `meter_records`
--
ALTER TABLE `meter_records`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `record_id` (`record_id`),
  ADD KEY `customer_id` (`customer_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- Indexes for table `work_orders`
--
ALTER TABLE `work_orders`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `work_order_id` (`work_order_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `customers`
--
ALTER TABLE `customers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `meters`
--
ALTER TABLE `meters`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `meter_records`
--
ALTER TABLE `meter_records`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `work_orders`
--
ALTER TABLE `work_orders`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `customers`
--
ALTER TABLE `customers`
  ADD CONSTRAINT `customers_ibfk_1` FOREIGN KEY (`work_order_id`) REFERENCES `work_orders` (`work_order_id`) ON DELETE CASCADE;

--
-- Constraints for table `meters`
--
ALTER TABLE `meters`
  ADD CONSTRAINT `meters_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`) ON DELETE CASCADE;

--
-- Constraints for table `meter_records`
--
ALTER TABLE `meter_records`
  ADD CONSTRAINT `meter_records_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
