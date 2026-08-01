/*
 * DigitalChannelControllerFake.java
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

import clarson.ftc.faker.wrapper.DigitalChannelData;
import com.qualcomm.hardware.lynx.commands.core.LynxGetSingleDIOInputCommand;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.util.SerialNumber;

public class DigitalChannelControllerFake implements DigitalChannelController {
    /**
     * Constructs a new DigitalChannelControllerFake whose module is null if an error 
     * is thrown while trying to create a unique module for it.
     * 
     * @return A newly constructed controller.
     */
    public static DigitalChannelControllerFake createPossiblyWithNullModule() {
        try {
            return new DigitalChannelControllerFake();
        } catch(RobotCoreException | InterruptedException err) {
            return new DigitalChannelControllerFake(null);
        }
    }

    private DigitalChannelData[] channels = new DigitalChannelData[this.totalPorts()];
    private final SerialNumber serialNumber = SerialNumber.createEmbedded();
    private boolean shouldReread = false;
    private LynxModuleHardwareFake module = null;

    public DigitalChannelControllerFake() throws RobotCoreException, InterruptedException {
        this(LynxModuleHardwareFake.createUniqueModule());
    }

    public DigitalChannelControllerFake(LynxModuleHardwareFake module) {
        this.module = module;
    }

    public void setLynxModule(LynxModuleHardwareFake newModule) {
        this.module = newModule;
    }

    
    public int totalPorts() {
        return 8;
    }

    public LynxModuleHardwareFake getLynxModule() {
        return this.module;
    }

    /**
     * Determines whether a channel can be connected to the given port. A port can
     * be connected to if it is between 0-7 (inclusive) and the port is not 
     * already occupied.
     * 
     * @param portNumber The port to check.
     * @return Whether the port exists and is unnoccupied.
     */
    public boolean isPortAvailable(int portNumber) {
        return channels[portNumber] == null && portNumber >= 0 && portNumber <= this.totalPorts();
    }

    /**
     * Attempts to connect the channel to this controller. The connection can fail 
     * if the given port number is occupied. 
     * 
     * @param channelData Metadata for the channel to be connected. Does not need
     * to have any specific `actuator` field value, as long as such `actuator`
     * field is set to the desired channel later.
     * @return True if the the connection was succesfully completed. 
     */
    public boolean connect(DigitalChannelData channelData, int portNumber) {
        if(!isPortAvailable(portNumber)) {
            return false;
        }

        channels[portNumber] = channelData;
        return true;
    }

    public DigitalChannelImplFake getChannel(int port) {
        if(channels[port] == null) {
            throw new IllegalArgumentException("Attempted to access unconnected port <" + port + ">.");
        }

        return channels[port].channel;
    }

    public DigitalChannelData getData(int port) {
        if(channels[port] == null) {
            throw new IllegalArgumentException("Attempted to access unconnected port <" + port + ">.");
        }

        return channels[port];
    }

    public void setForceReread(boolean newShouldReread) {
        this.shouldReread = newShouldReread;
    }

    boolean shouldReread() {
        return this.shouldReread;
    }

    @Override
    public SerialNumber getSerialNumber() {
        return this.serialNumber;
    }
    
    @Override
    public DigitalChannel.Mode getDigitalChannelMode(int channel) {
        return getData(channel).mode;
    }
    
    @Override
    public void setDigitalChannelMode(int channel, DigitalChannel.Mode mode)  {
        getData(channel).mode = mode;
    }
    
    @Override
    @Deprecated
    public void setDigitalChannelMode(int channel, DigitalChannelController.Mode mode)  {
        setDigitalChannelMode(channel, mode.migrate());
    }
    
    @Override
    public boolean getDigitalChannelState(int channel) {
        if(!shouldReread && module.getBulkCachingMode() != LynxModule.BulkCachingMode.OFF) {
            final LynxModule.BulkData data = module.recordBulkCachingCommandIntent(
                new LynxGetSingleDIOInputCommand(module, channel),
                "" // Empty tag because this is not a LynxGetBulkInputDataCommand (the only command which reuires a tag)
            );

            return data.getDigitalChannelState(channel);
        }

        return getData(channel).getState();
    }
    
    @Override
    public void setDigitalChannelState(int channel, boolean state) {
        // According to the documentation, behavior is undefined when in INPUT
        // in such case, we simply do nothing
        if(getData(channel).mode == DigitalChannel.Mode.INPUT) {
            return;
        }

        // The mode is OUTPUT; set the state
        getData(channel).lastState = state;
    }

    @Override
    public void close() {
        // Allow gargbage-collection of the contained channels
        this.channels = null;
    } 
    
    private String safeGetDeviceName(DigitalChannel channel) {
        return channel == null ? "null" : channel.getDeviceName();
    }

    @Override
    public String getConnectionInfo() {
        String result = "DigitalChannelControllerFake Connections:";
        for(int i = 0; i < this.totalPorts(); i++) {
            result += String.format("\n    [%d]: %s", i, safeGetDeviceName(channels[i].channel));
        }
        return result;
    } 
    
    @Override
    public String getDeviceName() {
        return "DigitalChannelControllerFake";
    } 
    
    @Override
    public Manufacturer getManufacturer() {
        return null;
    } 
    
    @Override
    public int getVersion() {
        return -1;
    } 
    
    @Override
    public void resetDeviceConfigurationForOpMode() {
        this.channels = new DigitalChannelData[this.totalPorts()];
    }
}