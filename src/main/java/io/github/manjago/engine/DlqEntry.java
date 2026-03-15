package io.github.manjago.engine;

import java.time.Instant;

public record DlqEntry(
        String originalPayload,
        String actorName,
        String errorMessage,
        String stackTrace,
        Instant failedAt,
        int attempts
) {

}