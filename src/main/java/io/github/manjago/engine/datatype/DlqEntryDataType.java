package io.github.manjago.engine.datatype;

import io.github.manjago.engine.DlqEntry;
import org.h2.mvstore.type.StringDataType;
import org.jetbrains.annotations.NotNull;

public class DlqEntryDataType extends JsonDataType<DlqEntry> {

    private static final StringDataType STRING_DATA_TYPE = new StringDataType();

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
        size +=  STRING_DATA_TYPE.getMemory(obj.originalPayload());
        size +=  STRING_DATA_TYPE.getMemory(obj.actorName());
        size +=  STRING_DATA_TYPE.getMemory(obj.errorMessage());
        size +=  STRING_DATA_TYPE.getMemory(obj.stackTrace());
        size +=  8;
        size +=  8;
        return size;
    }



}