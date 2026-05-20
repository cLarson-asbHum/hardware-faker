package clarson.ftc.faker;

import java.util.HashSet;

import com.qualcomm.robotcore.hardware.LED;

import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.wrapper.DigitalChannelData;
import clarson.ftc.faker.DigitalChannelImplFake;

import java.util.HashSet;
import java.util.Set;

public class LEDFake extends LED implements TwoWayUpdateable {
    public static final boolean DEFAULT_INITIALLY_ON = false;

    private static DigitalChannelImplFake lastChannel = null;
    private final DigitalChannelImplFake channel;
    
    public LEDFake() {
        this(new DigitalChannelImplFake());
    }

    public LEDFake(DigitalChannelControllerFake controller) {
        this(new DigitalChannelImplFake(new DigitalChannelData(DEFAULT_INITIALLY_ON), controller));
    }

    public LEDFake(DigitalChannelControllerFake controller, int port) {
        this(controller, port, DEFAULT_INITIALLY_ON);
    }
    
    public LEDFake(DigitalChannelControllerFake controller, int port, boolean isInitiallyOn) {
        this(new DigitalChannelImplFake(
            new DigitalChannelData(isInitiallyOn), 
            controller, 
            port
        ));
    }

    private LEDFake(DigitalChannelImplFake newChannel) {
        super(newChannel.getController(), newChannel.getPortNumber());
        this.channel = newChannel;
    }

    @Override
    public void remember(Updater updater) {
        channel.remember(updater);
    }

    @Override
    public void forget(Updater updater) {
        channel.forget(updater);
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        return channel.setUpdatingEnabled(newUpdatingEnabled);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return channel.isUpdatingEnabled();
    }

    @Override
    public double update(double deltaSec) {
        return channel.update(deltaSec);
    }
}