/*
 * ServoImplExFake.java
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

import clarson.ftc.faker.updater.Rotateable;
import clarson.ftc.faker.updater.SimulateDelay;
import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.wrapper.PositionalServoData;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import java.util.HashSet;
import static clarson.ftc.faker.updater.Updater.UpdateDelaySource.SERVO;
import static clarson.ftc.faker.updater.UpdatesWhen.ALWAYS;
import static clarson.ftc.faker.updater.UpdatesWhen.CONDITIONAL;
import static clarson.ftc.faker.updater.UpdatesWhen.NEVER;

public class ServoImplExFake extends ServoImplEx implements Rotateable, TwoWayUpdateable {
    public final static ServoConfigurationType getFakeConfiguration(PositionalServoData data) {
        return new ServoConfigurationType();
    }

    /**
     * Finds the lowest valued, unnoccupied port on the controller. If none are
     * found, -1 is returned.
     * 
     * @return The lowest available port, or -1 if none exists.
     */
    private static int findAvailablePort(ServoControllerExFake controller) {
        for(int i = 0; i < controller.totalPorts(); i++) {
            controller.isPortAvailable(i);
        }

        // The method would've early returned if any was available
        return -1;
    }

    private HashSet<Updater> updaters = new HashSet<>();

    public ServoImplExFake(double rpm, double maxRevolutions) {
        this(rpm, maxRevolutions, 0, PwmRange.defaultRange);
    }

    public ServoImplExFake(double rpm, double maxRevolutions, double initialPosition) {
        this(rpm, maxRevolutions, initialPosition, PwmRange.defaultRange);
    }

    public ServoImplExFake(
        double rpm, 
        double maxRevolutions,
        double initialPosition, 
        PwmRange maxRange
    ) {
        this(
            new PositionalServoData(rpm, maxRevolutions, initialPosition, maxRange), 
            new ServoControllerExFake(), 
            0
        );
    }

    public ServoImplExFake(PositionalServoData data, ServoControllerExFake controller, int portNumber) {
        super(
            controller, 
            portNumber, 
            ServoImplExFake.Direction.FORWARD, 
            getFakeConfiguration(data)
        );

        if(!controller.connect(PositionalServoData.copyForServo(this, data))) {
            throw new IllegalArgumentException("Port number <" + portNumber + "> is not available on controller");
        }

        controller.setServoType(portNumber, getFakeConfiguration(data));
    }

    public ServoImplExFake(PositionalServoData data, ServoControllerExFake controller) {
        this(data, controller, findAvailablePort(controller));
    }

    public PositionalServoData getData() {
        return (PositionalServoData) (this
            .getControllerFake()
            .getData(this.getPortNumber()));
    }

    @Override
    public double setAngularVelOffset(double thetaPrime) {
        return getData().setAngularVelOffset(thetaPrime);
    }
    
    @Override
    public double addAngularVelOffset(double thetaPrime) {
        return getData().addAngularVelOffset(thetaPrime);
    }
    
    @Override
    public double update(double deltaSec) {
        return getData().update(deltaSec);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return this.getData().isUpdatingEnabled();
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        return this.getData().setUpdatingEnabled(newUpdatingEnabled);
    }

    @Override
    public void remember(Updater updater) {
        this.updaters.add(updater);
    }

    @Override 
    public void forget(Updater updater) {
        this.updaters.remove(updater);
    }

    public ServoControllerExFake getControllerFake() {
        return (ServoControllerExFake) super.getController();
    }

    // #############################################################################
    //   NOTE: The following section only is super methods with delay simulation
    //         Nothing below is more informative than its Javadoc
    // #############################################################################
    
    /**
     * Simulates delay only when the set position is different form the last known 
     * position
     * 
     * @param position
     */
    @Override 
    @SimulateDelay(CONDITIONAL)
    public void setPosition(double position) {
        final ServoControllerExFake controller = this.getControllerFake();
        final int port = this.getPortNumber();
        final double lastKnown = controller.lastKnownPosition(port);
        controller.enableDry(true); // Prevent the position fom actually being changed
        super.setPosition(position);
        controller.enableDry(false);
        final double newPos = controller.lastKnownPosition(port);

        if(newPos != lastKnown) {
            Updater.updateAllOnce(updaters, SERVO);
            super.setPosition(position);
        } 
    }
    
    @Override 
    @SimulateDelay(NEVER)
    public double getPosition() {
        return super.getPosition();
    }
    
    @Override 
    @SimulateDelay(ALWAYS)
    public void setPwmRange(PwmControl.PwmRange range) {
        Updater.updateAllOnce(updaters, SERVO);
        super.setPwmRange(range);
    }
    
    @Override 
    @SimulateDelay(ALWAYS)
    public void setPwmEnable() {
        Updater.updateAllOnce(updaters, SERVO);
        super.setPwmEnable();
    }
    
    @Override 
    @SimulateDelay(ALWAYS)
    public void setPwmDisable() {
        Updater.updateAllOnce(updaters, SERVO);
        super.setPwmDisable();
    }
    
    @Override 
    @SimulateDelay(ALWAYS)
    public boolean isPwmEnabled() {
        Updater.updateAllOnce(updaters, SERVO);
        return super.isPwmEnabled();
    }
}
