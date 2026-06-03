package clarson.ftc.faker.util;

public class UnsupportedLynxUsbCommandException extends RuntimeException {
    public UnsupportedLynxUsbCommandException(String message) {
        super(message);
    }

    public UnsupportedLynxUsbCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}