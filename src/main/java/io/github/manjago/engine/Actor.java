package io.github.manjago.engine;

import org.h2.mvstore.tx.Transaction;
import org.h2.mvstore.tx.TransactionMap;
import org.h2.mvstore.type.StringDataType;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;

import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.parkUntil;

public abstract class Actor {
    private static final String PROCESSED = "processed";
    private final String mailboxName;
    private final Clock clock;
    private final MvStoreManager mvStoreManager;

    protected Actor(@NotNull String mailboxName,
            @NotNull Clock clock,
            @NotNull MvStoreManager mvStoreManager) {
        this.mailboxName = mailboxName;
        this.clock = clock;
        this.mvStoreManager = mvStoreManager;
    }

    public void mainLoop() {
        while (!Thread.interrupted()) {

            final Instant now = Instant.now(clock);
            final LoopResult result = mvStoreManager.runInTransactionWithResult(tx -> {

                final TransactionMap<QueueKey, String> mailbox = Utils.openMailbox(tx, mailboxName);
                final QueueKey firstKey = mailbox.firstKey();

                if (firstKey == null) {
                    return LoopResult.Empty.INSTANCE;
                } else if (firstKey.timestamp() <= now.toEpochMilli()) {
                    process(mailbox.get(firstKey), tx);
                    mailbox.remove(firstKey);
                    return LoopResult.ProcessNow.INSTANCE;
                } else {
                    return new LoopResult.SleepUntil(firstKey.timestamp());
                }
            });


            // паркуемся уже вне транзакции — правильно
            switch (result) {
                case LoopResult.Empty _ -> park(); //пусто — спим бессрочно, unpark разбудит или сами встанем
                case LoopResult.ProcessNow _ -> {
                    // уже обработали, сразу на следующую итерацию
                }
                case LoopResult.SleepUntil(long timestampMs) ->
                        parkUntil(timestampMs); // спим до этого времени, если что - нам скажут unpark и разбудят
            }
        }
    }

    abstract void process(@NotNull String payload, @NotNull Transaction transaction);

    sealed interface LoopResult permits LoopResult.Empty, LoopResult.ProcessNow, LoopResult.SleepUntil {

        enum Empty implements LoopResult {INSTANCE}

        enum ProcessNow implements LoopResult {INSTANCE}

        record SleepUntil(long timestampMs) implements LoopResult {
        }
    }
}
