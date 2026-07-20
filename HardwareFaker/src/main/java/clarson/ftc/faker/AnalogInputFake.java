/*
 * AnalogInputFake.java
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

import clarson.ftc.faker.function.TimedVoltageGetter;
import clarson.ftc.faker.function.VoltageGetter;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.SimulateDelay;
import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.EasyTimedVoltageGetter;

import com.qualcomm.hardware.lynx.commands.core.LynxGetADCCommand;
import com.qualcomm.robotcore.hardware.AnalogInput;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

import static clarson.ftc.faker.updater.Updater.UpdateDelaySource.ANALOG;
import static clarson.ftc.faker.updater.UpdatesWhen.NEVER;
import static clarson.ftc.faker.updater.UpdatesWhen.ON_BULK_READS;

import java.util.HashSet;
import java.util.function.DoubleSupplier;

public class AnalogInputFake extends AnalogInput implements TwoWayUpdateable {
    public static final VoltageUnit DEFAULT_UNITS = VoltageUnit.VOLTS;

    private static AnalogInputControllerFake lastController = null;    
    
    /**
     * Finds the lowest valued, unnoccupied port on the controller. If none are
     * found, -1 is returned.
     * 
     * @return The lowest avaiable port, or -1 if none exists.
     */
    private static int findAvaiablePort(AnalogInputControllerFake controller) {
        for(int i = 0; i < controller.totalPorts(); i++) {
            if(controller.isPortAvailable(i)) {
                return i;
            }
        }

        // The method would've early returned if any was avaiable
        return -1;
    }

    private final HashSet<Updater> updaters = new HashSet<>();
    private final AnalogInputControllerFake controller;
    private final int port;
    private final TimedVoltageGetter underlyingGetter;
    private double lastVoltage = 0;
   
    public AnalogInputFake(DoubleSupplier voltageGetter) {
        this(new EasyTimedVoltageGetter(voltageGetter));
    }

    public AnalogInputFake(VoltageGetter voltageGetter) {
        super(lastController = AnalogInputControllerFake.createPossiblyWithNullModule(), 0);
        lastController.connect(this, 0);
        this.controller = lastController;
        this.port = 0;
        this.underlyingGetter = new EasyTimedVoltageGetter(voltageGetter);
    }
    
    public AnalogInputFake(TimedVoltageGetter voltageGetter) {
        super(lastController = AnalogInputControllerFake.createPossiblyWithNullModule(), 0);
        lastController.connect(this, 0);
        this.controller = lastController;
        this.port = 0;
        this.underlyingGetter = voltageGetter;
    }

    public AnalogInputFake(TimedVoltageGetter voltageGetter, AnalogInputControllerFake controller, int port) {
        super(controller, port);
        
        // Connecting a new data wrapper
        if(!controller.connect(this, port)) {
            throw new IllegalArgumentException("Port number <" + port + "> is not available on controller");
        }

        this.controller = controller;
        this.port = port;
        this.underlyingGetter = voltageGetter;
    }

    public AnalogInputFake(TimedVoltageGetter voltageGetter, AnalogInputControllerFake controller) {
        this(voltageGetter, controller, findAvaiablePort(controller));
    }
    
    public AnalogInputControllerFake getControllerFake() {
        return this.controller;
    }

    @Override
    public String getDeviceName() {
        return "AnalogInputFake";
    }

    @Override
    public double update(double deltaSec)  {
        final double result = this.underlyingGetter.update(deltaSec);
        this.lastVoltage = this.underlyingGetter.getVoltage(DEFAULT_UNITS);
        return result;
    }

    @Override
    public boolean isUpdatingEnabled() {
        return this.underlyingGetter.isUpdatingEnabled();
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdating) {
        return this.underlyingGetter.setUpdatingEnabled(newUpdating);
    }

    @Override
    public void remember(Updater updater) {
        updaters.add(updater);
    }

    @Override
    public void forget(Updater updater) {
        updaters.remove(updater);
    }

    public double getLastVoltage() {
        return this.lastVoltage;
    }
    
    @SimulateDelay(ON_BULK_READS)
    @Override
    public double getVoltage() {
        // Simulating delay only if this method was called by the user, not by any 
        // internal methods. This is done to prevent Updater.updateAll() from being 
        // called multiple times by LynxModuleUsbDeviceImplFake.readBulkDataPayload()
        if(this.getControllerFake().shouldReread()) {
            return super.getVoltage();
        }

        // The method was called by a user rather than an internal method; 
        // simulate delay.
        final LynxGetADCCommand command = new LynxGetADCCommand(
            this.getControllerFake().getLynxModule(),
            LynxGetADCCommand.Channel.user(this.port),
            LynxGetADCCommand.Mode.ENGINEERING
        );

        ModularUpdater.updateAllOnceIfAnyCacheOutdated(updaters, ANALOG, this, command);
        return super.getVoltage();
    }

    @SimulateDelay(NEVER)
    @Override
    public double getMaxVoltage() {
        return super.getMaxVoltage();
    }
}