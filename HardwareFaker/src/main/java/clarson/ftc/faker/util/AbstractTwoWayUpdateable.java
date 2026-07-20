/*
 * AbstractTwoWayUpdateable.java
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

import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import java.util.function.DoubleFunction;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements methods of the TwoWayUpdateable interface which have standard implementations.
 * Methods such as `setUpdatingEnabled()` and `isUpdatingEnabled()` have the same implementation
 * across several different classes. 
 * 
 * This class is useful even when a one-way Updateable is desired rather than a TwoWayUpdateable;
 * an instance of AbstractTwoWayUpdateable can be created with the compose() static method, rather 
 * than subclassing it directly. From there, the methods of the instance can be composed into 
 * the desired one-way Updateable class, as illustrated below:
 * 
 * ```java
 * public class OneWayUpdateable implements Updateable { // Not TwoWayUpdatable
 *     private Updateable updateHandler = AbstractTwoWayUpdateable.compose(this::doThings);
 * 
 *     public double update(double deltaSec) {
 *         return updateHandler.update(deltaSec);
 *     }
 * 
 *     public boolean isUpdatingEnabled() {
 *         return updateHandler.isUpdatingEnabled();
 *     }
 * 
 *     public boolean setIsUpdatingEnabled(boolean newValue) {
 *         return updateHandler.setUpdatingEnabled(newValue);
 *     }
 * 
 *     private double doThings(double deltaSec) {
 *         // (Implementation not shown)
 *     }
 * }
 * ```
 */
public abstract class AbstractTwoWayUpdateable implements TwoWayUpdateable {
    /**
     * Implements an AbstractTwoWayUpdateable using the given implementation for the
     * `updateImplemenation()` method. This is useful for creating Updateables without
     * needing to continuously subclass.
     * 
     * @param updateImplementation The method used for 
     * @return A new AbstractTwoWayUpdateable with an implemented updateImplementation() method.
     */
    public static final AbstractTwoWayUpdateable compose(final DoubleFunction<Double> updateImplementation) {
        return new AbstractTwoWayUpdateable() {
            @Override
            protected final double updateImplementation(double deltaSec) {
                return updateImplementation.apply(deltaSec);
            }
        };
    }

    private Set<Updater> updaters = new HashSet<>();
    private boolean isUpdatingEnabled = true;


    /**
     * Does all the actions that a succesful call to `update()` would. In other words, 
     * update() relies on this method when it is enabled and the deltaSec was non-zero.
     * This means that the implementation of `updateImplementation()` should therefore
     * **not** verify that the time is not zero.
     * 
     * @param deltaSec The number of seconds that elapsed since the last call to update.
     * Can be negative or positive, but won't be zero.
     * @return The change in the Updateable's most relevant quantity. Should be 0 if
     * nothing was changed. If the call is succesful but the change is not easily 
     * quantifiable, 1.0 should be returend
     */
    protected abstract double updateImplementation(double deltaSec);

    @Override
    public final double update(double deltaSec) {
        if(!isUpdatingEnabled || deltaSec == 0) {
            return 0;
        }

        return this.updateImplementation(deltaSec);
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

    @Override
    public final void remember(Updater updater) {
        this.updaters.add(updater);
    }

    @Override
    public final void forget(Updater updater) {
        this.updaters.remove(updater);
    }
}