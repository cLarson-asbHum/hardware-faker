/*
 * AnalogTouchSensorFake.java
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

import clarson.ftc.faker.function.TimedDoubleSupplier;
import clarson.ftc.faker.function.TimedVoltageGetter;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.SimulateDelay;
import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.EasyTimedDoubleSupplier;
import com.qualcomm.hardware.lynx.commands.core.LynxGetADCCommand;
import com.qualcomm.robotcore.hardware.TouchSensor;
import java.util.function.DoubleSupplier;
import java.util.HashSet;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;
import static clarson.ftc.faker.updater.Updater.UpdateDelaySource.ANALOG;
import static clarson.ftc.faker.updater.UpdatesWhen.ALWAYS;
import static clarson.ftc.faker.updater.UpdatesWhen.ON_BULK_READS;

/**
 * A touch sensor which resides on an analog controller and whose output 
 * is continuous. In contrast to DigitalTouchSensorFake, the output of
 * the `getValue()` method can be anything between and including 0 and 1.
 * The `isPressed()` value returns true if getValue() is above some threshold
 * provided at construction.
 * 
 * All delays simulated by this fake are the length of a analog analog input's 
 * delay (see `#Updater.UpdateDelaySource.ANALOG`).
 */
public class AnalogTouchSensorFake implements TouchSensor, TwoWayUpdateable {
    private static TimedVoltageGetter voltsFromTouch(TimedDoubleSupplier getValue, double maxVolts) {
        return new TimedVoltageGetter() {
            @Override
            public double getVoltage(VoltageUnit units) {
                return maxVolts * getValue.getAsDouble();
            }

            @Override
            public double update(double deltaSec) {
                return getValue.update(deltaSec);
            }

            @Override
            public boolean isUpdatingEnabled() {
                return getValue.isUpdatingEnabled();
            }

            @Override
            public boolean setUpdatingEnabled(boolean updatingEnabled) {
                return getValue.setUpdatingEnabled(updatingEnabled);
            }
        };
    }

    private final HashSet<Updater> updaters = new HashSet<>();
    private final AnalogInputFake underlyingInput;
    private final double threshold;

    public AnalogTouchSensorFake(DoubleSupplier getValue, double threshold) {
        this(new EasyTimedDoubleSupplier(getValue), threshold);
    }
    
    public AnalogTouchSensorFake(TimedDoubleSupplier getValue, double threshold) {
        this(getValue, threshold, AnalogInputControllerFake.createPossiblyWithNullModule());
    }
    
    public AnalogTouchSensorFake(DoubleSupplier getValue, double threshold, AnalogInputControllerFake controller) {
        this(new EasyTimedDoubleSupplier(getValue), threshold, controller);
    }

    public AnalogTouchSensorFake(TimedDoubleSupplier getValue, double threshold, AnalogInputControllerFake controller) {
        this.threshold = threshold;
        underlyingInput = new AnalogInputFake(
            voltsFromTouch(getValue, controller.getMaxAnalogInputVoltage()), controller);
    }
    
    public AnalogTouchSensorFake(DoubleSupplier getValue, double threshold, AnalogInputControllerFake controller, int port) {
        this(new EasyTimedDoubleSupplier(getValue), threshold, controller, port);
    }

    public AnalogTouchSensorFake(TimedDoubleSupplier getValue, double threshold, AnalogInputControllerFake controller, int port) {
        this.threshold = threshold;
        underlyingInput = new AnalogInputFake(
            voltsFromTouch(getValue, controller.getMaxAnalogInputVoltage()), controller, port);
    }

    public AnalogInputFake asAnalogInput() {
        return this.underlyingInput;
    }

    public AnalogInputControllerFake getControllerFake() {
        return asAnalogInput().getControllerFake();
    }

    public int getPortNumber() {
        return asAnalogInput().getPortNumber();
    }

    /**
     * Returns true if the return of `getValue()` is above the threshold provided
     * at construction.
     * 
     * @return Whether the sensor is pressed enough to be considered pressed.
     */
    @SimulateDelay(ON_BULK_READS)
    @Override
    public boolean isPressed() {
        return getValue() >= threshold; // getValue() simulates delay
    }

    @SimulateDelay(ON_BULK_READS)
    @Override
    public double getValue() {
        // NOTE: underlyingInput does not simulate delay, because it isn't registered with any updaters
        
        // Simulating delay only if this method was called by the user, not by any 
        // internal methods. This is done to prevent Updater.updateAll() from being 
        // called multiple times by LynxModuleUsbDeviceImplFake.readBulkDataPayload()
        if(this.getControllerFake().shouldReread()) {
            return underlyingInput.getVoltage() / underlyingInput.getMaxVoltage();
        }

        // The method was called by a user rather than an internal method; 
        // simulate delay.
        final LynxGetADCCommand command = new LynxGetADCCommand(
            this.getControllerFake().getLynxModule(),
            LynxGetADCCommand.Channel.user(this.getPortNumber()),
            LynxGetADCCommand.Mode.ENGINEERING
        );

        ModularUpdater.updateAllOnceIfAnyCacheOutdated(updaters, ANALOG, this, command);
        return underlyingInput.getVoltage() / underlyingInput.getMaxVoltage();
    }

    @Override
    public void remember(Updater updater) {
        updaters.add(updater);
    }

    @Override
    public void forget(Updater updater) {
        updaters.remove(updater);
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        return underlyingInput.setUpdatingEnabled(newUpdatingEnabled);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return underlyingInput.isUpdatingEnabled();
    }

    @Override
    public double update(double deltaSec) {
        return underlyingInput.update(deltaSec);
    }

    @Override
    public String getDeviceName() {
        return "AnalogTouchSensorFake";
    }

    @Override
    public void close() {
        underlyingInput.close();
    }

    @Override
    public void resetDeviceConfigurationForOpMode() {
        underlyingInput.resetDeviceConfigurationForOpMode();
    }

    @Override
    public int getVersion() {
        return underlyingInput.getVersion();
    }

    @Override
    public String getConnectionInfo() {
        return underlyingInput.getControllerFake().getDeviceName() 
                + "; port " 
                + underlyingInput.getPortNumber();
    }

    @Override
    public Manufacturer getManufacturer() {
        return null;
    }
    
}