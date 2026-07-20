/*
 * CRServoImplExFake.java
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
import clarson.ftc.faker.wrapper.ContinuousServoData;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import com.qualcomm.robotcore.hardware.CRServoImplEx;
import com.qualcomm.robotcore.hardware.PwmControl;
import java.util.HashSet;
import java.util.Set;
import static clarson.ftc.faker.updater.Updater.UpdateDelaySource.SERVO;
import static clarson.ftc.faker.updater.UpdatesWhen.ALWAYS;
import static clarson.ftc.faker.updater.UpdatesWhen.CONDITIONAL;
import static clarson.ftc.faker.updater.UpdatesWhen.NEVER;

public class CRServoImplExFake extends CRServoImplEx implements Rotateable, TwoWayUpdateable {
    public final static ServoConfigurationType getFakeConfiguration(ContinuousServoData data) {
        return new ServoConfigurationType();
    }

    /**
     * Finds the lowest valued, unnoccupied port on the controller. If none are
     * found, -1 is returned.
     * 
     * @return The lowest avaiable port, or -1 if none exists.
     */
    private static int findAvaiablePort(ServoControllerExFake controller) {
        for(int i = 0; i < 4; i++) {
            controller.isPortAvailable(i);
        }

        // The method would've early returned if any was avaiable
        return -1;
    }

    private Set<Updater> updaters = new HashSet<>();
    
    public CRServoImplExFake(double rpm) {
        this(rpm, 0, PwmRange.defaultRange);
    }

    public CRServoImplExFake(double rpm, double initialPosition) {
        this(rpm, initialPosition, PwmRange.defaultRange);
    }

    public CRServoImplExFake(double rpm, double initialPosition, PwmRange maxRange) {
        this(
            new ContinuousServoData(rpm, initialPosition, maxRange), 
            new ServoControllerExFake(), 
            0
        );
    }

    public CRServoImplExFake(ContinuousServoData data, ServoControllerExFake controller, int portNumber) {
        super(
            controller, 
            portNumber, 
            CRServoImplExFake.Direction.FORWARD, 
            getFakeConfiguration(data)
        );

        if(!controller.connect(ContinuousServoData.copyForServo(this, data))) {
            throw new IllegalArgumentException("Port number <" + portNumber + "> is not avaiable on controller");
        }

        controller.setServoType(portNumber, getFakeConfiguration(data));
    }

    public CRServoImplExFake(ContinuousServoData data, ServoControllerExFake controller) {
        this(data, controller, findAvaiablePort(controller));
    }

    @Override
    public double update(double deltaSec) {       
        return this.getData().update(deltaSec);
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        return this.getData().setUpdatingEnabled(newUpdatingEnabled);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return this.getData().isUpdatingEnabled();
    }

    @Override
    public double addAngularVelOffset(double thetaPrime) {
        return this.getData().addAngularVelOffset(thetaPrime);
    }
    
    @Override
    public double setAngularVelOffset(double thetaPrime) {
        return this.getData().setAngularVelOffset(thetaPrime);
    }

    public ContinuousServoData getData() {
        final ServoControllerExFake controller = (ServoControllerExFake) this.getController();
        return (ContinuousServoData) controller.getData(this.getPortNumber());
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
     * Updates only when the power is different from the last known power
     * @param power
     */
    @Override 
    @SimulateDelay(CONDITIONAL)
    public void setPower(double power) {
        final ServoControllerExFake controller = this.getControllerFake();
        final int port = this.getPortNumber();
        final double lastKnown = controller.lastKnownPosition(port);
        controller.enableDry(true); // Prevent the power fom actually being changed
        super.setPower(power);
        controller.enableDry(false);
        final double newPos = controller.lastKnownPosition(port);

        if(newPos != lastKnown) {
            Updater.updateAllOnce(updaters, SERVO);
            super.setPower(power);
        } 
    }
    
    @Override 
    @SimulateDelay(NEVER)
    public double getPower() {
        return super.getPower();
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