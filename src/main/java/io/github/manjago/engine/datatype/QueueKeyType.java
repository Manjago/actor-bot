package io.github.manjago.engine.datatype;

import io.github.manjago.engine.QueueKey;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;

public class QueueKeyType extends BasicDataType<QueueKey> {

    @Override
    public int compare(QueueKey a, QueueKey b) {
        return a.compareTo(b);
    }

    @Override
    public int getMemory(QueueKey obj) {
        // 8 (timestamp) + 16 (UUID) = 24 байта данных, и добавляем оверхед объекта 16 - получаем 40
        // лучше взять немножко больше, чем немножко меньше

        // getMemory() используется MVStore для оценки размера данных в памяти — на основе
        // этого он решает, когда пора сбрасывать страницы (chunks) на диск, чтобы не превысить
        // лимит autoCommitBufferSize.

        // Если занизить — MVStore будет думать, что в памяти мало данных, будет реже сбрасывать на диск,
        // и в итоге может съесть больше RAM, чем ожидалось.

        // Если завысить — будет сбрасывать чаще, чем нужно. Чуть больше I/O, но ничего страшного.

        return 40;
    }

    // Запись ОДНОГО объекта
    @Override
    public void write(@NotNull WriteBuffer buff, @NotNull QueueKey obj) {
        buff.putLong(obj.timestamp());
        buff.putLong(obj.uuid().getMostSignificantBits());
        buff.putLong(obj.uuid().getLeastSignificantBits());
    }

    // Чтение ОДНОГО объекта
    @Override
    public QueueKey read(@NotNull ByteBuffer buff) {
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
