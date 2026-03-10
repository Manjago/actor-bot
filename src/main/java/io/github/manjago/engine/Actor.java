package io.github.manjago.engine;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.tx.Transaction;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;

import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.parkUntil;

public abstract class Actor {
    private final MVMap<QueueKey, String> mailbox;
    private final Clock clock;
    private final MvStoreManager mvStoreManager;

    protected Actor(@NotNull MVMap<QueueKey, String> mailbox, @NotNull Clock clock, @NotNull MvStoreManager mvStoreManager) {
        this.mailbox = mailbox;
        this.clock = clock;
        this.mvStoreManager = mvStoreManager;
    }

    public void mainLoop() {
        while(!Thread.interrupted()) {
            final Instant now = Instant.now(clock);
            final QueueKey firstKey = mailbox.firstKey();
            if (firstKey == null) {
                // пусто — спим бессрочно, unpark разбудит или сами встанем
                park();
            } else if (firstKey.timestamp() <= now.toEpochMilli()) {
                // обрабатываем
                // timestamp положили с правильным clock! Ответственность за правильность - на кладущем
                mvStoreManager.runInTransaction(transaction -> {
                    process(mailbox.get(firstKey), transaction);
                    mailbox.remove(firstKey);
                });
            } else {
                // спим до этого времени, если что - нам скажут unpark и разбудят
                parkUntil(firstKey.timestamp());
            }


        }
    }

    abstract void process(@NotNull String payload, @NotNull Transaction transaction);
}
