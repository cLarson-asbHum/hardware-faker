package clarson.ftc.faker;

import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.TimedStateGetter;
import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.SimulateDelay;
import clarson.ftc.faker.wrapper.DigitalChannelData;

import static clarson.ftc.faker.updater.UpdatesWhen.ALWAYS;
import static clarson.ftc.faker.updater.UpdatesWhen.CONDITIONAL;
import static clarson.ftc.faker.updater.UpdatesWhen.ON_BULK_READS;

import com.qualcomm.hardware.lynx.commands.core.LynxGetSingleDIOInputCommand;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.DigitalChannelImpl;

import java.util.HashSet;
import java.util.function.BooleanSupplier;

public class DigitalChannelImplFake extends DigitalChannelImpl implements TwoWayUpdateable {
    private static DigitalChannelControllerFake lastController = null;    
    
    /**
     * Finds the lowest valued, unnoccupied port on the controller. If none are
     * found, -1 is returned.
     * 
     * @return The lowest avaiable port, or -1 if none exists.
     */
    private static int findAvaiablePort(DigitalChannelControllerFake controller) {
        for(int i = 0; i < 4; i++) {
            if(controller.isPortAvailable(i)) {
                return i;
            }
        }

        // The method would've early returned if any was avaiable
        return -1;
    }

    private final HashSet<Updater> updaters = new HashSet<>();
    private final DigitalChannelControllerFake controller;
    private final int port;

    public DigitalChannelImplFake() {
        this(false);
    }

    public DigitalChannelImplFake(boolean initialState) {
        super(lastController = DigitalChannelControllerFake.createPossiblyWithNullModule(), 0);

        final DigitalChannelData data = new DigitalChannelData(this, initialState);
        lastController.connect(data, 0);
        this.controller = lastController;
        this.port = 0;
    }

    public DigitalChannelImplFake(BooleanSupplier stateGetter) {
        this(new TimedStateGetter() {
            private boolean isUpdatingEnabled = true;

            @Override
            public boolean getAsBoolean() {
                return stateGetter.getAsBoolean();
            }

            @Override
            public double update(double unused) {
                // Do nothing
                return 0;
            }

            public boolean isUpdatingEnabled() {
                return isUpdatingEnabled;
            }

            @Override
            public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
                final boolean oldValue = this.isUpdatingEnabled;
                this.isUpdatingEnabled = newUpdatingEnabled;
                return oldValue != newUpdatingEnabled;
            }
        });
    }
    
    public DigitalChannelImplFake(TimedStateGetter stateGetter) {
        super(lastController = DigitalChannelControllerFake.createPossiblyWithNullModule(), 0);
        
        final DigitalChannelData data = new DigitalChannelData(this, stateGetter);
        lastController.connect(data, 0);
        this.controller = lastController;
        this.port = 0;
    }

    public DigitalChannelImplFake(DigitalChannelData data, DigitalChannelControllerFake controller, int port) {
        super(controller, port);
        
        // Connecting a new data wrapper
        final DigitalChannelData associatedData = DigitalChannelData.copyForChannelData(this, data);
        if(!controller.connect(associatedData, port)) {
            throw new IllegalArgumentException("Port number <" + port + "> is not available on controller");
        }

        this.controller = controller;
        this.port = port;
    }

    public DigitalChannelImplFake(DigitalChannelData data, DigitalChannelControllerFake controller) {
        this(data, controller, findAvaiablePort(controller));
    }

    public DigitalChannelControllerFake getController() {
        return this.controller;
    }

    public int getPortNumber() {
        return this.port;
    }

    public DigitalChannelData getData() {
        return this.controller.getData(this.port);
    }

    @Override
    public String getDeviceName() {
        return "DigitalChannelImplFake";
    }

    @Override
    public double update(double deltaSec)  {
        return getData().update(deltaSec);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return getData().isUpdatingEnabled();
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdating) {
        return getData().setUpdatingEnabled(newUpdating);
    }

    @Override
    public void remember(Updater updater) {
        updaters.add(updater);
    }

    @Override
    public void forget(Updater updater) {
        updaters.remove(updater);
    }
    
    // #############################################################################
    //   NOTE: The following section only is super methods with delay simulation
    //         Nothing below is more informative than its Javadoc
    // #############################################################################

    @SimulateDelay(ON_BULK_READS)
    @Override
    public boolean getState() {
        final LynxGetSingleDIOInputCommand command = new LynxGetSingleDIOInputCommand(
            controller.getLynxModule(),
            this.getPortNumber()
        );
        ModularUpdater.updateAllOnceIfAnyCacheOutdated(
            updaters, 
            Updater.UpdateDelaySource.DIGITAL, 
            this, 
            command
        );
        return super.getState();
    }

    @SimulateDelay(ALWAYS)
    @Override
    public void setState(boolean newState) {
        Updater.updateAllOnce(updaters, Updater.UpdateDelaySource.DIGITAL);
        super.setState(newState);
    }

    @SimulateDelay(ALWAYS)
    @Override
    public DigitalChannel.Mode getMode() {
        Updater.updateAllOnce(updaters, Updater.UpdateDelaySource.DIGITAL);
        return super.getMode();
    }

    @SimulateDelay(ALWAYS)
    @Override
    public void setMode(DigitalChannel.Mode mode) {
        Updater.updateAllOnce(updaters, Updater.UpdateDelaySource.DIGITAL);
        super.setMode(mode);
    }

    @SimulateDelay(ALWAYS)
    @Deprecated
    @Override
    public void setMode(DigitalChannelController.Mode mode) {
        Updater.updateAllOnce(updaters, Updater.UpdateDelaySource.DIGITAL);
        super.setMode(mode);
    }
}