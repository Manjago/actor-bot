package io.github.manjago.engine;

import io.github.manjago.engine.datatype.QueueKeyType;
import io.github.manjago.engine.datatype.UUIDDataType;
import org.h2.mvstore.tx.Transaction;
import org.h2.mvstore.tx.TransactionMap;
import org.h2.mvstore.type.LongDataType;
import org.h2.mvstore.type.StringDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class Utils {
    private static final String PROCESSED = "processed";
    private Utils() {
        // utility class
    }

    // 1) QueueKeyType -stateless, вызывать конструктор new QueueKeyType() - дешево, JIT оптимизирует
    // 2) Этот метод с openMap внутри цикла — это дёшево, H2 кэширует map по имени внутри store, каждый раз новый объект не создаётся.
    // 3) TransactionMap — это wrapper над MVMap с транзакционным контекстом, поэтому можно открывать его в цикле на каждой итерации из tx — правильный паттерн.
    public static TransactionMap<QueueKey, String> openMailbox(@NotNull Transaction tx, @NotNull String mailboxName) {
        return tx.openMap(mailboxName, new QueueKeyType(), StringDataType.INSTANCE);
    }

    public static TransactionMap<UUID, Long> openProcessed(@NotNull Transaction tx) {
        return tx.openMap(PROCESSED, new UUIDDataType(), LongDataType.INSTANCE);
    }

}
