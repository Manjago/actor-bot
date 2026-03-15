package io.github.manjago.engine;

import io.github.manjago.engine.datatype.QueueKeyType;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueKeyTest {

    @Test
    void compareOthers() {
        //given
        final Instant firstDate = Instant.now();
        final Instant secondDate = firstDate.plusSeconds(30);
        final QueueKey firstKey = new QueueKey(firstDate, UUID.randomUUID());
        final QueueKey secondKey = new QueueKey(secondDate, UUID.randomUUID());
        //when then
        assertTrue(firstKey.compareTo(secondKey) < 0);
    }

    @Test
    void compareOthersSameDate() {
        //given
        final Instant firstDate = Instant.now();

        UUID uuidLesser = UUID.randomUUID();
        UUID uuidGreater = UUID.randomUUID();
        if (uuidLesser.compareTo(uuidGreater) > 0) {
            UUID temp = uuidLesser;
            uuidLesser = uuidGreater;
            uuidGreater = temp;
        }

        final QueueKey firstKey = new QueueKey(firstDate, uuidLesser);
        final QueueKey secondKey = new QueueKey(firstDate, uuidGreater);
        //when then
        assertTrue(firstKey.compareTo(secondKey) < 0);
    }

    @Test
    void compareOthersSameUUID() {
        //given
        final Instant firstDate = Instant.now();
        final Instant secondDate = firstDate.plusSeconds(30);
        final UUID uuid = UUID.randomUUID();
        final QueueKey firstKey = new QueueKey(firstDate, uuid);
        final QueueKey secondKey = new QueueKey(secondDate, uuid);
        //when then
        assertTrue(firstKey.compareTo(secondKey) < 0);
    }

    @Test
    void messagesOrderedByFireDate(@TempDir Path tempDir) {

        // given
        final Instant firstDate = Instant.now();
        final Instant secondDate = firstDate.plusSeconds(30);
        final QueueKey firstKey = new QueueKey(firstDate, UUID.randomUUID());
        final QueueKey secondKey = new QueueKey(secondDate, UUID.randomUUID());
        final String firstValue = "first";
        final String secondValue = "second";

        try (final MVStore store = new MVStore.Builder().fileName(tempDir.resolve("mvstore").toString()).open()) {

            final MVMap<QueueKey, String> map = store.openMap("queueMap",
                    new MVMap.Builder<QueueKey, String>().keyType(new QueueKeyType())

            );

            //when

            map.put(secondKey, secondValue);
            map.put(firstKey, firstValue);

            //then

            assertEquals(firstKey, map.firstKey());
            assertEquals(firstValue, map.remove(firstKey));

            assertEquals(secondKey, map.firstKey());
            assertEquals(secondValue, map.remove(secondKey));

        }


    }

}