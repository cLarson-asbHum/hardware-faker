package clarson.ftc.faker.util;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Creates a TimedStateGetter with a boolean generator.  
 */
public final class EasyTimedStateGetter implements TimedStateGetter {
    private final Function<Double, Boolean> booleanGenerator;
    private boolean lastState;
    private boolean isUpdatingEnabled = true;

    /**
     * Constructs a TimedStateGetter whose getAsBoolean() and getState() methods return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * Boolean generator, and the first return value from it will be used as the return of 
     * the new TimedStateGetter's getAsBoolean() and getState() methods until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * Becuase the provided BooleanSupplier takes no parameters, the generation of the 
     * booleans is independent of the time.
     * 
     * @param supplier What determines the return value of getAsBoolean() and getState(). 
     */
    public EasyTimedStateGetter(BooleanSupplier supplier) {
        this((unused) -> supplier.getAsBoolean());
    }

    /**
     * Constructs a TimedStateGetter whose getAsBoolean() and getState() methods return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * Boolean generator, and the first return value from it will be used as the return of 
     * the new TimedStateGetter's getAsBoolean() and getState() methods until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * The Double paramter of the boolean generator is the `deltaSec` argument of the call to 
     * update which calls the generator's apply() method.
     * 
     * @param supplier What determines the return value of getAsBoolean() and getState(). 
     */
    public EasyTimedStateGetter(Function<Double, Boolean> booleanGenerator) {
        this.booleanGenerator = booleanGenerator;
        this.lastState = booleanGenerator.apply(0.0);
    }

    /**
     * Gets the most recent value from the Boolean generator. Such value is updated whenever 
     * update() is called succesfully (aka return of update() == 1).
     * 
     * @return The last value obtained from the supplier provided at construction. 
     */
    @Override
    public boolean getAsBoolean() {
        return lastState;
    }

    /**
     * Attempts to update the value returned by `getAsBoolean()`. In other words, `getAsBoolean()`
     * may return a different value after this method is called (succesfully); this is the only
     * way by which it can return a different value. 
     * 
     * An invocation may be unsuccesful either because updating is not enabled (see 
     * `setUpdatingIsEnabled()` and `isUpdatingEnabled()`), or because the deltaSec argument was 
     * 0. In both cases, the return value of getAsBoolean() is guaranteed to be the same as it was 
     * before. Note that a succesful update call does not guarantee that the value *will* differ;
     * rather, it will simply be what ever the boolean generator provided at construction will 
     * return. 
     * 
     * @param deltaSec The time (in seconds) that has passed since the last call to update().
     * Can be any number, but the update will be unsucessful if it is 0.
     * @return 0 if the update was unsuccesful; 1 otherwise. 
     */
    @Override
    public double update(double deltaSec) {
        if(!isUpdatingEnabled || deltaSec == 0) {
            return 0;
        }

        // All conditions were met; updating the lastState
        this.lastState = booleanGenerator.apply(deltaSec);
        return 1;
    }

    @Override
    public boolean isUpdatingEnabled() {
        return isUpdatingEnabled;
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        final boolean old = isUpdatingEnabled;
        this.isUpdatingEnabled = newUpdatingEnabled;
        return old != newUpdatingEnabled;
    }
}