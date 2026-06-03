package clarson.ftc.faker.util;

import clarson.ftc.faker.updater.Updateable;

import java.util.function.BooleanSupplier;

public interface TimedStateGetter extends BooleanSupplier, Updateable {
    /**
     * Alias for `getAsBoolean()`.
     */
    default public boolean getState() {
        return getAsBoolean();
    }
}