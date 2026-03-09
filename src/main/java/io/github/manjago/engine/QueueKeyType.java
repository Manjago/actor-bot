package io.github.manjago.engine;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

import java.nio.ByteBuffer;
import java.util.UUID;

public class QueueKeyType extends BasicDataType<QueueKey> {

    @Override
    public int compare(QueueKey a, QueueKey b) {
        return a.compareTo(b);
    }

    @Override
    public int getMemory(QueueKey obj) {
        // 8 (timestamp) + 16 (UUID) = 24 байта данных + оверхед объекта
        return 40;
    }

    // Запись ОДНОГО объекта
    @Override
    public void write(WriteBuffer buff, QueueKey obj) {
        buff.putLong(obj.timestamp());
        buff.putLong(obj.uuid().getMostSignificantBits());
        buff.putLong(obj.uuid().getLeastSignificantBits());
    }

    // Чтение ОДНОГО объекта
    @Override
    public QueueKey read(ByteBuffer buff) {
        long timestamp = buff.getLong();
        long mostSigBits = buff.getLong();
        long leastSigBits = buff.getLong();
        return new QueueKey(timestamp, new UUID(mostSigBits, leastSigBits));
    }

    // Создание массива для хранения (из DataType interface)
    @Override
    public QueueKey[] createStorage(int size) {
        return new QueueKey[size];
    }
}
