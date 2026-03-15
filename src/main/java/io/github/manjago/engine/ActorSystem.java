package io.github.manjago.engine;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import org.h2.mvstore.type.StringDataType;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;

public class ActorSystem {
    private static final TimeBasedEpochGenerator UUID_GENERATOR = Generators.timeBasedEpochGenerator();
    private final ConcurrentMap<String, Thread> mailboxes = new ConcurrentHashMap<>();
    private final MvStoreManager mvStoreManager;
    private final Clock clock;

    public ActorSystem(MvStoreManager mvStoreManager, Clock clock) {
        this.mvStoreManager = mvStoreManager;
        this.clock = clock;
    }

    public void register(@NotNull String mailboxName, @NotNull Thread thread) {
        mailboxes.put(mailboxName, thread);
        LockSupport.unpark(thread);
    }

    public void send(String mailboxName, String payload, Instant fireDate) {
        mvStoreManager.runInTransaction(tx -> Utils.openMailbox(tx, mailboxName).put(new QueueKey(fireDate, UUID_GENERATOR.generate()), payload));
        LockSupport.unpark(mailboxes.get(mailboxName));
    }

    public void send(String mailboxName, String payload) {
        send(mailboxName, payload, clock.instant());
    }

}
