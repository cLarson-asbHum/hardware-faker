package clarson.ftc.faker.util;

import clarson.ftc.faker.updater.Updateable;

import java.util.function.Supplier;
import java.util.function.Function;

/**
 * A Supplier which only returns a new value upon an invocation of the update() 
 * method.
 */
public class UpdateableSupplier<T> implements Updateable, Supplier<T> {
    private final Function<Double, T> generator;
    private T lastValue;
    private boolean isUpdatingEnabled = true;

    /**
     * Constructs a Supplier whose get() return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * generator, and the first return value from it will be used as the return of 
     * the new Supplier's get() until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * Becuase the provided Supplier takes no parameters, the generation of the 
     * values is independent of the time.
     * 
     * @param supplier What determines the return value of get(). 
     */
    public UpdateableSupplier(Supplier<T> supplier) {
        this((unused) -> supplier.get());
    }

    /**
     * Constructs a Supplier whose get() return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * generator, and the first return value from it will be used as the return of 
     * the new Supplier's get() until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * The Double paramter of the generator is the `deltaSec` argument of the call to 
     * update which calls the generator's apply() method.
     * 
     * @param supplier What determines the return value of get(). 
     */
    public UpdateableSupplier(Function<Double, T> generator) {
        this.generator = generator;
        this.lastValue = generator.apply(0.0);
    }

    /**
     * Gets the most recent value from the generator. Such value is updated whenever 
     * update() is called succesfully (aka return of update() == 1).
     * 
     * @return The last value obtained from the supplier provided at construction. 
     */
    @Override
    public T get() {
        return lastValue;
    }

    /**
     * Attempts to update the value returned by `get()`. In other words, `get()`
     * may return a different value after this method is called (succesfully); this is the only
     * way by which it can return a different value. 
     * 
     * An invocation may be unsuccesful either because updating is not enabled (see 
     * `setUpdatingIsEnabled()` and `isUpdatingEnabled()`), or because the deltaSec argument was 
     * 0. In both cases, the return value of get() is guaranteed to be the same as it was 
     * before. Note that a succesful update call does not guarantee that the value *will* differ;
     * rather, it will simply be what ever the generator provided at construction will 
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

        // All conditions were met; updating the lastValue
        this.lastValue = generator.apply(deltaSec);
        return 1;
    }

    @Override
    public final boolean isUpdatingEnabled() {
        return isUpdatingEnabled;
    }

    @Override
    public final boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        final boolean old = isUpdatingEnabled;
        this.isUpdatingEnabled = newUpdatingEnabled;
        return old != newUpdatingEnabled;
    }
}