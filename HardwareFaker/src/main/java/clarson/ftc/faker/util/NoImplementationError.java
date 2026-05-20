package clarson.ftc.faker.util;

public class NoImplementationError extends Error {
    public NoImplementationError() {
        super();
    }

    public NoImplementationError(String message) {
        super(message);
    }

    public NoImplementationError(String message, Throwable cause) {
        super(message, cause);
    }

    public NoImplementationError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NoImplementationError(Throwable cause) {
        super(cause);
    }
}