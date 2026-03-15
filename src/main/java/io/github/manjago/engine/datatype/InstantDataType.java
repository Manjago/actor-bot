package io.github.manjago.engine.datatype;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.time.Instant;

public class InstantDataType extends BasicDataType<Instant> {

    @Override
    public int compare(Instant a, Instant b) {
        return a.compareTo(b);
    }

    @Override
    public int getMemory(Instant obj) {
        // 8 байт данных Instant, и добавляем оверхед объекта 16 - получаем 24
        // лучше взять немножко больше, чем немножко меньше

        // getMemory() используется MVStore для оценки размера данных в памяти — на основе
        // этого он решает, когда пора сбрасывать страницы (chunks) на диск, чтобы не превысить
        // лимит autoCommitBufferSize.

        // Если занизить — MVStore будет думать, что в памяти мало данных, будет реже сбрасывать на диск,
        // и в итоге может съесть больше RAM, чем ожидалось.

        // Если завысить — будет сбрасывать чаще, чем нужно. Чуть больше I/O, но ничего страшного.

        return 24;
    }

    // Запись ОДНОГО объекта
    @Override
    public void write(@NotNull WriteBuffer buff, @NotNull Instant obj) {
        buff.putLong(obj.toEpochMilli());
    }

    // Чтение ОДНОГО объекта
    @Override
    public Instant read(@NotNull ByteBuffer buff) {
        final long epochMilli = buff.getLong();
        return Instant.ofEpochMilli(epochMilli);
    }

    // Создание массива для хранения (из DataType interface)
    @Override
    public Instant[] createStorage(int size) {
        return new Instant[size];
    }
}
