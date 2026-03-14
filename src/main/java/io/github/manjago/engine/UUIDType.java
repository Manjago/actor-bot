package io.github.manjago.engine;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDType extends BasicDataType<UUID> {

    @Override
    public int compare(UUID a, UUID b) {
        return a.compareTo(b);
    }

    @Override
    public int getMemory(UUID obj) {
        // 16 байт данных UUID, и добавляем оверхед объекта 16 - получаем 32
        // лучше взять немножко больше, чем немножко меньше

        // getMemory() используется MVStore для оценки размера данных в памяти — на основе
        // этого он решает, когда пора сбрасывать страницы (chunks) на диск, чтобы не превысить
        // лимит autoCommitBufferSize.

        // Если занизить — MVStore будет думать, что в памяти мало данных, будет реже сбрасывать на диск,
        // и в итоге может съесть больше RAM, чем ожидалось.

        // Если завысить — будет сбрасывать чаще, чем нужно. Чуть больше I/O, но ничего страшного.

        return 32;
    }

    // Запись ОДНОГО объекта
    @Override
    public void write(@NotNull WriteBuffer buff, @NotNull UUID obj) {
        buff.putLong(obj.getMostSignificantBits());
        buff.putLong(obj.getLeastSignificantBits());
    }

    // Чтение ОДНОГО объекта
    @Override
    public UUID read(@NotNull ByteBuffer buff) {
        long mostSigBits = buff.getLong();
        long leastSigBits = buff.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    // Создание массива для хранения (из DataType interface)
    @Override
    public UUID[] createStorage(int size) {
        return new UUID[size];
    }
}
