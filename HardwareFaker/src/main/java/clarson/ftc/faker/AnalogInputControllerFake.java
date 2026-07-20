/*
 * AnalogInputControllerFake.java
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

import clarson.ftc.faker.LynxModuleHardwareFake;
import com.qualcomm.hardware.lynx.commands.core.LynxGetADCCommand;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.AnalogInputController;
import com.qualcomm.robotcore.util.SerialNumber;

public class AnalogInputControllerFake implements AnalogInputController {
    /**
     * Constructs a new AnalogInputControllerFake whose module is null if an error 
     * is thrown while trying to create a unique module for it.
     * 
     * @return A newly constructed controller.
     */
    public static AnalogInputControllerFake createPossiblyWithNullModule() {
        try {
            return new AnalogInputControllerFake();
        } catch(RobotCoreException | InterruptedException err) {
            return new AnalogInputControllerFake(null);
        }
    }

    
    private final SerialNumber serialNumber = SerialNumber.createEmbedded();
    private AnalogInputFake[] analogs = new AnalogInputFake[this.totalPorts()];
    protected LynxModuleHardwareFake module;
    protected boolean shouldReread = false;

    public AnalogInputControllerFake() throws RobotCoreException, InterruptedException {
        this(LynxModuleHardwareFake.createUniqueModule());
    }

    public AnalogInputControllerFake(LynxModuleHardwareFake module) {
        this.module = module;
    }
    
    /**
     * Determines whether a analog can be connected to the given port. A port can
     * be connected to if it is between 0-3 (inclusive) and the port is not 
     * already occupied.
     * 
     * @param portNumber The port to check.
     * @return Whether the port exists and is unnoccupied.
     */
    public boolean isPortAvailable(int portNumber) {
        return analogs[portNumber] == null && portNumber >= 0 && portNumber <= this.totalPorts();
    }

    /**
     * Attempts to connect the analog to this controller. The connection can fail 
     * if the given port number is occupied. 
     * 
     * @param analog The analog input to connect to the controller
     * @return True if the the connection was succesfully completed. 
     */
    public boolean connect(AnalogInputFake analog, int portNumber) {
        if(!isPortAvailable(portNumber)) {
            return false;
        }

        analogs[portNumber] = analog;
        return true;
    }

    public int totalPorts() {
        return 4;
    }

    public void setLynxModule(LynxModuleHardwareFake newModule) {
        this.module = newModule;
    }

    public LynxModuleHardwareFake getLynxModule() {
        return this.module;
    }

    /**
     * Get the value of this analog input
     *
     * Return the current ADC results from the A0-A7 analog input pins.
     * 
     * @param analog which analog analog to read
     * @return the current voltage in volts
     */
    @Override
    public double getAnalogInputVoltage(int analog) {
        if(!shouldReread && module.getBulkCachingMode() != LynxModule.BulkCachingMode.OFF) {
            final LynxModule.BulkData data = module.recordBulkCachingCommandIntent(
                new LynxGetADCCommand(
                    module,
                    LynxGetADCCommand.Channel.user(analog),
                    LynxGetADCCommand.Mode.ENGINEERING
                ),
                ""
            );
        }

        return analogs[analog].getLastVoltage();
    }

    /**
     * Returns the maximum value that getAnalogInputVoltage() is capable of reading
     * 
     * @return the maximum value that getAnalogInputVoltage() is capable of reading,
     *         in volts.
     * @see #getAnalogInputVoltage(int)
     */
    @Override
    public double getMaxAnalogInputVoltage() {
        return 5.0; // TODO: Determine whether this actually accurate
    }

    /**
     * Dictates whether bulk cache-accesssing methods are forced instead to 
     * re-read their data. This can be used to get the data of a method like
     * `getVoltage()` for a bulk cache without causing infinite 
     * recursion (because `getVoltage()` sends a command for a new 
     * cache, which requires calling `getVoltage()` to get its data, and 
     * so on).
     * 
     * This **always** takes precedence over the bulk caching mode. Setting 
     * bulk caching to AUTO does (nearly) nothing if this has been set to true 
     * 
     * @param shouldReread True if bulk caching should be ignored. False for 
     * normal operation.
     * @return Whether any change was made. False if was already set to the 
     * given `shouldReread` value
     */
    public boolean setForceReread(boolean shouldReread) {
        final boolean oldValue = this.shouldReread;
        this.shouldReread = shouldReread;
        return oldValue != shouldReread;
    }

    boolean shouldReread() {
        return this.shouldReread;
    }

    /**
     * Serial Number
     *
     * @return return the USB serial number of this device
     */
    @Override
    public SerialNumber getSerialNumber() {
        return serialNumber;
    }

    @Override
    public void close() {
        module = null; // Allow it to be garbage-collected
        analogs = null; // Allow it to be garbage-collected
    }

    @Override
    public void resetDeviceConfigurationForOpMode() {
        analogs = new AnalogInputFake[this.totalPorts()];
    }

    @Override
    public int getVersion() {
        // TODO: Convert this value to decimal
        return 0x190F1B44;
    }

    @Override
    public String getConnectionInfo() {
        return String.format(
              "AnalogInputControllerFake Connections:" +
            "\n    [0]: %s" +  
            "\n    [1]: %s" +  
            "\n    [2]: %s" +  
            "\n    [3]: %s" +  
            "\n    [4]: %s" +  
            "\n    [5]: %s",
            safeGetDeviceName(analogs[0]),
            safeGetDeviceName(analogs[1]),
            safeGetDeviceName(analogs[2]),
            safeGetDeviceName(analogs[3]),
            safeGetDeviceName(analogs[4]),
            safeGetDeviceName(analogs[5])
        );
    }

    private String safeGetDeviceName(AnalogInputFake analog) {
        return analog == null ? "null" : analog.getDeviceName();
    }

    @Override
    public String getDeviceName() {
        return "AnalogInputControllerFake";
    }

    @Override
    public Manufacturer getManufacturer() {
        return null;
    }
}