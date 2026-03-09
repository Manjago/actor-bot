package io.github.manjago.engine;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

// Класс для составного ключа
// Важно: должен быть Comparable для правильной сортировки в B-Tree
public record QueueKey(long timestamp, UUID uuid) implements Comparable<QueueKey> {

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
        // UUID.compareTo в Java сравнивает как signed long-и. Это значит, что UUID с MSB
        // начинающимся на 0x8... будет считаться "меньше" чем 0x0....
        // Для нас это не проблема — нужен стабильный порядок, а не какой-то конкретный.
        // Но для редкого случая совпадения времени не удивляемся порядку при отладке!
        return this.uuid.compareTo(o.uuid);
    }

    // equals и hashCode не переопределяю - они согласованы с compareTo

}
