package clarson.ftc.faker;

import clarson.ftc.faker.wrapper.DigitalChannelData;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.commands.core.LynxGetBulkInputDataCommand;
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

    private DigitalChannelData[] channels = new DigitalChannelData[8];
    private SerialNumber serialNumber = SerialNumber.createEmbedded();
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
        return channels[portNumber] != null && portNumber >= 0 && portNumber <= 7;
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

    @Override
    public SerialNumber getSerialNumber() {
        return this.serialNumber;
    }
    
    @Override
    public DigitalChannel.Mode getDigitalChannelMode(int channel) {
        // TODO: wrapper mode get
        return null;
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
                new LynxGetBulkInputDataCommand(module),
                "digitalState" + channel
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
        
        return String.format(
              "DigitalChannelControllerFake Connections:" +
            "\n    [1]: %s" +  
            "\n    [2]: %s" +  
            "\n    [3]: %s" +  
            "\n    [4]: %s" +  
            "\n    [5]: %s" +  
            "\n    [6]: %s" +  
            "\n    [7]: %s" +  
            "\n    [8]: %s",
            safeGetDeviceName(getChannel(0)),
            safeGetDeviceName(getChannel(1)),
            safeGetDeviceName(getChannel(2)),
            safeGetDeviceName(getChannel(3)),
            safeGetDeviceName(getChannel(4)),
            safeGetDeviceName(getChannel(5)),
            safeGetDeviceName(getChannel(6)),
            safeGetDeviceName(getChannel(7))
        );
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
        this.channels = new DigitalChannelData[8];
    }
}