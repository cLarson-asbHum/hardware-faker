/*
 * ElapsedTimeFake.java
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

import clarson.ftc.faker.updater.Updateable;

import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * A polymorphic ElapsedTime that relies on a setter method to get the current 
 * time, rather than relying on `System.now()`. This allows the time of an opmode
 * to be simulated rather than reliant on the system clock (and so actual time).
 * 
 * By implementing Updateable, this is able to be added to an updater and be 
 * synced with any global time updater.
 */
public class ElapsedTimeFake extends ElapsedTime implements Updateable {
    private long accumulatedNanos = 0;
    private boolean isUpdatingEnabled = true;

    public ElapsedTimeFake() {
        super();
    }
    
    public ElapsedTimeFake(long startTime) {
        super(startTime);
    }
    
    public ElapsedTimeFake(Resolution resolution) {
        super(resolution);
    }

    @Override
    public long nsNow() {
        return accumulatedNanos;
    }

    /**
     * @return Change in the number of nano seconds.
     */
    @Override
    public double update(double deltaSeconds) {
        accumulatedNanos += (long) (deltaSeconds * 1_000_000_000);
        return (long) (deltaSeconds * 1_000_000_000);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return isUpdatingEnabled;
    }

    @Override
    public boolean setUpdatingEnabled(boolean shouldBeEnabled) {
        final boolean oldValue = isUpdatingEnabled;
        isUpdatingEnabled = shouldBeEnabled;
        return shouldBeEnabled != oldValue;
    }
}