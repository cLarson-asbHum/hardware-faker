package clarson.ftc.faker.util;

public class MissingRequiredAttributeException extends Exception {
    MissingRequiredAttributeException(String message) {
        super(message);
    }

    MissingRequiredAttributeException(String message, Throwable cause) {
        super(message, cause);
    }
}