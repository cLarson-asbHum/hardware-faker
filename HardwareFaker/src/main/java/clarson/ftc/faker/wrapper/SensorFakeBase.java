/*
 * SensorFakeBase.java
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

package clarson.ftc.faker.wrapper;

import clarson.ftc.faker.updater.Updateable;

import com.qualcomm.robotcore.hardware.HardwareDevice;

import java.util.HashSet;
import java.util.Set;

/**
 * Wraps a given getter implementing `Updateable` in such a way that allows
 * the wrapper to be added to an Updater and a HardwareMap while still acting exactly 
 * like the Supplier.
 * 
 * The intended use of this is to be overridden by custom fakes implementing sensor 
 * interfaces, such as DistanceSensor. 
 */
public class SensorFakeBase <T extends Updateable> implements HardwareDevice, Updateable {
    protected final T underlyingGetter;

    public SensorFakeBase(T underlyingGetter) {
        this.underlyingGetter = underlyingGetter;
    }

    /**
     * Updates the internal getter
     */
    @Override
    public double update(double deltaSec) {
        return underlyingGetter.update(deltaSec);
    }

    /**
     * Gets whether the internal getter is enabled.
     * 
     * @return Wether the internal getter is enabled.
     */
    @Override
    public boolean isUpdatingEnabled() {
        return underlyingGetter.isUpdatingEnabled();
    }

    /**
     * Sets whether the internal getter is enabled.
     * 
     * @param newUpdatingEnabled Whether the getter should be able to be updated
     * @return True if the new value is different from the previous value of isUpdatingEnabled 
     */
    @Override
    public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        return underlyingGetter.setUpdatingEnabled(newUpdatingEnabled);
    }

    @Override
    public void close() {
        // *Coding Concentratedly*
    }

    @Override
    public Manufacturer getManufacturer() {
        return null;
    }
    
    @Override
    public void resetDeviceConfigurationForOpMode() {
        // Oh! Sorry, I didn't notice you there.
    }

    @Override
    public int getVersion() {
        return -1;
    }

    @Override
    public String getConnectionInfo() {
        return "";
    }

    @Override
    public String getDeviceName() {
        return "";
    }
}