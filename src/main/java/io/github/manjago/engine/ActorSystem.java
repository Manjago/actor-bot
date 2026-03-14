package io.github.manjago.engine;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import org.h2.mvstore.type.StringDataType;

import java.time.Clock;
import java.time.Instant;

public class ActorSystem {
    private static final TimeBasedEpochGenerator UUID_GENERATOR = Generators.timeBasedEpochGenerator();
    private final MvStoreManager mvStoreManager;
    private final Clock clock;

    public ActorSystem(MvStoreManager mvStoreManager, Clock clock) {
        this.mvStoreManager = mvStoreManager;
        this.clock = clock;
    }

    public void send(String mailboxName, String payload, Instant fireDate) {
        mvStoreManager.runInTransaction(tx -> tx.openMap(mailboxName, new QueueKeyType(), StringDataType.INSTANCE).put(new QueueKey(fireDate, UUID_GENERATOR.generate()), payload));
    }

    public void send(String mailboxName, String payload) {
        send(mailboxName, payload, clock.instant());
    }

}
