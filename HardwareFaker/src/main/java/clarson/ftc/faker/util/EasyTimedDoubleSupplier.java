/*
 * EasyTimedDoubleSupplier.java
 * 
 * Copyright 2026 Connor Larson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package clarson.ftc.faker.util;

import clarson.ftc.faker.function.TimedDoubleSupplier;
import java.util.function.Function;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleFunction;

/**
 * Creates a TimedDoubleSupplier with a double generator.  
 */
public final class EasyTimedDoubleSupplier extends UpdateableSupplier<Double> implements TimedDoubleSupplier {
    /**
     * Constructs a TimedDoubleSupplier whose getAsDouble() method return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * Double generator, and the first return value from it will be used as the return of 
     * the new TimedDoubleSupplier's getAsDouble() method until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * Becuase the provided DoubleSupplier takes no Double parameters, the generation of 
     * the doubles is independent of the time.
     * 
     * @param supplier What determines the return value of getAsDouble(). 
     */
    public EasyTimedDoubleSupplier(DoubleSupplier supplier) {
        this((double unused) -> supplier.getAsDouble());
    }

    /**
     * Constructs a TimedDoubleSupplier whose getAsDouble() method return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * Double generator, and the first return value from it will be used as the return of 
     * the new TimedDoubleSupplier's getAsDouble() method until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * The Double paramter of the double generator is the `deltaSec` argument of the call to 
     * update which calls the generator's apply() method.
     * 
     * @param supplier What determines the return value of getAsDouble(). 
     */
    public EasyTimedDoubleSupplier(DoubleFunction<Double> doubleGenerator) {
        super((deltaSec) -> doubleGenerator.apply(deltaSec));
    }

    /**
     * Gets the most recent value from the Double generator. Such value is updated whenever 
     * update() is called succesfully (aka return of update() == 1).
     * 
     * @return The last value obtained from the supplier provided at construction. 
     */
    @Override
    public double getAsDouble() {
        return this.get();
    }
}