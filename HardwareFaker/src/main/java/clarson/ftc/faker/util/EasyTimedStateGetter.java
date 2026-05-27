package clarson.ftc.faker.util;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Creates a TimedStateGetter with a boolean generator.  
 */
public final class EasyTimedStateGetter extends UpdateableSupplier<Boolean> implements TimedStateGetter {
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
        super(booleanGenerator);
    }

    /**
     * Gets the most recent value from the Boolean generator. Such value is updated whenever 
     * update() is called succesfully (aka return of update() == 1).
     * 
     * @return The last value obtained from the supplier provided at construction. 
     */
    @Override
    public boolean getAsBoolean() {
        return this.get();
    }
}