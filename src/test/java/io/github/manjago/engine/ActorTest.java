package io.github.manjago.engine;

import org.h2.mvstore.tx.Transaction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ActorTest {

    @Test
    void simpleHappyWay(@TempDir Path tempDir) throws InterruptedException {
        final String mailboxName = "testactor:mailbox";
        final Clock clock = MutableClock.of(Instant.now());

        try(final MvStoreManager mvStoreManager = new MvStoreManager(tempDir.resolve("mvStore"))) {
            final ActorSystem actorSystem = new ActorSystem(mvStoreManager, clock);
            actorSystem.send(mailboxName, "firstPayload");
            actorSystem.send(mailboxName, "secondPayload");

            final CountDownLatch processed = new CountDownLatch(2);

            final TestActor actor = new TestActor(mailboxName, clock, mvStoreManager) {
                @Override
                void process(@NotNull String payload, @NotNull Transaction tx) {
                    super.process(payload, tx);
                    processed.countDown();
                }
            };

            final Thread actorThread = Thread.ofVirtual()
                    .name("testactor-", 0)
                    .start(actor::mainLoop);

            assertTrue(processed.await(5, TimeUnit.SECONDS), "Messages were not processed in time");

            actorThread.interrupt();
            actorThread.join(Duration.ofSeconds(5));
            assertFalse(actorThread.isAlive(), "Actor thread should have stopped");
        }
    }

    static class TestActor extends Actor {

        protected TestActor(@NotNull String mailboxName, @NotNull Clock clock, @NotNull MvStoreManager mvStoreManager) {
            super(mailboxName, clock, mvStoreManager);
        }

        @Override
        void process(@NotNull String payload, @NotNull Transaction transaction) {
            System.out.println("processing payload: " + payload);
        }

        @Override
        public void mainLoop() {
            System.out.println( "main loop enter");
            super.mainLoop();
            System.out.println( "main loop exit");
        }
    }
}