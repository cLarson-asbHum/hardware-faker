/*
 * EasyTimedDistanceGetter.java
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

import clarson.ftc.faker.function.DistanceGetter;
import clarson.ftc.faker.function.TimedDistanceGetter;
import java.util.function.BiFunction;
import java.util.function.DoubleSupplier;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Creates a TimedDistanceGetter with a double generator.  
 */
public final class EasyTimedDistanceGetter extends UpdateableSupplier<Double> implements TimedDistanceGetter {
    public static final DistanceUnit DEFAULT_UNIT = DistanceUnit.INCH;

    /**
     * Constructs a TimedDistanceGetter whose getDistance() method return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * Double generator, and the first return value from it will be used as the return of 
     * the new TimedDistanceGetter's getDistance() method until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * Becuase the provided DistanceGetter takes no Double parameters, the generation of 
     * the distances is independent of the time.
     * 
     * The default units of the `get()` method are given by the static `DEFAULT_UNIT` 
     * field. 
     * 
     * @param supplier What determines the return value of getDistance(). 
     */
    public EasyTimedDistanceGetter(DistanceGetter supplier) {
        this((DistanceUnit unit, Double unused) -> supplier.getDistance(unit));
    }
    
    /**
     * Constructs a TimedDistanceGetter whose getDistance() method return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * Double generator, and the first return value from it will be used as the return of 
     * the new TimedDistanceGetter's getDistance() method until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * Becuase the provided DistanceGetter takes no Double parameters, the generation of 
     * the distances is independent of the time.
     * 
     * The default units of the `get()` method are given by the static `DEFAULT_UNIT` 
     * field. 
     * 
     * @param supplier What determines the return value of getDistance(). The supplier's
     * units are assumed to be DEFAULT_UNIT.
     */
    public EasyTimedDistanceGetter(DoubleSupplier supplier) {
        this((DistanceUnit unit, Double unused) -> unit.fromUnit(DEFAULT_UNIT, supplier.getAsDouble()));
    }

    /**
     * Constructs a TimedDistanceGetter whose getDistance() method return
     * values obtained from invoking the provided supplier. Construction invokes the 
     * Double generator, and the first return value from it will be used as the return of 
     * the new TimedDistanceGetter's getDistance() method until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * The Double paramter of the double generator is the `deltaSec` argument of the call to 
     * update which calls the generator's apply() method.
     * 
     * The default units of the `get()` method are given by the static `DEFAULT_UNIT` 
     * field. 
     * 
     * @param supplier What determines the return value of getDistance(). 
     */
    public EasyTimedDistanceGetter(BiFunction<DistanceUnit, Double, Double> distanceGenerator) {
        super((deltaSec) -> distanceGenerator.apply(DEFAULT_UNIT, deltaSec));
    }

    /**
     * Gets the most recent value from the Double generator. Such value is updated whenever 
     * update() is called succesfully (aka return of update() == 1).
     * 
     * @return The last value obtained from the supplier provided at construction. 
     */
    @Override
    public double getDistance(DistanceUnit units) {
        return units.fromUnit(DEFAULT_UNIT, this.get() /* <-- Already in the default unit */);
    }
}