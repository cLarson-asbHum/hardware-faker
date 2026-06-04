/*
 * DigitalChannelImplFake.java
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

import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.TimedStateGetter;
import clarson.ftc.faker.util.EasyTimedStateGetter;
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
        for(int i = 0; i < 8; i++) {
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

    /**
     * Constructs a new DigitalCahnnel fake in the OUTPUT mode, with an initial 
     * state of false (off).
     */
    public DigitalChannelImplFake() {
        this(false);
    }

    /**
     * Constructs a new DigitalChannel fake in the OUTPUT mode, with the given 
     * initial state.
     * 
     * @param initialState Whether the channel is in the on state (true), or off
     * (false) 
     */
    public DigitalChannelImplFake(boolean initialState) {
        super(lastController = DigitalChannelControllerFake.createPossiblyWithNullModule(), 0);

        final DigitalChannelData data = new DigitalChannelData(this, initialState);
        lastController.connect(data, 0);
        this.controller = lastController;
        this.port = 0;
    }

    /**
     * Creates a new DigitalChannel fake in the INPUT mode. The values for
     * `getState()` are obtained from the provided stateGetter argument 
     * everytime update() is called.
     * 
     * A channel constructed from this constructor must be updated for 
     * new values to be obtained from getState(). This can occur automatically 
     * if the channel is connected to an updater, but this may not be the case 
     * if the channel was constructed manually (i.e., not obtained from a 
     * HardwareMap). 
     * 
     * @param stateGetter How the values for `getState()` are obtained
     */
    public DigitalChannelImplFake(BooleanSupplier stateGetter) {
        this(new EasyTimedStateGetter(stateGetter));
    }

    /**
     * Creates a new DigitalChannel fake in the INPUT mode. The values for
     * `getState()` are obtained from the provided stateGetter argument.
     * 
     * NOTE: EasyTimedStateGetters only have different values for `getState()`
     * if they are updated through their `update()` method, which is called 
     * internally by DigitalChannelImplFake's update() method. This is normally 
     * done automatically if the digital channel is connected to 
     * 
     * @param stateGetter How the values for `getState()` are obtained
     */
    public DigitalChannelImplFake(TimedStateGetter stateGetter) {
        super(lastController = DigitalChannelControllerFake.createPossiblyWithNullModule(), 0);
        
        final DigitalChannelData data = new DigitalChannelData(this, stateGetter);
        lastController.connect(data, 0);
        this.controller = lastController;
        this.port = 0;
    }

    /**
     * Creates a new DigitalChannel fake with initial conditions specified by 
     * the given data wrapper. The channel is then attempted to be connected to 
     * the given port on the controller. If the port cannot be connected to 
     * (either because it is occupied or nonexistent), this constructor will 
     * throw an IllegalArumentException.
     * 
     * The specific data wrapper is cloned, not reused, in the channel fake. 
     * In other words, modifications to the provided argument will not affect
     * the digital channel's internal data wrapper (which can be accessed with 
     * the getData() method), and vice versa. The `channel` property of the 
     * data wrapper will not be respected, for obvious reasons.
     * 
     * 
     * @param data The initial conditions for the DigitalChannel. Is cloned, not 
     * reused (see above).
     * @param controller The controller which the DigitalChannel resides on
     * @param port The port number, 0-7 inclusive, which the channel is plugged 
     * into on the controller
     */
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

    /**
     * Creates a new DigitalChannel fake with initial conditions specified by 
     * the given data wrapper. The channel is then attempted to be connected to 
     * the controller. If the port cannot be connected to (becuase no ports are 
     * available), this constructor will throw an IllegalArumentException.
     * 
     * The specific data wrapper is cloned, not reused, in the channel fake. 
     * In other words, modifications to the provided argument will not affect
     * the digital channel's internal data wrapper (which can be accessed with 
     * the getData() method), and vice versa. The `channel` property of the 
     * data wrapper will not be respected, for obvious reasons.
     * 
     * 
     * @param data The initial conditions for the DigitalChannel. Is cloned, not 
     * reused (see above).
     * @param controller The controller which the DigitalChannel resides on
     */
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
        // Simulating delay only if this method was called by the user, not by any 
        // internal methods. This is done to prevent Updater.updateAll() from being 
        // called multiple times by LynxModuleUsbDeviceImplFake.readBulkDataPayload()
        // FIXME: Every Bulk-read hardware needs this condition!! Add a corresponding test for each one
        if(controller.shouldReread()) {
            return super.getState();    
        }

        // The method was called by a user rather than an internal method; 
        // simulate delay.
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