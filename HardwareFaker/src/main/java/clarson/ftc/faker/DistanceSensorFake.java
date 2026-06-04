/*
 * DistanceSensorFake.java
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

package clarson.ftc.faker;

import clarson.ftc.faker.updater.SimulateDelay;

import clarson.ftc.faker.function.DistanceGetter;
import clarson.ftc.faker.function.TimedDistanceGetter;
import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.wrapper.SensorFakeBase;

import static clarson.ftc.faker.updater.UpdatesWhen.ALWAYS;

import com.qualcomm.robotcore.hardware.DistanceSensor;

import java.util.HashSet;
import java.util.Set;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Represents a sensor that reads a distance. This serves as a wrapper for 
 * DistanceGetters and TimedDistanceGetters, able to be consumed by a hardware map
 * and able to simulate delay of reading I2C.
 */
public class DistanceSensorFake extends SensorFakeBase<TimedDistanceGetter> implements DistanceSensor, TwoWayUpdateable {
    protected final Set<Updater> rememberedUpdaters = new HashSet<>();

    /**
     * Creates a distance sensor whose getDistance() is implemented by the given 
     * object. The given object is converted to a TimedDistanceGetter that can never be 
     * updated (gotcha: its `isUpdatingEnabled()` method can return true, although this functionally 
     * means nothing).
     * 
     * @param distanceSupplier What is called every time DistanceSensorFake.getDistance() is called
     */
    public DistanceSensorFake(DistanceGetter distanceGetter) {
        this(new TimedDistanceGetter() {
            private boolean isUpdatingEnabled = true;

            @Override
            public double getDistance(DistanceUnit units) {
                return distanceGetter.getDistance(units);
            }

            @Override
            public double update(double deltaSec) {
                // This does nothing by default; return 0.
                return 0;
            }

            @Override
            public boolean isUpdatingEnabled() {
                return isUpdatingEnabled;
            }

            @Override
            public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
                final boolean oldValue = isUpdatingEnabled;
                this.isUpdatingEnabled = newUpdatingEnabled;
                return oldValue != newUpdatingEnabled;
            }
        });
    }
    
    /**
     * Creates a distance sensor whose getDistance() is implemented by the given 
     * object. The given distance getter is updated every call to DistanceSensorFake.update.
     * 
     * NOTE: The given distance getter can be added to an Updater if the getter's `setIsUpdatingEnabled()` 
     * and `update()` methods are implemented correctly. This allows the getter to only be updated once
     * per call to `Updater.updateAllOnlyOnce()`, as intended.
     * 
     * @param distanceSupplier What is called every time DistanceSensorFake.getDistance() is called
     */
    public DistanceSensorFake(TimedDistanceGetter distanceGetter) {
        super(distanceGetter);
    }

    @Override
    @SimulateDelay(ALWAYS)
    public double getDistance(DistanceUnit resultUnits) {
        Updater.updateAllOnce(rememberedUpdaters, Updater.UpdateDelaySource.I2C);
        return underlyingGetter.getDistance(resultUnits);
    }

    /**
     * NOTE: Does not call the internal TimedDistanceGetter's `remember()` method
     * 
     * @param updater What to remember
     */
    @Override
    public void remember(Updater updater) {
        rememberedUpdaters.add(updater);
    }

    /**
     * NOTE: Does not call the internal TimedDistanceGetter's `forget()` method
     * 
     * @param updater What to forget
     */
    @Override
    public void forget(Updater updater) {
        rememberedUpdaters.remove(updater);
    }

    @Override
    public String getDeviceName() {
        return "DistanceSensorFake";
    }
}