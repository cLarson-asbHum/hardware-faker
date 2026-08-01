/*
 * ServoControllerExFake.java
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

import androidx.annotation.NonNull;
import clarson.ftc.faker.wrapper.ContinuousServoData;
import clarson.ftc.faker.wrapper.PositionalServoData;
import clarson.ftc.faker.wrapper.ServoData;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import com.qualcomm.robotcore.hardware.CRServoImplEx;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.ServoControllerEx;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import com.qualcomm.robotcore.util.Range;
import java.util.Arrays;
import static com.qualcomm.robotcore.hardware.PwmControl.PwmRange;

public class ServoControllerExFake implements ServoControllerEx {
    protected ServoData[] servos = new ServoData[this.totalPorts()];
    protected double[] lastKnown = clearedLastKnowns();
    private boolean enableDry = false;

    private static double[] clearedLastKnowns() {
        final double[] cleared = { Double.NaN, Double.NaN, Double.NaN, 
                Double.NaN, Double.NaN, Double.NaN };
            return cleared;
    }
    
    /**
     * Constructs a new ServoControllerExFake with the given servos instantly
     * connected. 
     * 
     * @param servoData The servos and their data to be connected. The actuator
     * field must point to a non-null servo with a valid `getPortNumber()` 
     * return value.
     */
    public ServoControllerExFake(ServoData... servoData) {
        for(final ServoData servoDatum : servoData) {
            this.connect(servoDatum);
        }
    }

    public int totalPorts() {
        return 6;
    }

    /**
     * Determines whether a servo can be connected to the given port. A port can
     * be connected to if it is between 0-5 (inclusive) and the port is not 
     * already occupied.
     * 
     * @param portNumber The port to check.
     * @return Whether the port exists and is unnoccupied.
     */
    public boolean isPortAvailable(int portNumber) {
        return  servos[portNumber] == null && portNumber >= 0 && portNumber <= this.totalPorts();
    }

    /**
     * Attempts to connect the servo to this controller. The connection can fail 
     * if the servo's port (from `getPort()`) is already occupied. 
     * 
     * @param servoData Metadata for the servo to be connected. The `getPort()` 
     * method should return a value in the range 0-5 (inclusive), or the 
     * connection will fail. 
     * @return True if the the connection was succesfully completed. 
     */
    public boolean connect(ServoData servoData) {
        int portNumber = -1;
        final HardwareDevice actuator = servoData.actuator;

        if(actuator instanceof CRServoImplEx) {
            portNumber = ((CRServoImplEx) actuator).getPortNumber();
        } else if(actuator instanceof ServoImplEx) {
            portNumber = ((ServoImplEx) actuator).getPortNumber();
        } else {
            throw new ClassCastException("servoData.actuator must be instance of CRServoImplEx or ServoImplEx");
        }

        if(!isPortAvailable(portNumber)) {
            return false;
        }

        servos[portNumber] = servoData;
        return true;
    }
    
    /**
     * Attempts to connect the servo to this controller. The connection can fail 
     * if the given port number is occupied. 
     * 
     * @param servoData Metadata for the servo to be connected. Does not need
     * to have any specific `actuator` field value, as long as such `actuator`
     * field is set to the desired servo later.
     * @return True if the the connection was succesfully completed. 
     */
    public boolean connect(ServoData servoData, int portNumber) {
        if(!isPortAvailable(portNumber)) {
            return false;
        }

        servos[portNumber] = servoData;
        return true;
    }

    /**
     * Gets the servo data at the given port number. If the port is not occupied 
     * or does not exists, the method throws an IllegalArgumentException.
     * 
     * @param port The port number at which the servo was connected.
     * @return The servo data at the given port.
     */
    public ServoData getData(int port) {
        if(servos[port] == null) {
            throw new IllegalArgumentException("Attempted to access unconnected port <" + port + ">.");
        }

        return servos[port];
    }
    
    /**
     * Gets the servo at the given port number. If the port is not occupied or 
     * does not exists, the method throws an IllegalArgumentException. If the 
     * servo at the given port exists but is not a continuous servo, a 
     * ClassCastException will be thrown.
     * 
     * @param port The port number at which the servo was connected.
     * @return The continuous servo at the given port.
     */
    public CRServoImplEx getContinuousServo(int port) {
        if(servos[port] == null) {
            throw new IllegalArgumentException("Attempted to access unconnected port <" + port + ">.");
        }

        final ServoData datum = getData(port);
        if(!datum.isPositional() || !(datum instanceof ContinuousServoData)) {
            throw new ClassCastException("Cannot cast non-positional servo to CRServoImplEx");
        }

        return ((ContinuousServoData) datum).actuator;
    }
    
    /**
     * Gets the servo at the given port number. If the port is not occupied or 
     * does not exists, the method throws an IllegalArgumentException. If the 
     * servo at the given port exists but is not a positional servo, a 
     * ClassCastException will be thrown.
     * 
     * @param port The port number at which the servo was connected.
     * @return The positional servo at the given port.
     */
    public ServoImplEx getPositionalServo(int port) {
        if(servos[port] == null) {
            throw new IllegalArgumentException("Attempted to access unconnected port <" + port + ">.");
        }

        final ServoData datum = getData(port);
        if(!datum.isPositional() || !(datum instanceof PositionalServoData)) {
            throw new ClassCastException("Cannot cast non-positional servo to ServoImplEx");
        }

        return ((PositionalServoData) datum).actuator;
    }

    @Override
    public void pwmEnable() {
        Arrays.spliterator(servos).forEachRemaining(servo -> {
            servo.isEnabled = true;
        });
    }

    @Override
    public void pwmDisable() {
        Arrays.spliterator(servos).forEachRemaining(servo -> {
            servo.isEnabled = false;
        });
    }

    @Override
    public PwmStatus getPwmStatus() {
        if(servos.length == 0) {
            return PwmStatus.MIXED;
        }

        // Checking if any of the `isEnabled`s are different from the first.
        boolean firstIsEnabled = servos[0].isEnabled;
        for(int i = 1; i < servos.length; i++) {
            if(servos[i].isEnabled != firstIsEnabled) {
                return PwmStatus.MIXED;
            }
        }

        // All the `isEnableds` are the same. Returning whether its enabled or not
        if(firstIsEnabled) {
            return PwmStatus.ENABLED;
        } else {
            return PwmStatus.DISABLED;
        }
    }

    @Override
    public void setServoPosition(int portNumber, double position) {
        if(!getData(portNumber).isEnabled) {
            return;
        }

        lastKnown[portNumber] = position;
        if(enableDry) {
            return;
        }

        if(getData(portNumber).isContinuous()) {
            final ContinuousServoData servo = (ContinuousServoData) getData(portNumber);
            servo.power = Range.clip(2 * pwmPowerFromPosition(servo, position), -1.0, 1.0);
            servo.unaffectedVelocity = servo.power * servo.maxRevsPerSec;
            servo.isTargetSet = true;
        } else if(getData(portNumber).isPositional()) {
            final PositionalServoData servo = (PositionalServoData) getData(portNumber);
            servo.targetPosition = 
                servo.maxPosition 
                * Range.clip(0.5 + pwmPowerFromPosition(servo, position), 0, 1.0);
            servo.isTargetSet = true;
        } else {
            throw new ClassCastException("servoData.actuator must be instance of CRServoImplEx or ServoImplEx");
        }
    }

    @Override
    public double getServoPosition(int portNumber) {
        final double last = lastKnown[portNumber];
        if(getData(portNumber).isContinuous()) {
            return Double.isNaN(last) ? 0.5 : last;
        }

        return Double.isNaN(last) ? 0.0 : last;
    }

    @Override
    public void forgetLastKnownPosition(int portNumber) {
        lastKnown[portNumber] = Double.NaN;
    }
    
    public double lastKnownPosition(int portNumber) {
        return lastKnown[portNumber];
    }

    public double lastKnownPower(int portNumber) {
        return Range.scale(lastKnown[portNumber], 0, 1, -1, 1);
    } 

    public void enableDry(boolean doEnable) {
        this.enableDry = doEnable;
    }

    @Override
    public void setServoPwmRange(int portNumber, @NonNull PwmRange range) {
        getData(portNumber).range = range;
        forgetLastKnownPosition(portNumber);
    }

    @Override
    @NonNull
    public PwmRange getServoPwmRange(int portNumber) {
        return getData(portNumber).range;
    }

    @Override
    public void setServoPwmEnable(int portNumber) {
        getData(portNumber).isEnabled = true;
    }

    @Override
    public void setServoPwmDisable(int portNumber) {
        getData(portNumber).isEnabled = false;
        getData(portNumber).unaffectedVelocity = 0;
        if(getData(portNumber).isContinuous()) {
            ((ContinuousServoData) getData(portNumber)).power = 0;
        }
    }

    @Override
    public boolean isServoPwmEnabled(int portNumber) {
        return getData(portNumber).isEnabled;
    }

    @Override
    public double getPulseWidth(int portNumber) {
        if(getData(portNumber).isContinuous()) {
            final ContinuousServoData servo = (ContinuousServoData) getData(portNumber);
            return pwmWidthFromPower(servo, servo.power);
        } else if(getData(portNumber).isPositional()) {
            final PositionalServoData servo = (PositionalServoData) getData(portNumber);
            return pwmWidthFromPosition(servo, servo.targetPosition);
        } else {
            throw new ClassCastException("servoData.actuator must be instance of CRServoImplEx or ServoImplEx");
        }
    }
    
    @Override
    public void setPulseWidth(int portNumber, double micros) {
        if(getData(portNumber).isContinuous()) {
            final ContinuousServoData servo = (ContinuousServoData) getData(portNumber);
            forgetLastKnownPosition(portNumber);
            servo.power = powerFromPwmWidth(servo, micros);
            servo.unaffectedVelocity = servo.power * servo.maxRevsPerSec;
            servo.isTargetSet = true;
        } else if(getData(portNumber).isPositional()) {
            final PositionalServoData servo = (PositionalServoData) getData(portNumber);
            forgetLastKnownPosition(portNumber);
            servo.targetPosition = servo.maxPosition * positionFromPwmWidth(servo, micros);
            servo.isTargetSet = true;
        } else {
            throw new ClassCastException("servoData.actuator must be instance of CRServoImplEx or ServoImplEx");
        }
    }

    @Override
    public void setServoType(int portNumber, ServoConfigurationType servoType) {
        if(isPortAvailable(portNumber)) {
            return;
        }

        // This isn't usefult for ANYTHING, but someone probably uses it.
        getData(portNumber).servoType = servoType;
    }

    @Override
    public Manufacturer getManufacturer() {
        return Manufacturer.Other;
    }

    @Override
    public String getDeviceName() {
        return "ServoControllerExFake. Hi youtube!";
    }

    private String safeGetDeviceName(HardwareDevice servo) {
        return servo == null ? "null" : servo.getDeviceName();
    }

    @Override
    public String getConnectionInfo() {
        String result = "ServoControllerExFake Connections:";
        for(int i = 0; i < this.totalPorts(); i++) {
            result += String.format("\n    [%d]: %s", i, safeGetDeviceName(servos[i].actuator));
        }
        return result;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void resetDeviceConfigurationForOpMode() {
        this.servos = new ServoData[6];
        this.lastKnown = clearedLastKnowns();
    }

    @Override
    public void close() {
        this.servos = null; // Allow the connections ot be garbage collected
    }

    /**
     * Gets the number of pulse-width-modulated microseconds used to generate the 
     * given position.
     * 
     * @param data Source of the current and max PWM ranges
     * @param position The position of the servo, in range [0, 1]
     * @return The width of PWM require to generate the given position
     */
    private double pwmWidthFromPosition(ServoData data, double position) {
        return Range.scale(
            position,
            0.0,
            1.0,
            data.maxPwm.usPulseLower,
            data.maxPwm.usPulseUpper
        );
    }
    
    /**
     * Gets the position which generates the given pulse-width-modulated width.
     * 
     * @param data Source of the current and max PWM ranges
     * @param micros Width of the PWM wave
     * @return The position that would be held, in range [-, 1]
     */
    private double positionFromPwmWidth(ServoData data, double position) {
        return Range.scale(
            position,
            data.maxPwm.usPulseLower,
            data.maxPwm.usPulseUpper,
            0.0,
            1.0
        );
    }

    /**
     * Gets the number of pulse-width-modulated microseconds used to generate the 
     * given power.
     * 
     * @param data Source of the current and max PWM ranges
     * @param power The power of the servo, in range [-1, 1]
     * @return The width of PWM require to generate the given power
     */
    private double pwmWidthFromPower(ServoData data, double power) {
        return Range.scale(
            power,
            -1.0,
            1.0,
            data.maxPwm.usPulseLower,
            data.maxPwm.usPulseUpper
        );
    }
    
    /**
     * Gets the power which generates the given pulse-width-modulated width.
     * 
     * @param data Source of the current and max PWM ranges
     * @param micros Width of the PWM wave
     * @return The power that would be held, in range [-1, 1]
     */
    private double powerFromPwmWidth(ServoData data, double power) {
        return Range.scale(
            power,
            data.maxPwm.usPulseLower,
            data.maxPwm.usPulseUpper,
            0.0,
            1.0
        );
    }


    /**
     * Converts an abstract position value into a normalized fraction of the max
     * PWM range. This considers the max and current PWM ranges of the given 
     * servo data, so that the return value represents the maximum PWM range,
     * assuming that the position is in terms of the current PWM range. 
     * 
     * @param data Source of the current and max PWM ranges
     * @param position Value on interval [0, 1] where 0 correpsonds to the 
     * `usPulseLower` on the current range and 1 to `usPulseUpper`.
     * @return A value on interval [-0.5, 0.5] where -0.5 corresponds to 
     * `usPulseLower` on the max range and 0.5 to `usPulseUpper`.
     */
    private double pwmPowerFromPosition(ServoData data, double position) {
        final double midpoint = 0.5 * (data.maxPwm.usPulseLower + data.maxPwm.usPulseUpper);
        final double length = data.maxPwm.usPulseUpper - data.maxPwm.usPulseLower;
        final double pwmMicros = Range.scale(
            position, 
            0.0, 
            1.0,
            data.range.usPulseLower, 
            data.range.usPulseUpper
        );

        System.out.println("[pwm power position] midpoint: " + midpoint);
        System.out.println("[pwm power position] length: " + length);
        System.out.println("[pwm power position] pwmMicros: " + pwmMicros);

        return (pwmMicros - midpoint) / length;
    }

    /**
     * Calculates the inverse to `pwmPowerFromPosition()`. The following 
     * examples illustrate what this means:
     * 
     * ```java
     * positionFromPwmPower(data, pwmPowerFromPosition(data, 0.83)) == 0.83 // True
     * pwmPowerFromPosition(data, positionFromPwmPower(data, 0.16)) == 0.16 // True
     * ```
     * 
     * @param data Source of the current and max PWM ranges
     * @param power Value on interval [-0.5, 0.5] where -0.5 correpsonds to the 
     * `usPulseLower` field on the current range and 0.5 to `usPulseUpper`.
     * @return A value on interval [0, 1.0] where 0 corresponds to 
     * `usPulseLower` field on the max range and 1.0 to `usPulseUpper`.
     */
    private double positionFromPwmPower(ServoData data, double power) {
        final double midpoint = 0.5 * (data.maxPwm.usPulseLower + data.maxPwm.usPulseUpper);
        final double length = data.maxPwm.usPulseUpper - data.maxPwm.usPulseLower;
        final double pwmMicros = power * length + midpoint;
        return Range.scale(
            pwmMicros,
            data.range.usPulseLower,
            data.range.usPulseUpper,
            0.0,
            1.0
        );
    }
}
