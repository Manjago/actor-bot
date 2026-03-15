package io.github.manjago.engine.datatype;

import io.github.manjago.engine.DlqEntry;
import org.h2.mvstore.type.StringDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DlqEntryDataType extends JsonDataType<DlqEntry> {

    private static final StringDataType STRING_DATA_TYPE = new StringDataType();
    private static final InstantDataType INSTANT_DATA_TYPE = new InstantDataType();

    public DlqEntryDataType() {
        super(DlqEntry.class);
    }

    @Override
    public int getMemory(@NotNull DlqEntry obj) {

        /*
        public record DlqEntry(
        String originalPayload,
        String actorName,
        String errorMessage,
        String stackTrace,
        Instant failedAt,
        int attempts)
         */

        int size = 16;
        size += getStringMemory(obj.originalPayload());
        size += getStringMemory(obj.actorName());
        size += getStringMemory(obj.errorMessage());
        size += getStringMemory(obj.stackTrace());
        size += INSTANT_DATA_TYPE.getMemory(obj.failedAt());
        size += 4; // int attempts 4 байта
        return size;
    }

    private int getStringMemory(@Nullable String str) {
        return str != null ? STRING_DATA_TYPE.getMemory(str) : 0;
    }


}