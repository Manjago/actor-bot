package io.github.manjago.engine.datatype;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;

public abstract class JsonDataType<T> extends BasicDataType<T> {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private final Class<T> type;

    protected JsonDataType(Class<T> type) {
        this.type = type;
    }

    @Override
    public int compare(T a, T b) {
        throw new UnsupportedOperationException("Json structure not comparable!");
    }

    @Override
    public void write(WriteBuffer buff, T obj) {
        try {
            final byte[] json = MAPPER.writeValueAsBytes(obj);
            buff.putInt(json.length);
            buff.put(json);
        } catch (Exception e) {
            throw new JsonDataTypeSerializationException("Failed to serialize " + type.getSimpleName(), e);
        }
    }

    @Override
    public T read(ByteBuffer buff) {
        try {
            final int length = buff.getInt();
            final byte[] json = new byte[length];
            buff.get(json);
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new JsonDataTypeSerializationException("Failed to deserialize " + type.getSimpleName(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] createStorage(int size) {
        // да, не пугаемся словва reflect - публичный API, борьба разработчиков с рефлексией его не касается!
        return (T[]) java.lang.reflect.Array.newInstance(type, size);
    }

    public static class JsonDataTypeSerializationException extends RuntimeException {
        public JsonDataTypeSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}