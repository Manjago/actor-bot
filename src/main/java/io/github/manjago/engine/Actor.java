package io.github.manjago.engine;

import org.h2.mvstore.tx.Transaction;
import org.h2.mvstore.tx.TransactionMap;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.parkUntil;

public abstract class Actor {
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

                final TransactionMap<QueueKey, String> mailbox = StoreSchema.openMailbox(tx, mailboxName);
                final QueueKey firstKey = mailbox.firstKey();

                if (firstKey == null) {
                    return LoopResult.Empty.INSTANCE;
                } else if (firstKey.timestamp() <= now.toEpochMilli()) {
                    return process(tx, mailbox, firstKey) ? LoopResult.ProcessNow.INSTANCE : LoopResult.Duplicate.INSTANCE;
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
                case LoopResult.Duplicate _ -> {
                    // не обработали, потому что обрабатывали раньше, сразу на следующую итерацию
                }
                case LoopResult.SleepUntil(long timestampMs) ->
                        parkUntil(timestampMs); // спим до этого времени, если что - нам скажут unpark и разбудят
            }
        }
    }

    private boolean process(@NotNull Transaction tx, @NotNull TransactionMap<QueueKey, String> mailbox, @NotNull QueueKey firstKey) {
        final TransactionMap<UUID, Instant> processed = StoreSchema.openProcessed(tx);
        final boolean result;
        // мы не запариваемся атомарностью, один актор с одним потоком, Check-And-Act можем
        if (processed.containsKey(firstKey.uuid())) {
            result = false;
        } else {
            process(mailbox.get(firstKey), tx);
            processed.put(firstKey.uuid(), Instant.now(clock));
            result = true;
        }
        // мы удаляем ключ всегда, обрабатывался ли он или пропущен из-за дедупликации
        // данные для дедупликации НЕ ВЕЧНЫ, у них тоже есть свой срок жизни,
        // не можем позволить себе ключам болтаться вечно
        mailbox.remove(firstKey);
        return result;
    }

    abstract void process(@NotNull String payload, @NotNull Transaction transaction);

    sealed interface LoopResult permits LoopResult.Duplicate, LoopResult.Empty, LoopResult.ProcessNow, LoopResult.SleepUntil {

        enum Empty implements LoopResult {INSTANCE}

        enum ProcessNow implements LoopResult {INSTANCE}

        enum Duplicate implements LoopResult {INSTANCE}

        record SleepUntil(long timestampMs) implements LoopResult {
        }
    }
}
