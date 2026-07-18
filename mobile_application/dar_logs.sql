-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Jul 01, 2026 at 11:41 AM
-- Server version: 8.4.3
-- PHP Version: 8.3.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `dar_logs`
--

-- --------------------------------------------------------

--
-- Table structure for table `activity_logs`
--

CREATE TABLE `activity_logs` (
  `id` int UNSIGNED NOT NULL,
  `municipality` varchar(128) DEFAULT NULL,
  `lo_claimant` varchar(255) DEFAULT NULL,
  `title_no` varchar(64) DEFAULT NULL,
  `odts_no` varchar(128) DEFAULT NULL,
  `lot_no` varchar(64) DEFAULT NULL,
  `survey_no` varchar(64) DEFAULT NULL,
  `area_has` decimal(10,2) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `transmitted_documents` varchar(255) DEFAULT NULL,
  `route_to` varchar(255) DEFAULT NULL,
  `route_to_user_id` int UNSIGNED DEFAULT NULL,
  `routed_from_user_id` int UNSIGNED DEFAULT NULL,
  `routed_at` datetime DEFAULT NULL,
  `received_by_control_no` varchar(255) DEFAULT NULL,
  `remarks_action_taken` text,
  `work_status` enum('not_finished','finished') NOT NULL DEFAULT 'not_finished',
  `archived_at` datetime DEFAULT NULL,
  `archived_by` int UNSIGNED DEFAULT NULL,
  `created_by` int UNSIGNED DEFAULT NULL,
  `updated_by` int UNSIGNED DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `activity_logs`
--

INSERT INTO `activity_logs` (`id`, `municipality`, `lo_claimant`, `title_no`, `odts_no`, `lot_no`, `survey_no`, `area_has`, `location`, `transmitted_documents`, `route_to`, `route_to_user_id`, `routed_from_user_id`, `routed_at`, `received_by_control_no`, `remarks_action_taken`, `work_status`, `archived_at`, `archived_by`, `created_by`, `updated_by`, `created_at`, `updated_at`) VALUES
(2, 'TINAMBAC', 'SPS. GONZALO BRIOSO & NORMA OLMEDILLO', 'TCT-8145', '23-09-2448', '112, Pls-37', 'Psd-05-0033742(AR)', NULL, 'Sagrada, Tinambac', '', '', NULL, NULL, NULL, '', 'Request for Relocation Survey recvd by Engr. Rayos 4/23/26', 'not_finished', NULL, NULL, 1, 1, '2026-05-04 14:15:05', '2026-05-04 14:17:05'),
(7, 'CALABANGA', 'LIBORIO ANDAL', 'UT', '26-04-959', '12043', 'Cad-494-D', 13.60, 'Binaliw, Calabanga', 'RSS Docs: Pre-OCI, (LRC) Psd-189992, LDC, NOC, Tax Dec, LRA Cert, Lot Desciption, CENRO Lot Status Cert.', 'Maribel', 9, 4, '2026-05-13 06:12:00', '', 'Request for Survey Services c/o Engr. Teodones', 'not_finished', NULL, NULL, 1, 4, '2026-05-04 14:24:59', '2026-05-13 14:12:00'),
(8, 'CALABANGA', 'NORBERTO PANIBE', '', '26-02-323', '6396', '', NULL, '', '', 'Maribel', 9, 4, '2026-05-13 06:11:53', '', 'REQUEST FOR GEOTAGGING/IDENTIFICATION OF LOCATION OF PROPERTY c/o Engr. Teodones', 'not_finished', NULL, NULL, 1, 4, '2026-05-04 14:33:54', '2026-05-13 14:11:53'),
(9, 'NAGA', 'Ramos, Teodulo/Verdadero, Consolacion', 'TCT-17611/TCT-13268', '24-06-967', '1; 873-D', 'Psd-8465; Psd05-003049', 14.90, 'Carolina & Cararayan, Naga City', 'copy of title', 'Richard', 8, 4, '2026-05-13 06:11:44', 'NEIL', 'RQST FOR GEOTAGGING c/o Engr. Rayos', 'not_finished', NULL, NULL, 1, 4, '2026-05-04 14:37:36', '2026-05-13 14:11:44'),
(10, 'TINAMBAC', 'Carlito Corpuz, ET. AL.', 'TCT-RT-9718(6383)', '', '189', 'Psd-05-042015(AR)', NULL, 'Magsaysay, Tinambac', 'DARRO suggest the cancellation of the approved survey plan pursuant to MC-2010-13 and DAO 2007-29', '', NULL, NULL, NULL, 'NEIL', '', 'finished', NULL, NULL, 4, 1, '2026-05-06 13:19:31', '2026-05-06 13:23:41'),
(11, 'TINAMBAC', 'Emily R. Tuy ET. AL.', '00859770', '', '1-B, Psu-145803', '', 23.79, 'San Vicente, Tinambac', 'Request for survey authority', 'Maribel', 9, 4, '2026-06-08 06:05:07', 'NEIL', 'c/o Engr. Bel', 'finished', NULL, NULL, 4, 4, '2026-05-06 14:40:21', '2026-06-08 14:05:07'),
(12, 'SANJOSE', 'Juan Panday', '', '', '2238', '', 1.19, 'Bagacay, San Jose', 'Request for BL Form 700 and Certified Sketch Map', '', NULL, NULL, NULL, '', 'Awaiting BL Form 700 received by DARRO dtd March 11, 2026', 'finished', NULL, NULL, 4, 4, '2026-05-06 17:31:45', '2026-05-13 09:12:24'),
(13, 'SANJOSE', 'Manuel Garchitorena', '', '', '3027', '', 0.98, 'Mampirao, San Jose', 'Request for Certifed Sketch Map', '', NULL, NULL, NULL, '', 'Received by DARRO dtd March 11, 2026', 'finished', NULL, NULL, 4, 4, '2026-05-06 17:32:34', '2026-05-13 09:12:28'),
(15, 'GARCHITORENA', 'Deogracias Cledera', '', '', '1762, AC-1011', '', 2.31, 'Bahi', 'Forwarded Certified Sketch plan', '', NULL, NULL, '2026-05-18 09:06:37', '', 'For filing', 'finished', NULL, NULL, 4, 4, '2026-05-18 09:06:37', '2026-05-18 09:06:37'),
(16, 'TIGAON', 'Alfredo E. Jacob', 'TCT-RT-7553', '20-03-364', '14', 'Psu-18766', 5.64, 'San Antonio', 'Copy of TCT-RT-7553 as reference for superimposition', 'AldonPogi', NULL, NULL, NULL, 'Neil', 'Received 5/20/26;', 'finished', NULL, NULL, 3, 6, '2026-05-21 08:48:42', '2026-06-08 17:19:30'),
(17, 'SANJOSE', 'Rogelio Pacamara', 'UT', '26-05-1059', '2048', 'Cad-470-D', 1.35, 'Palale', 'RSS Docs; LRA Cert., CARPER LAD Form 2-B, DENR Lot Status/LC Cert.; Tax Dec', 'Maribel', 9, 3, '2026-05-21 11:00:51', 'NEIL', 'Received 5/21/26', 'not_finished', NULL, NULL, 3, 3, '2026-05-21 11:00:51', '2026-05-21 11:00:51'),
(18, 'LAGONOY', 'Apolonio Monserate / Agosto Gaor. Sr., et. al.', '', '26-05-1061', '27/28', '', 33.00, 'Balaton', 'Request for Relocation Survey', 'Richard', 8, 3, '2026-05-21 11:06:05', 'Neil', 'Received 5/21/26', 'not_finished', NULL, NULL, 3, 3, '2026-05-21 11:06:05', '2026-05-21 11:06:05'),
(19, 'CALABANGA', 'Cipriaso Florendo/Vicente Tapalla Florendo', '', '26-05-1095', '', '', 2.90, '', 'Request for Relocation Survey', 'Richard', 8, 3, '2026-05-21 11:09:00', 'Neil', 'Received 05/21/26', 'not_finished', NULL, NULL, 3, 3, '2026-05-21 11:09:00', '2026-05-21 11:09:00'),
(20, 'CALABANGA', 'Susan J. Rodriguez', 'TCT-7800', '26-05-1128', 'G, (LRC) pCS-6006', 'Bsd-05-003472(OLT)', 32.84, 'Cadlan, Pili', 'Indorsement - Request for the cancellation of Bsd-05-003472(OLT)', 'Maribel', 9, 3, '2026-05-28 18:53:37', 'Neil', 'recvd 6/1/2026', 'not_finished', NULL, NULL, 3, 3, '2026-05-28 18:53:37', '2026-05-28 18:53:37'),
(22, 'MAGARAO', '', '', '', '', '', NULL, 'Sta. Lucia, Magarao', 'Requesting for sketch plan and PHILARIS Copy', 'AldonPogi', 6, 4, '2026-06-08 09:23:01', 'NEIL', 'Transmitted to DARRO', 'finished', NULL, NULL, 4, 4, '2026-06-08 17:22:35', '2026-06-08 17:23:01'),
(23, 'TINAMBAC', 'Fausto V. Rejante, Pascual San Andres', 'TCT-RT-1358(5134), OCT-RP-2727(27263)', '', 'H-135432, Lot 2, Psu-233508', '', NULL, 'Brgy. Bani, Tinambac, CS', 'Request for super imposition', 'AldonPogi', 6, 4, '2026-06-08 17:22:48', 'NEIL', '', 'not_finished', NULL, NULL, 4, 4, '2026-06-08 17:22:48', '2026-06-08 17:22:48'),
(24, 'NAGA', '', '', '26-05-1049', '', '', NULL, '', 'Letter from Engr. Richard Tollo dtd 6-4-26 re: Follow-up on Request for CTC of CCLOAs', 'Maribel', 9, 3, '2026-06-11 17:50:55', 'Neil', 'recv\'d 6/15/26', 'not_finished', NULL, NULL, 3, 3, '2026-06-11 17:50:55', '2026-06-11 17:50:55'),
(25, 'NAGA', '', '', '26-05-1049', '', '', NULL, '', 'Letter from Engr. Richard Tollo dtd 6-4-26 re: Request for Endorsement of SR under Project SPLIT; 2 LHs, Garchitorena', 'Maribel', 9, 3, '2026-06-11 17:52:57', 'Neil', 'recv\'d 6/15/26', 'not_finished', NULL, NULL, 3, 3, '2026-06-11 17:52:57', '2026-06-11 17:52:57'),
(26, 'OCAMPO', 'Jacinta Navarro Vda. De Bunyi, et. al.', 'TCT-RT-3033(2323)', '26-06-1360', '4', 'Psu-62685', 43.44, 'Ayungan', 'RSS; copy of title, tax dec', 'Maribel', 9, 3, '2026-06-25 05:19:06', 'Neil', 'recvd 6/25/26', 'not_finished', NULL, NULL, 3, 3, '2026-06-25 05:19:06', '2026-06-25 05:19:06'),
(27, 'OCAMPO', 'Sps. Clemente Balbastro & Felisa Manalo', 'TCT-6902', '26-06-1403', 'D', 'Psd-39724', 3.60, 'Guinaban', 'RSS, copy of title, tax dec', 'Maribel', 9, 3, '2026-06-25 05:44:01', 'Neil', 'recvd 6/25/26', 'not_finished', NULL, NULL, 3, 3, '2026-06-25 05:44:01', '2026-06-25 05:44:01');

-- --------------------------------------------------------

--
-- Table structure for table `audit_log`
--

CREATE TABLE `audit_log` (
  `id` int UNSIGNED NOT NULL,
  `user_id` int UNSIGNED DEFAULT NULL,
  `username` varchar(64) NOT NULL,
  `action` enum('add','edit','delete','archive','restore','login','logout') NOT NULL,
  `table_name` varchar(64) DEFAULT 'activity_logs',
  `record_id` int UNSIGNED DEFAULT NULL,
  `details` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `audit_log`
--

INSERT INTO `audit_log` (`id`, `user_id`, `username`, `action`, `table_name`, `record_id`, `details`, `created_at`) VALUES
(98, 3, 'Neil', 'add', 'activity_logs', 25, 'Record created', '2026-06-11 17:52:57'),
(99, 4, 'Jhaps', 'login', NULL, NULL, NULL, '2026-06-22 10:26:13'),
(100, 1, 'admin', 'login', NULL, NULL, NULL, '2026-06-22 13:39:44'),
(101, 3, 'Neil', 'login', NULL, NULL, NULL, '2026-06-22 13:47:13'),
(102, 1, 'admin', 'login', NULL, NULL, NULL, '2026-06-24 18:24:18'),
(103, 3, 'Neil', 'login', NULL, NULL, NULL, '2026-06-25 05:14:37'),
(104, 3, 'Neil', 'login', NULL, NULL, NULL, '2026-06-25 05:14:37'),
(105, 3, 'Neil', 'add', 'activity_logs', 26, 'Record created', '2026-06-25 05:19:07'),
(106, 3, 'Neil', 'add', 'activity_logs', 27, 'Record created', '2026-06-25 05:44:02'),
(107, 1, 'admin', 'login', NULL, NULL, NULL, '2026-07-01 19:27:38');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` int UNSIGNED NOT NULL,
  `user_id` int UNSIGNED NOT NULL,
  `type` varchar(32) NOT NULL DEFAULT 'route',
  `record_id` int UNSIGNED DEFAULT NULL,
  `sender_id` int UNSIGNED DEFAULT NULL,
  `message` varchar(255) NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`id`, `user_id`, `type`, `record_id`, `sender_id`, `message`, `is_read`, `created_at`) VALUES
(1, 1, 'route', NULL, NULL, 'Record routed from tristan.', 1, '2026-04-30 15:06:08'),
(2, 3, 'route', NULL, 1, 'Record routed from admin.', 0, '2026-05-04 14:16:19'),
(3, 3, 'route', NULL, 1, 'Record routed from admin.', 1, '2026-05-04 14:17:23'),
(4, 3, 'route', NULL, 1, 'Record routed from admin.', 0, '2026-05-04 14:18:24'),
(5, 3, 'route', NULL, 1, 'Record routed from admin.', 1, '2026-05-04 14:18:25'),
(6, 6, 'route', NULL, 4, 'Record routed from Jhaps.', 1, '2026-05-13 14:11:21'),
(7, 9, 'route', 11, 4, 'Record routed from Jhaps.', 0, '2026-05-13 14:11:34'),
(8, 8, 'route', 9, 4, 'Record routed from Jhaps.', 0, '2026-05-13 14:11:44'),
(9, 9, 'route', 8, 4, 'Record routed from Jhaps.', 0, '2026-05-13 14:11:54'),
(10, 9, 'route', 7, 4, 'Record routed from Jhaps.', 0, '2026-05-13 14:12:00'),
(11, 6, 'route', 16, 3, 'Record routed from Neil.', 1, '2026-05-21 08:48:42'),
(12, 9, 'route', 17, 3, 'Record routed from Neil.', 0, '2026-05-21 11:00:52'),
(13, 8, 'route', 18, 3, 'Record routed from Neil.', 0, '2026-05-21 11:06:05'),
(14, 8, 'route', 19, 3, 'Record routed from Neil.', 0, '2026-05-21 11:09:00'),
(15, 9, 'route', 20, 3, 'Record routed from Neil.', 0, '2026-05-28 18:53:37'),
(16, 6, 'route', NULL, 4, 'Record routed from Jhaps.', 1, '2026-06-08 17:09:15'),
(17, 6, 'route', 22, 4, 'Record routed from Jhaps.', 1, '2026-06-08 17:22:35'),
(18, 6, 'route', 23, 4, 'Record routed from Jhaps.', 1, '2026-06-08 17:22:48'),
(19, 9, 'route', 24, 3, 'Record routed from Neil.', 0, '2026-06-11 17:50:55'),
(20, 9, 'route', 25, 3, 'Record routed from Neil.', 0, '2026-06-11 17:52:57'),
(21, 9, 'route', 26, 3, 'Record routed from Neil.', 0, '2026-06-25 05:19:07'),
(22, 9, 'route', 27, 3, 'Record routed from Neil.', 0, '2026-06-25 05:44:02');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int UNSIGNED NOT NULL,
  `username` varchar(64) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('admin','user') NOT NULL DEFAULT 'user',
  `approved` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_activity` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password`, `role`, `approved`, `created_at`, `updated_at`, `last_activity`) VALUES
(1, 'admin', '$2y$10$6FvE1iWK4zuZFtFU2oT19ebWFU0vdFyfn1lanydNxiM2yKc/3y1lW', 'admin', 1, '2026-04-30 15:02:49', '2026-07-01 19:41:07', '2026-07-01 19:41:07'),
(3, 'Neil', '$2y$10$zorEAHEBfg5BiMYq51W24uhpdnsmjCmA2be5QQoDjg9.J91pwvURS', 'admin', 1, '2026-05-04 14:15:33', '2026-06-25 09:50:25', '2026-06-25 09:50:25'),
(4, 'Jhaps', '$2y$10$NQgRFgs6rPQ5R7xXR3En..K5pTjWoUOo37zQCtZDGMXbjwkxLV/s6', 'admin', 1, '2026-05-06 13:08:39', '2026-06-22 10:39:45', '2026-06-22 10:39:45'),
(5, 'Chok', '$2y$10$hrsqXWggLBTR.w37/R64..sMrQ3.QX2wAWQ2de/.ulQsgTmT6k3L2', 'user', 1, '2026-05-13 14:09:27', '2026-05-13 15:19:33', '2026-05-13 15:19:33'),
(6, 'AldonPogi', '$2y$10$2wExq8aLPWnyw0RMU0NtFOgeAs4vVcy5QSxIWGgzpgqPm8oy2dMke', 'user', 1, '2026-05-13 14:09:35', '2026-06-08 17:26:20', '2026-06-08 17:26:20'),
(7, 'Honey', '$2y$10$DB/FHnPnq/C1c3eAw8AshOqzHgae/4QtObvqyPU.1vR6XfmVQjFwe', 'admin', 1, '2026-05-13 14:09:43', '2026-06-08 17:10:08', '2026-05-19 16:22:55'),
(8, 'Richard', '$2y$10$lBG8hc71kWPw1.94W/WPkuSvusbCqHCPqsk4nBX7NdQVzoCKf1mwO', 'user', 1, '2026-05-13 14:10:08', '2026-05-13 14:10:08', NULL),
(9, 'Maribel', '$2y$10$CBTRbaJAno7NUe6BLomBaehsbLiDBr3ITt8u3FiYaZqHVheBWJIvi', 'user', 1, '2026-05-13 14:10:19', '2026-05-13 14:10:19', NULL),
(10, 'Job', '$2y$10$LERriRwdIGz63wxn1QRzF.cUXZxgK4jwqKEs0bTEGt/IvSde7RJLW', 'user', 1, '2026-05-13 14:10:44', '2026-05-13 14:10:44', NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `activity_logs`
--
ALTER TABLE `activity_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `archived_at` (`archived_at`),
  ADD KEY `archived_by` (`archived_by`),
  ADD KEY `created_by` (`created_by`),
  ADD KEY `updated_by` (`updated_by`),
  ADD KEY `route_to_user_id` (`route_to_user_id`),
  ADD KEY `routed_from_user_id` (`routed_from_user_id`);

--
-- Indexes for table `audit_log`
--
ALTER TABLE `audit_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `action` (`action`),
  ADD KEY `created_at` (`created_at`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `is_read` (`is_read`),
  ADD KEY `created_at` (`created_at`),
  ADD KEY `record_id` (`record_id`),
  ADD KEY `notifications_sender_id` (`sender_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `activity_logs`
--
ALTER TABLE `activity_logs`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=28;

--
-- AUTO_INCREMENT for table `audit_log`
--
ALTER TABLE `audit_log`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=108;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `activity_logs`
--
ALTER TABLE `activity_logs`
  ADD CONSTRAINT `activity_logs_archived_by` FOREIGN KEY (`archived_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `activity_logs_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `activity_logs_route_to_user_id` FOREIGN KEY (`route_to_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `activity_logs_routed_from_user_id` FOREIGN KEY (`routed_from_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `activity_logs_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `audit_log`
--
ALTER TABLE `audit_log`
  ADD CONSTRAINT `audit_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_record_id` FOREIGN KEY (`record_id`) REFERENCES `activity_logs` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `notifications_sender_id` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `notifications_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
