package io.github.manjago.engine;

import org.h2.mvstore.tx.Transaction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class ActorTest {

    @Test
    void simpleHappyWay(@TempDir Path tempDir) throws InterruptedException {
        final String mailboxName = "testactor:mailbox";
        final MutableClock mutableClock = MutableClock.of(Instant.now());

        try (final MvStoreManager mvStoreManager = new MvStoreManager(tempDir.resolve("mvStore"))) {
            final ActorSystem actorSystem = new ActorSystem(mvStoreManager, mutableClock);
            actorSystem.send(mailboxName, "firstPayload");
            mutableClock.advance(Duration.ofSeconds(1));
            actorSystem.send(mailboxName, "secondPayload");

            final List<String> collected = new CopyOnWriteArrayList<>();
            final Actor actor = new TestActor(mailboxName, mutableClock, mvStoreManager, collected);

            final Thread actorThread = Thread.ofVirtual()
                    .name("testactor-", 0)
                    .start(actor::mainLoop);
            actorSystem.register(mailboxName, actorThread);

            await().atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(500))
                    .until(() -> collected.size() >= 2);

            actorThread.interrupt();
            actorThread.join(Duration.ofSeconds(5));
            assertFalse(actorThread.isAlive(), "Actor thread should have stopped");
            assertEquals(List.of("firstPayload", "secondPayload"), collected);
        }
    }

    @Test
    void delayedHappyWay(@TempDir Path tempDir) throws InterruptedException {
        final String mailboxName = "testactor:mailbox";
        final MutableClock mutableClock = MutableClock.of(Instant.now());

        try (final MvStoreManager mvStoreManager = new MvStoreManager(tempDir.resolve("mvStore"))) {
            final ActorSystem actorSystem = new ActorSystem(mvStoreManager, mutableClock);

            final List<String> collected = new CopyOnWriteArrayList<>();
            final Actor actor = new TestActor(mailboxName, mutableClock, mvStoreManager, collected);

            final Thread actorThread = Thread.ofVirtual()
                    .name("testactor-", 0)
                    .start(actor::mainLoop);
            actorSystem.register(mailboxName, actorThread);
            actorSystem.send(mailboxName, "delay-after-30m", mutableClock.instant().plus(30, ChronoUnit.MINUTES));

            mutableClock.advance(Duration.ofHours(1));
            actorSystem.send(mailboxName, "wakeUp");

            await().atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(500))
                    .until(() -> collected.size() == 2);


            actorThread.interrupt();
            actorThread.join(Duration.ofSeconds(5));
            assertFalse(actorThread.isAlive(), "Actor thread should have stopped");
            assertEquals(List.of("delay-after-30m", "wakeUp"), collected);
        }
    }

    private static class TestActor extends Actor {

        private final List<String> collected;

        private TestActor(String mailboxName,
                MutableClock mutableClock,
                MvStoreManager mvStoreManager,
                List<String> collected) {
            super(mailboxName, mutableClock, mvStoreManager);
            this.collected = collected;
        }

        @Override
        void process(@NotNull String payload, @NotNull Transaction tx) {
            System.out.println("processing payload: " + payload);
            collected.add(payload);
        }

        @Override
        public void mainLoop() {
            System.out.println("main loop enter");
            super.mainLoop();
            System.out.println("main loop exit");
        }
    }
}