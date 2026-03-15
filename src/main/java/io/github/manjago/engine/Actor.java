package io.github.manjago.engine;

import io.github.manjago.PanicException;
import org.h2.mvstore.tx.Transaction;
import org.h2.mvstore.tx.TransactionMap;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.parkUntil;

public abstract class Actor {
    private final String mailboxName;
    private final Clock clock;
    private final MvStoreManager mvStoreManager;
    private final RetryOrDlqLogic retryOrDlqLogic;

    protected Actor(@NotNull String mailboxName,
            @NotNull Clock clock,
            @NotNull MvStoreManager mvStoreManager,
            RetryOrDlqLogic retryOrDlqLogic) {
        this.mailboxName = mailboxName;
        this.clock = clock;
        this.mvStoreManager = mvStoreManager;
        this.retryOrDlqLogic = retryOrDlqLogic;
    }

    private static void parkOrContinue(@NotNull LoopResult result) {
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
            case LoopResult.Retried _ -> {
                // счетчик увеличили, идем на следующую итерацию (сообщение в очереди оставлено)
            }
            case LoopResult.SentToDlq _ -> {
                // сообщение из очереди убрали, поместили в DLQ, идем на следующую итерацию
            }
        }
    }

    public void mainLoop() {
        while (!Thread.interrupted()) {
            final LoopResult result = processOneEvent(Instant.now(clock));
            // паркуемся уже вне транзакции — правильно
            parkOrContinue(result);
        }
    }

    @NotNull
    private LoopResult processOneEvent(Instant now) {

        record FailureContext(QueueKey key, String payload) {
        }
        final AtomicReference<FailureContext> failureRef = new AtomicReference<>();

        try {
            return mvStoreManager.runInTransactionWithResult(tx -> {

                final TransactionMap<QueueKey, String> mailbox = StoreSchema.openMailbox(tx, mailboxName);
                final QueueKey firstKey = mailbox.firstKey();

                if (firstKey == null) {
                    return LoopResult.Empty.INSTANCE;
                } else if (firstKey.timestamp() <= now.toEpochMilli()) {
                    return processOneEvent(tx, mailbox, firstKey) ? LoopResult.ProcessNow.INSTANCE :
                            LoopResult.Duplicate.INSTANCE;
                } else {
                    return new LoopResult.SleepUntil(firstKey.timestamp());
                }
            });

        } catch (Exception e) {
            var ctx = failureRef.get();
            if (ctx == null) {
                // failureRef не успели установить → упало ДО process()
                throw new PanicException("Infrastructure failure in actor " + mailboxName, e);
            }
            return handleRetryOrDlq(ctx.key(), e);
        }
    }

    private LoopResult handleRetryOrDlq(QueueKey key, Exception e) {
        final RetryOrDlqLogic.Result result = retryOrDlqLogic.handleException(key, e, mailboxName);
        if (result == RetryOrDlqLogic.Result.SENT_TO_DLQ) {
            return LoopResult.SentToDlq.INSTANCE;
        } else  {
            return LoopResult.Retried.INSTANCE;
        }
    }

    private boolean processOneEvent(@NotNull Transaction tx,
            @NotNull TransactionMap<QueueKey, String> mailbox,
            @NotNull QueueKey firstKey) {
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
        retryOrDlqLogic.handleSuccess(firstKey.uuid());
        return result;
    }

    abstract void process(@NotNull String payload, @NotNull Transaction transaction);

    sealed interface LoopResult permits LoopResult.Duplicate, LoopResult.Empty, LoopResult.ProcessNow,
            LoopResult.Retried, LoopResult.SentToDlq, LoopResult.SleepUntil {

        enum Empty implements LoopResult {INSTANCE}

        enum ProcessNow implements LoopResult {INSTANCE}

        enum Duplicate implements LoopResult {INSTANCE}

        enum SentToDlq implements LoopResult {INSTANCE}

        enum Retried implements LoopResult {INSTANCE}

        record SleepUntil(long timestampMs) implements LoopResult {
        }

    }
}
