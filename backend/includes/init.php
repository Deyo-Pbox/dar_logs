<?php
/**
 * DAR Activity Logs - Bootstrap
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/app.php';
require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/municipalities.php';

header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');
