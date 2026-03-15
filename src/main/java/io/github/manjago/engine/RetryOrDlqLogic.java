package io.github.manjago.engine;

import io.github.manjago.PanicException;
import org.h2.mvstore.tx.TransactionMap;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RetryOrDlqLogic {
    private static final int MAX_RETRIES = 3;
    private final MvStoreManager mvStoreManager;
    private final Clock clock;
    private final ConcurrentMap<UUID, Integer> retries = new ConcurrentHashMap<>();

    public RetryOrDlqLogic(MvStoreManager mvStoreManager, Clock clock) {
        this.mvStoreManager = mvStoreManager;
        this.clock = clock;
    }

    public enum Result {
        SENT_TO_DLQ, RETRIED
    }


    // rollback произошел, транзакция завершена, проблемное сообщение в очереди
    @NotNull
    public Result handleException(@NotNull QueueKey key, Exception e, String mailboxName) {
        final UUID uuid = key.uuid();

        final int attempts = retries.merge(uuid, 1, Integer::sum);

        if (attempts > MAX_RETRIES) {
            retries.remove(uuid);
            try {
                sendToDlq(key, e, mailboxName, attempts);
            } catch (Exception ex) {
                throw new PanicException("Fail put message with key + " + key + " from " + mailboxName + " to DLQ", ex);
            }
            return Result.SENT_TO_DLQ;
        } else {
            // ничего делать не надо, проблемное сообщение в очереди
            return Result.RETRIED;
        }
    }

    private void sendToDlq(QueueKey key, Exception e, String mailboxName, int attempts) {
        mvStoreManager.runInTransaction(tx -> {
            final TransactionMap<QueueKey, String> mailbox = StoreSchema.openMailbox(tx, mailboxName);
            final String problemPayload = mailbox.get(key);
            mailbox.remove(key);
            final TransactionMap<UUID, DlqEntry> dlq = StoreSchema.openDlq(tx);
            dlq.put(key.uuid(), new DlqEntry(
                    problemPayload,
                    mailboxName,
                    e != null ? ExceptionUtils.getRootCauseMessage(e) : null,
                    e != null ? ExceptionUtils.getStackTrace(e) : null,
                    clock.instant(),
                    attempts));
        });
    }

    public void handleSuccess(UUID uuid) {
        retries.remove(uuid);
    }

}
