package io.github.manjago.engine;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Класс для составного ключа
// Важно: должен быть Comparable для правильной сортировки в B-Tree
public class QueueKey implements Serializable, Comparable<QueueKey> {
    public final long timestamp;
    public final UUID uuid;

    public QueueKey(long timestamp, UUID uuid) {
        this.timestamp = timestamp;
        this.uuid = uuid;
    }

    public QueueKey(@NotNull Instant instant, UUID uuid) {
        this(instant.toEpochMilli(), uuid);
    }

    @Override
    public int compareTo(QueueKey o) {
        // Сначала сортируем по времени
        int timeCmp = Long.compare(this.timestamp, o.timestamp);
        if (timeCmp != 0)
            return timeCmp;
        // Если время совпало, сортируем по UUID (чтобы обеспечить уникальность ключа)
        return this.uuid.compareTo(o.uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        QueueKey that = (QueueKey) o;
        return this.compareTo(that) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, uuid);
    }
}
