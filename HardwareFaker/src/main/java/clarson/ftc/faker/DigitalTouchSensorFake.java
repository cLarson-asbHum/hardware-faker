/*
 * DigitalTouchSensorFake.java
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

import clarson.ftc.faker.function.TimedStateGetter;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.SimulateDelay;
import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.EasyTimedStateGetter;
import clarson.ftc.faker.wrapper.DigitalChannelData;
import com.qualcomm.hardware.lynx.commands.core.LynxGetSingleDIOInputCommand;
import com.qualcomm.robotcore.hardware.TouchSensor;
import java.util.function.BooleanSupplier;
import java.util.HashSet;
import static clarson.ftc.faker.updater.UpdatesWhen.ON_BULK_READS;

/**
 * A touch sensor which resides on a digital controller and whose output 
 * is strictly digital. In contrast to AnalogTouchSensorFake, the output of
 * the `getValue()` method is only 0 or 1.
 * 
 * All delays simulated by this fake are the length of a digital channel's 
 * delay (see `#Updater.UpdateDelaySource.DIGITAL`).
 */
public class DigitalTouchSensorFake implements TouchSensor, TwoWayUpdateable {
    /**
     * Creates an `INPUT` DigitalChannelData whose state getter returns the 
     * inverted value of the isPressed argument.
     * 
     * @param isPressed Whether the touch sensor is being pressed.
     * @return A DigitalChannelData in `INPUT`, with a null channel, and a
     * TimedStateGetter that return false when isPressed is true and vice versa.
     */
    private static DigitalChannelData invertedGetterData(TimedStateGetter isPressed) {
        // Inverting the output of isPressed
        return new DigitalChannelData(new TimedStateGetter() {
            @Override
            public boolean getAsBoolean() {
                return !isPressed.getState();
            }

            @Override
            public boolean setUpdatingEnabled(boolean newVal) {
                return isPressed.setUpdatingEnabled(newVal);
            }

            @Override
            public boolean isUpdatingEnabled() {
                return isPressed.isUpdatingEnabled();
            }

            @Override
            public double update(double deltaSec) {
                return isPressed.update(deltaSec);
            }
        });
        
    }

    private final HashSet<Updater> updaters = new HashSet<>();
    private final DigitalChannelImplFake underlyingChannel;

    /**
     * Creates a touch sensor which is pressed when the given supplier is true.
     * The values for `isPressed()` are obtained from the provided isPressed 
     * argument every time update() is called.
     * 
     * The controller is newly constructed.
     * 
     * @param isPressed Whether `touchSensor.isPressed()` returns true.
     */
    public DigitalTouchSensorFake(BooleanSupplier isPressed) {
        this(new EasyTimedStateGetter(() -> isPressed.getAsBoolean()));
    }

    /**
     * Creates a touch sensor which is pressed when the given supplier is true.
     * The values for `isPressed()` are obtained from the provided isPressed 
     * argument every time update() is called, which in-turn updates the provided
     * `isPressed` TimedStateGetter.
     * 
     * The controller is newly constructed.
     * 
     * @param isPressed Whether `touchSensor.isPressed()` returns true.
     */
    public DigitalTouchSensorFake(TimedStateGetter isPressed) {
        this(
            invertedGetterData(isPressed), 
            DigitalChannelControllerFake.createPossiblyWithNullModule(),
            0
        );
    }
    
    /**
     * Creates a touch sensor which is pressed when the given supplier is true.
     * The values for `isPressed()` are obtained from the provided isPressed 
     * argument every time update() is called.
     * 
     * This throws an IllegalArgument exception if no port is available
     * 
     * @param isPressed Whether `touchSensor.isPressed()` returns true.
     * @param controller What to connect the touch sensor to.
     */
    public DigitalTouchSensorFake(BooleanSupplier isPressed, DigitalChannelControllerFake controller) {
        this(new EasyTimedStateGetter(() -> isPressed.getAsBoolean()), controller);
    }
    
    /**
     * Creates a touch sensor which is pressed when the given supplier is true.
     * The values for `isPressed()` are obtained from the provided isPressed 
     * argument every time update() is called, which in-turn updates the provided
     * `isPressed` TimedStateGetter.
     * 
     * This throws an IllegalArgument exception if no port is available
     * 
     * @param isPressed Whether `touchSensor.isPressed()` returns true.
     * @param controller What to connect the touch sensor to.
     */
    public DigitalTouchSensorFake(TimedStateGetter isPressed, DigitalChannelControllerFake controller) {
        this(invertedGetterData(isPressed), controller);
    }
    
    /**
     * Creates a touch sensor which is pressed when the given supplier is true.
     * The values for `isPressed()` are obtained from the provided isPressed 
     * argument every time update() is called.
     * 
     * This throws an IllegalArgument exception if the given port is unavailable
     * on the controller, either if it is outside of range 0-7 (inclusive), or if 
     * the port is taken by another device.
     * 
     * @param isPressed Whether `touchSensor.isPressed()` returns true.
     * @param controller What to connect the touch sensor to.
     * @param port Where to connect the sensor on the controller. Between 0 and 7 inclusive
     */
    public DigitalTouchSensorFake(BooleanSupplier isPressed, DigitalChannelControllerFake controller, int port) {
        this(new EasyTimedStateGetter(() -> isPressed.getAsBoolean()), controller, port);
    }
    
    /**
     * Creates a touch sensor which is pressed when the given supplier is true.
     * The values for `isPressed()` are obtained from the provided isPressed 
     * argument every time update() is called, which in-turn updates the provided
     * `isPressed` TimedStateGetter.
     * 
     * This throws an IllegalArgument exception if the given port is unavailable
     * on the controller, either if it is outside of range 0-7 (inclusive), or if 
     * the port is taken by another device.
     * 
     * @param isPressed Whether `touchSensor.isPressed()` returns true.
     * @param controller What to connect the touch sensor to.
     * @param port Where to connect the sensor on the controller. Between 0 and 7 inclusive
     */
    public DigitalTouchSensorFake(TimedStateGetter isPressed, DigitalChannelControllerFake controller, int port) {
        this(invertedGetterData(isPressed), controller, port);
    }

    private DigitalTouchSensorFake(DigitalChannelData data, DigitalChannelControllerFake controller, int port) {
        underlyingChannel = new DigitalChannelImplFake(data, controller, port);
    }

    private DigitalTouchSensorFake(DigitalChannelData data, DigitalChannelControllerFake controller) {
        underlyingChannel = new DigitalChannelImplFake(data, controller);
    }

    public DigitalChannelImplFake asDigitalChannel() {
        return this.underlyingChannel;
    }

    public DigitalChannelControllerFake getControllerFake() {
        return asDigitalChannel().getController();
    }

    public int getPortNumber() {
        return asDigitalChannel().getPortNumber();
    }

    /**
     * Returns the logical opposite of the underlying controller's state. This
     * is based off of REV touch sensors, which pull LOW when they are pressed.
     * This also means that when bulk cached, the cached state will be the logical
     * opposite of the isPressed value (the output of isPressed will still be as 
     * expected).
     * 
     * @return True if the controller gets false at this sensor's port; false 
     * otherwise
     */
    @SimulateDelay(ON_BULK_READS)
    @Override
    public boolean isPressed() {
        // NOTE: underlyingChannel does not simulate delay, because it isn't registered with any updaters

        // Simulating delay only if this method was called by the user, not by any 
        // internal methods. This is done to prevent Updater.updateAll() from being 
        // called multiple times by LynxModuleUsbDeviceImplFake.readBulkDataPayload()
        if(getControllerFake().shouldReread()) {
            return !underlyingChannel.getState();    
        }

        // The method was called by a user rather than an internal method; 
        // simulate delay.
        final LynxGetSingleDIOInputCommand command = new LynxGetSingleDIOInputCommand(
            getControllerFake().getLynxModule(),
            this.getPortNumber()
        );

        ModularUpdater.updateAllOnceIfAnyCacheOutdated(
            updaters, 
            Updater.UpdateDelaySource.DIGITAL, 
            this, 
            command
        );
        
        /* 
         *  It makes the most sense to return true when the digital state is LOW (aka. false).
         *  > "Any touch sensor that connects its output to ground when pressed
         *  >  (known as "active low") can be configured as a 'REV Touch Sensor.' "
         *  (Source: SensorTouch.java in the samples folder of the official FtcRobotController project) 
         */
        underlyingChannel.setMode(DigitalChannelImplFake.Mode.INPUT);
        return !underlyingChannel.getState();
    }

    /**
     * 1.0 if being pressed; 0.0 otherwise.
     * 
     * @return 1.0 if being pressed; 0.0 otherwise.
     */
    @SimulateDelay(ON_BULK_READS)
    @Override
    public double getValue() {
        return isPressed() ? 1.0 : 0.0;
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
        return underlyingChannel.setUpdatingEnabled(newUpdatingEnabled);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return underlyingChannel.isUpdatingEnabled();
    }

    @Override
    public double update(double deltaSec) {
        return underlyingChannel.update(deltaSec);
    }

    @Override
    public String getDeviceName() {
        return "DigitalTouchSensorFake";
    }

    @Override
    public void close() {
        underlyingChannel.close();
    }

    @Override
    public void resetDeviceConfigurationForOpMode() {
        underlyingChannel.resetDeviceConfigurationForOpMode();
    }

    @Override
    public int getVersion() {
        return underlyingChannel.getVersion();
    }

    @Override
    public String getConnectionInfo() {
        return underlyingChannel.getController().getDeviceName() 
                + "; port " 
                + underlyingChannel.getPortNumber();
    }

    @Override
    public Manufacturer getManufacturer() {
        return null;
    }
    
}