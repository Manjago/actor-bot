package io.github.manjago;

/**
 * Если это исключение произошло - безусловно прерываем работу
 */
public class PanicException extends RuntimeException {
    public PanicException(String message) {
        super(message);
    }

    public PanicException(String message, Throwable cause) {
        super(message, cause);
    }
}
