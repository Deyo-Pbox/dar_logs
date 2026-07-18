<?php

declare(strict_types=1);

namespace DarLogs\Exceptions;

use RuntimeException;

abstract class AppException extends RuntimeException
{
    abstract public function getHttpCode(): int;
}
