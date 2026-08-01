/*
 * ServoImplExFakeUnitTest.java
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

package clarson.ftc.faker.test;

import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.ServoControllerExFake;
import clarson.ftc.faker.ServoImplExFake;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.util.AbstractTwoWayUpdateable;
import clarson.ftc.faker.wrapper.PositionalServoData;
import clarson.ftc.faker.wrapper.ServoData;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import com.qualcomm.robotcore.util.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import static clarson.ftc.faker.test.TestUtil.*;
import static com.qualcomm.robotcore.hardware.PwmControl.PwmRange;
import static org.junit.jupiter.api.Assertions.*;

public class ServoImplExFakeUnitTest {
    @DisplayName("Can construct")
    @Test 
    void canConstruct() {
        
        assertDoesNotThrow(() -> new ServoImplExFake(312, 5));
        assertDoesNotThrow(() -> new ServoImplExFake(-1600, 1));
        assertDoesNotThrow(() -> new ServoImplExFake(-1600, 5));

        assertDoesNotThrow(() -> new ServoImplExFake(312,  5, 0));
        assertDoesNotThrow(() -> new ServoImplExFake(312,  1, 1200));
        assertDoesNotThrow(() -> new ServoImplExFake(312,  5, -1200));
        assertDoesNotThrow(() -> new ServoImplExFake(-312, 1, -1200));
        
        assertDoesNotThrow(() -> new ServoImplExFake(312, 5, 0, new PwmRange(0, 100)));
        assertDoesNotThrow(() -> new ServoImplExFake(312, 1, 1200, new PwmRange(0, 100, 40)));
        assertDoesNotThrow(() -> new ServoImplExFake(-312, 5, -1200, new PwmRange(100, 98300)));
        assertDoesNotThrow(() -> new ServoImplExFake(312, 1, -1200, new PwmRange(1000, 98300, 40)));
    }

    
    @DisplayName("Initial settings are FORWARD, enabled, and power 0")
    @Test 
    void checkInitialSettings() {
        final ServoImplEx servo = new ServoImplExFake(180, 5);
        assertEquals(Servo.Direction.FORWARD, servo.getDirection());
        assertEquals(0, servo.getPosition());
        assertTrue(servo.isPwmEnabled());
    }

    @ParameterizedClass
    @ValueSource(strings = { "FORWARD", "REVERSE" })
    @DisplayName("Construction Dependent")
    @Nested
    class ConstructionDependent {
        ServoImplExFake servo = null;
        double rpm = 180;
        double turns = 5;
        PwmRange maxPwm = new PwmRange(500, 2500);
        final double TOL = 0.5 / 360; // half a degree of tolerance;

        @Parameter
        String directionName;

        @BeforeEach
        void constructServo() {
            servo = new ServoImplExFake(rpm, turns, 0, maxPwm);
            servo.setPwmRange(maxPwm);

            System.out.println("\n[construct servo] directionName: " + directionName);
            if(directionName.equals("FORWARD")) {
                servo.setDirection(Servo.Direction.FORWARD);
            } else {
                servo.setDirection(Servo.Direction.REVERSE);
            }
        }

        /**
         * Sets the servo position to the given position and waits for the position
         * to be reached within specificed tolerance.
         * 
         * @param position  Where to rotate to, on interval [0, 1]
         * @param tolerance Plus-or-minus difference from target, in revolutions
         * @return The change in the position as a result of setting position.
         */
        double driveServoTo(double position, double tolerance) {
            final PositionalServoData data = servo.getData();
            final double startPosition = data.position;
            data.tolerance = tolerance;
            servo.setPosition(position);

            final double MAX = 2 * turns / rpm * 60;
            final double STEP = 0.001;
            double t = 0;
            while(t < MAX && Math.abs(data.position - data.targetPosition) > tolerance) {
                servo.update(STEP);
                t += STEP;
            }

            // System.out.println("[drive servo to] :" + );
            System.out.println("[drive servo to] elapsed: " + t);
            System.out.println("[drive servo to] start: " + startPosition);
            System.out.println("[drive servo to] position: " + data.position);
            System.out.println("[drive servo to] target: " + data.targetPosition);
            System.out.println("[drive servo to] tolerance: " + data.tolerance);

            return data.position - startPosition;
        }
        
        /**
         * Drives the servo for the given amount of seconds, with the given 
         * tolerance.
         * 
         * @param position  Where to rotate to, on interval [0, 1]
         * @param tolerance Plus-or-minus difference from target, in revolutions
         * @param duration Number of seconds that are to be simulated.
         * @param step Seconds between each consecutive call to `update()`
         * @return The change in the position as a result of setting position.
         */
        double driveServoTo(double position, double tolerance, double duration, double step) {
            final PositionalServoData data = servo.getData();
            final double startPosition = data.position;
            data.tolerance = tolerance;
            servo.setPosition(position);

            double t = 0;
            while(t < duration/*  && Math.abs(data.position - data.targetPosition) > tolerance */) {
                servo.update(step);
                t += Math.abs(step);
            }

            // System.out.println("[drive servo to] :" + );
            System.out.println("[drive servo to] elapsed: " + t);
            System.out.println("[drive servo to] position: " + data.position);
            System.out.println("[drive servo to] target: " + data.targetPosition);
            System.out.println("[drive servo to] tolerance: " + data.tolerance);

            return data.position - startPosition;
        }

        /**
         * Times the duration of setting the target to the given position and 
         * waiting for the position to be reached within specificed tolerance.
         * 
         * @param position  Where to rotate to, on interval [0, 1]
         * @param tolerance Plus-or-minus difference from target, in revolutions
         * @return The change in the position as a result of setting position.
         */
        double timeServoTo(double position, double tolerance) {
            final PositionalServoData data = servo.getData();
            final double startPosition = data.position;
            data.tolerance = tolerance;
            servo.setPosition(position);

            final double MAX = 2 * turns / rpm * 60;
            final double STEP = 0.001;
            double t = 0;
            while(t < MAX && Math.abs(data.position - data.targetPosition) > tolerance) {
                servo.update(STEP);
                t += STEP;
            }

            // System.out.println("[drive servo to] :" + );
            System.out.println("[drive servo to] elapsed: " + t);
            System.out.println("[drive servo to] start: " + startPosition);
            System.out.println("[drive servo to] position: " + data.position);
            System.out.println("[drive servo to] target: " + data.targetPosition);
            System.out.println("[drive servo to] tolerance: " + data.tolerance);

            return t;
        }
        /**
         * Times the duration of setting the target to the given position and 
         * waiting for the position to be reached within specificed tolerance.
         * 
         * @param position  Where to rotate to, on interval [0, 1]
         * @param tolerance Plus-or-minus difference from target, in revolutions
         * @param maxTime Max amount of seconds that can be simulated
         * @param step seconds between each update
         * @return The change in the position as a result of setting position.
         */
        double timeServoTo(double position, double tolerance, double maxTime, double step) {
            final PositionalServoData data = servo.getData();
            final double startPosition = data.position;
            data.tolerance = tolerance;
            servo.setPosition(position);

            // final double MAX = 2 * turns / rpm * 60;
            // final double STEP = 0.001;
            double t = 0;
            while(t < maxTime && Math.abs(data.position - data.targetPosition) > tolerance) {
                servo.update(step);
                t += step;
            }

            // System.out.println("[drive servo to] :" + );
            System.out.println("[drive servo to] elapsed: " + t);
            System.out.println("[drive servo to] start: " + startPosition);
            System.out.println("[drive servo to] position: " + data.position);
            System.out.println("[drive servo to] target: " + data.targetPosition);
            System.out.println("[drive servo to] tolerance: " + data.tolerance);

            return t;
        }

        @DisplayName("Set Position")
        @Nested
        class SetPosition {
            @DisplayName("Set Position = Get Position (unless clipped)")
            @Test
            void setPositionArgumentStored() {
                final double[] positions = { 1.0, 0, 0.33, -1.0, 1.5, 0.5 };
                final double[] expected =  { 1.0, 0, 0.33,    0, 1.0, 0.5 };

                for(int i = 0; i < positions.length; i++) {
                    servo.setPosition(positions[i]);
                    assertFloatEquals(expected[i], servo.getPosition(), 1e-13);
                }
            }

            @DisplayName("setPosition reaches position on time")
            @Test
            void reachesPosition() {
                final double target = 0.5;

                // Making sure RUN_TO_POSITION reaches the specified target within 3 seconds.
                // This is done with a 30 millisecond update rate to simulate real life latency,
                // and to "reach the position", the current position must get within 3 degrees.
                final double maxSeconds = 3.0;
                final double updateDelaySeconds = 0.030; // 30 milliseconds
                final double tolerance = 1.5 / 360.0; // Revolutions
                final PositionalServoData servoData = servo.getData();

                servoData.tolerance = tolerance;
                servo.setPosition(target);

                double t = 0;
                while(
                    Math.abs(servoData.position - servoData.targetPosition) >= tolerance 
                    && t < maxSeconds
                ) {
                    servo.update(updateDelaySeconds);
                    t += updateDelaySeconds;
                }

                System.out.println("[reaches on time] t: " + t);
                System.out.println("[reaches on time] currentPos: " + servoData.position);
                System.out.println("[reaches on time] target: " + servoData.targetPosition);
                System.out.println("[reaches on time] tolerance: " + tolerance);
                assertTrue(t < maxSeconds); // Making sure the loop didn't timeout.
                assertTrue(Math.abs(servoData.position - servoData.targetPosition) < tolerance); // Did the servo *actually* reach the position?
            }

            @DisplayName("Holds position after reaching")
            @Test
            void holdsPositionAfterReaching() {
                final double target = 0.5;

                // Making sure RUN_TO_POSITION reaches the specified target within 3 seconds.
                // This is done with a 30 millisecond update rate to simulate real life latency,
                // and to "reach the position", the current position must get within 0.5 degree.
                final double maxSeconds = 3.0;
                final double updateDelaySeconds = 0.030; // 30 milliseconds
                final double tolerance = 0.5 / 360.0; // Revolutions
                final PositionalServoData servoData = servo.getData();

                servoData.tolerance = tolerance;
                servo.setPosition(target);

                double t = 0;
                while(
                    Math.abs(servoData.position - servoData.targetPosition) >= tolerance 
                    && t < maxSeconds
                ) {
                    servo.update(updateDelaySeconds);
                    t += updateDelaySeconds;
                }

                System.out.println("[holds position after] t: " + t);
                System.out.println("[holds position after] currentPos: " + servoData.position);
                System.out.println("[holds position after] target: " + servoData.targetPosition);
                System.out.println("[holds position after] tolerance: " + tolerance);
                assertTrue(t < maxSeconds); // Making sure the loop didn't timeout.
                assertTrue(Math.abs(servoData.position - servoData.targetPosition) < tolerance);

                // Seeing if the position is held
                while(t < maxSeconds) {
                    System.out.println("[holds position after] t: " + t);
                    System.out.println("[holds position after] position: " + servoData.position);
                    assertTrue(Math.abs(servoData.position - servoData.targetPosition) < tolerance);
                    servo.update(updateDelaySeconds);
                    t += updateDelaySeconds;   
                }
            }
            
        }

        @DisplayName("PWM")
        @Nested 
        class Pwm {
            @DisplayName("Set Servo Enabled = Get Servo")
            @Test
            void setPwmEnabledArgumentStored() {
                servo.setPwmEnable();
                assertTrue(servo.isPwmEnabled());

                servo.setPwmDisable();
                assertFalse(servo.isPwmEnabled());
            }

            @DisplayName("Disabling servo prevents setting power")
            @Test
            void disableDisablesSetPower() {
                final PositionalServoData data = servo.getData();
                driveServoTo(0.8, TOL);
                final double unaffectedPosition = data.position;

                servo.setPwmDisable();
                driveServoTo(0.5, TOL);
                final double noPowerDiabledPosition = data.position;
                driveServoTo(0.314, TOL);
                final double poweredDisabledPosition = data.position;

                assertNotEquals(0, unaffectedPosition);
                assertEquals(unaffectedPosition, noPowerDiabledPosition);
                assertEquals(unaffectedPosition, poweredDisabledPosition);
                assertEquals(noPowerDiabledPosition, poweredDisabledPosition);
            }

            @DisplayName("Disabled servo affected by addAngularVelOffset")
            @Test 
            void disabledAffectedByaddAngularVelOffset() {
                final double unaffectedDeltaPos = driveServoTo(0.5, TOL);
                assertNotEquals(0, unaffectedDeltaPos);
                System.out.println(unaffectedDeltaPos - servo.getData().targetPosition);
                System.out.println(TOL);
                assertTrue(Math.abs(unaffectedDeltaPos - servo.getData().targetPosition) <= TOL);

                // Seeing if it holds position under load
                // final double posFromTarget = Math.abs(servo.getData().position - servo.getData().targetPosition);
                servo.setAngularVelOffset(2 * Math.PI);
                final double enabledDeltaPos = driveServoTo(0.5, TOL, 2.0, 0.01);
                assertFloatEquals(0, enabledDeltaPos, TOL);

                // Disabling
                servo.setPwmDisable();
                servo.setAngularVelOffset(2 * Math.PI);
                final double disabledDeltaPos = driveServoTo(0.5, TOL, 1.0, 0.01);
                assertFloatEquals(1, disabledDeltaPos, 1e-9);
            }
        
            @DisplayName("Renabled can set power")
            @Test
            void renabledStillWorks() {
                final double unaffectedDeltaPos = driveServoTo(0.5, TOL);
                assertNotEquals(0, unaffectedDeltaPos);
                System.out.println(unaffectedDeltaPos - servo.getData().targetPosition);
                System.out.println(TOL);
                assertTrue(Math.abs(unaffectedDeltaPos - servo.getData().targetPosition) <= TOL);

                // Seeing if it holds position under load
                // final double posFromTarget = Math.abs(servo.getData().position - servo.getData().targetPosition);
                final double enabledDeltaPos = driveServoTo(0.5, TOL, 2.0, 0.01);
                assertFloatEquals(0, enabledDeltaPos, TOL);

                // Disabling
                servo.setPwmDisable();
                final double disabledDeltaPos = driveServoTo(0.25, TOL, 1.0, 0.01);
                assertFloatEquals(0, disabledDeltaPos, 1e-9);

                // Renabling 
                servo.setPwmEnable();
                final double reenabledDeltaPos = driveServoTo(0.25, TOL, 1.0, 0.01);
                assertFloatEquals(0.25 * turns, Math.abs(reenabledDeltaPos), TOL);
            }
        
            @DisplayName("SET PWM Range = Get PWM Range")
            @Test
            void setPwmRangeArgumentStored() {
                final PwmRange range = new PwmRange(1000, 2000);
                servo.setPwmRange(range);
                assertEquals(range, servo.getPwmRange());

                final PwmRange range2 = new PwmRange(1314.15, 1414.2);
                servo.setPwmRange(range2);
                assertEquals(range2, servo.getPwmRange());
                
                final PwmRange range3 = new PwmRange(628.31, 2400);
                servo.setPwmRange(range3);
                assertEquals(range3, servo.getPwmRange());
            }

            @DisplayName("Setting PWM subset range limits the extrema")
            @Test
            void pwmSubsetLimitsRange() {
                final PositionalServoData data = servo.getData();
                final double direction = directionName.equals("REVERSE") ? -1 : 1;
                driveServoTo(direction * 1.0, TOL);
                assertFloatEquals(turns, data.position, TOL);
                driveServoTo(direction * -1.0, TOL);
                assertFloatEquals(-0, data.position, TOL);

                // Symmetric subset. 800 mircos difference in length (400 micros on each end)
                final double start1 = 900;
                final double end1 = 2100;
                servo.setPwmRange(new PwmRange(start1, end1));
                driveServoTo(-1.0 * direction, TOL);
                assertFloatEquals(0.2 * turns, data.position, TOL);
                driveServoTo(1.0 * direction, TOL);
                assertFloatEquals(0.8 * turns, data.position, TOL);

                // Assymetric subset. 1200 micros difference in length (400 on the lower, 800 on the upper).
                final double start2 = 900;
                final double end2 = 1700;
                servo.setPwmRange(new PwmRange(start2, end2));
                driveServoTo(-1.0 * direction, TOL);
                assertFloatEquals(0.2 * turns, data.position, TOL);
                driveServoTo(1.0 * direction, TOL);
                assertFloatEquals(0.6 * turns, data.position, TOL);

                // Identity subset. Making sure that we are able to reset the PWM range when we want to
                final double start4 = maxPwm.usPulseLower;
                final double end4 = maxPwm.usPulseUpper;
                servo.setPwmRange(new PwmRange(start4, end4));
                driveServoTo(-1.0 * direction, TOL);
                assertFloatEquals(0, data.position, TOL);
                driveServoTo(1.0 * direction, TOL);
                assertFloatEquals(turns, data.position, TOL);
                
                // Null subset. Length 0 micros. Positions should be the same
                final double start3 = 1200;
                final double end3 = 1200;
                servo.setPwmRange(new PwmRange(start3, end3));
                driveServoTo(-1.0 * direction, TOL);
                assertFloatEquals(0.35 * turns, data.position, TOL);
                driveServoTo(1.0 * direction, TOL);
                assertFloatEquals(0.35 * turns, data.position, TOL);
            }

            @DisplayName("Setting PWM superset range clips the extrema")
            @Test
            void pwmSuperSetClipsRange() {
                final double direction = directionName.equals("REVERSE") ? -1 : 1;
                final PositionalServoData data = servo.getData();
                driveServoTo(-1.0 * direction, TOL);
                assertFloatEquals(0, data.position, TOL);
                driveServoTo(1.0 * direction, TOL);
                assertFloatEquals(turns, data.position, TOL);

                final double start1 = 300;
                final double end1 = 2700;
                servo.setPwmRange(new PwmRange(start1, end1));
                driveServoTo(-1.0 * direction, TOL);
                assertFloatEquals(0, data.position, TOL);
                driveServoTo(1.0 * direction, TOL);
                assertFloatEquals(turns, data.position, TOL);

                
                final double start2 = 300;
                final double end2 = 3100;
                servo.setPwmRange(new PwmRange(start2, end2));
                driveServoTo(-1.0 * direction, TOL);
                assertFloatEquals(0, data.position, TOL);
                driveServoTo(1.0 * direction, TOL);
                assertFloatEquals(turns, data.position, TOL);

                final double start3 = -500;
                final double end3 = 2600;
                servo.setPwmRange(new PwmRange(start3, end3));
                driveServoTo(-1.0 * direction, TOL);
                assertFloatEquals(0, data.position, TOL);
                driveServoTo(1.0 * direction, TOL);
                assertFloatEquals(turns, data.position, TOL);
            }
        
        }

        @DisplayName("Add Velocity Misc.")
        @Nested
        class AddVelocity {
            @DisplayName("Small offset delays setPosition")
            @Test
            void smallOffsetDelays() {
                // Verifiying that the position can be reached in time
                final double unaffectedTime = timeServoTo(0.5, TOL);
                assertFloatEquals(0.5 * turns, servo.getData().position, TOL);
                assertFloatEquals(0.5 * turns / rpm * 60, unaffectedTime, 0.5);
                assertFloatNotEquals(0, unaffectedTime, 1e-5);

                // Testing the the time is longer
                servo.setAngularVelOffset(0.6 * 2 * Math.PI * rpm / 60);
                final double reversedPosition = directionName.equals("REVERSE") ? 1.0 : 0;
                final double affectedTime = timeServoTo(reversedPosition, TOL);
                assertFloatEquals(0, servo.getData().position, TOL);
                assertFloatNotEquals(0, unaffectedTime, 1e-5);
                System.out.println("🎈");
                System.out.println("[small offset delays] affectedTime: " + affectedTime);
                System.out.println("[small offset delays] unaffectedTime: " + unaffectedTime);
                assertTrue(unaffectedTime / affectedTime <= 0.5);
            }

            @DisplayName("Large offset prevents setPosition")
            @Test
            void largeOffsetPrevents() {
                // Verifiying that the position can be reached in time
                final double unaffectedTime = timeServoTo(0.5, TOL);
                assertFloatEquals(0.5 * turns, servo.getData().position, TOL);
                assertFloatEquals(0.5 * turns / rpm * 60, unaffectedTime, 0.5);
                assertFloatNotEquals(0, unaffectedTime, 1e-5);

                // Testing the the position has (A) moved and (B) it not the target
                servo.setAngularVelOffset(1.5 * 2 * Math.PI * rpm / 60);
                final double affectedTime = timeServoTo(0, TOL);
                assertFloatNotEquals(0, servo.getData().position, TOL);
                assertFloatNotEquals(0.5 * turns, servo.getData().position, TOL);
                assertFloatNotEquals(0, affectedTime, 1e-5);
                // assertTrue(affectedTime / unaffectedTime >= 0.5);
            }

            @DisplayName("Two small offsets can prevent setPosition")
            @Test
            void sumOfPartsCanPrevent() {
                // Verifiying that the position can be reached in time
                final double reversedPos = directionName.equals("REVERSE") ? 0 : 1.0; // Rotates around fully
                final double unaffectedTime = timeServoTo(reversedPos, TOL);
                assertFloatEquals(turns, servo.getData().position, TOL);
                assertFloatEquals(turns / rpm * 60, unaffectedTime, 0.5);
                assertFloatNotEquals(0, unaffectedTime, 1e-5);

                // Testing the the time is longer, but it reaches there
                servo.addAngularVelOffset(0.75 * 2 * Math.PI * rpm / 60);
                final double maxTime = 10 * turns / rpm * 60;
                final double affectedTime = timeServoTo(0.5, TOL, maxTime, 0.001); // Give extra time because its gonna take a WHILE
                assertFloatEquals(0.5 * turns, servo.getData().position, TOL);
                assertFloatNotEquals(0, unaffectedTime, 1e-5);
                assertTrue((0.5 * unaffectedTime) / affectedTime <= 0.3);

                // Testing that with another offset added (not overriding)
                servo.addAngularVelOffset(0.75 * 2 * Math.PI * rpm / 60);
                final double affectedSumOfPartsTime = timeServoTo(1 - reversedPos, TOL, maxTime, 0.001);
                assertFloatNotEquals(0, servo.getData().position, TOL);
                assertFloatNotEquals(0.5 * turns, servo.getData().position, TOL);
                assertFloatNotEquals(0, affectedSumOfPartsTime, 1e-5);
                assertFloatEquals(maxTime, affectedSumOfPartsTime, 0.001);
            }
        }
    }

    @Nested
    class UpdaterDependent {
        private ModularUpdater updater = new ModularUpdater();
        ServoControllerExFake controller;
        ServoImplExFake servo = null;

        LynxUsbDeviceImplFake lynxUsb = null;
        LynxModuleHardwareFake lynx = null;

        double rpm = 180;
        double turns = 5;
        PwmRange maxPwm = new PwmRange(500, 2500);

        private final MockUpdateable counter = new MockUpdateable();

        LynxModuleHardwareFake createLynxModule(LynxUsbDeviceImplFake lynx) {
            LynxModuleHardwareFake module = null;
            try {
                module = (LynxModuleHardwareFake) lynx.getOrAddModule(
                    new LynxModuleDescription.Builder(-1, true)
                        .setUserModule()
                        // .setSystemSynthetic()
                        .build()    
                );
            }  catch (InterruptedException unused) {
                // Do nothing...
            } catch(RobotCoreException err) {
                fail("RobotCoreException caught in addLynxModule(): " + err.getMessage());
            }
            
            try {
                lynx.armOrPretend();
            } catch(RobotCoreException | InterruptedException err) {
                fail(err);
            }

        
            return module;
        }

        @BeforeEach
        void constructAndRegister() {
            System.out.println("*****************************");

            // Creating the controller
            controller = new ServoControllerExFake();
            servo = new ServoImplExFake(new PositionalServoData(rpm, turns, 0, maxPwm), controller);
            servo.setPwmRange(maxPwm);
            
            lynxUsb = new LynxUsbDeviceImplFake();
            lynx = createLynxModule(lynxUsb);
            updater.register(servo, lynx);
            updater.register(counter, lynx);
        }

        /**
         * Counts the number of times it has been updated
         */
        private final class MockUpdateable extends AbstractTwoWayUpdateable {
            private int totalUpdates = 0;
            private double lastDeltaSec = 0;

            @Override
            protected final double updateImplementation(double deltaSec) {
                totalUpdates++;
                lastDeltaSec = deltaSec;
                return 1;
            }

            public final void clearCount() {
                totalUpdates = 0;
            }

            public final int getTotalUpdates() {
                return this.totalUpdates;
            }
            
            public final double getLastDeltaSec() {
                return this.lastDeltaSec;
            }

            /**
             * Alias for getTotalUpdates()
             */
            public final int getTotalCalls() {
                return this.getTotalUpdates();
            }
        }

        @DisplayName("setUpdatingEnabled and isUpdatingEnabled match")
        @Test
        void setAndGetUpdatingEnabledMatch() {
            servo.setUpdatingEnabled(false);
            assertEquals(false, servo.isUpdatingEnabled());
            
            servo.setUpdatingEnabled(false);
            assertEquals(false, servo.isUpdatingEnabled());

            servo.setUpdatingEnabled(true);
            assertEquals(true, servo.isUpdatingEnabled());

            servo.setUpdatingEnabled(false);
            assertEquals(false, servo.isUpdatingEnabled());

            servo.setUpdatingEnabled(true);
            assertEquals(true, servo.isUpdatingEnabled());
        }
        
        @DisplayName("forget removes updater internally; remember adds it back")
        @Test
        void forgetRemovesRememberAdds() {
            // NOTE: Assumes that getPulseWidth simulates delay always
            servo.getPulseWidth();
            System.out.println("[ForgetRemoves]");
            assertEquals(1, counter.getTotalUpdates(), "is registered initially");
            
            servo.forget(updater);
            servo.getPulseWidth();
            assertEquals(1, counter.getTotalUpdates(), "updater no longer updated by servo");

            servo.getPulseWidth();
            servo.getPulseWidth();
            servo.getPulseWidth();
            assertEquals(1, counter.getTotalUpdates(), "updater still no longer updated by servo");

            servo.remember(updater);
            servo.getPulseWidth();
            assertEquals(2, counter.getTotalUpdates(), "updater now is updated by servo");
            
            servo.getPulseWidth();
            servo.getPulseWidth();
            servo.getPulseWidth();
            assertEquals(5, counter.getTotalUpdates(), "updater still is updated by servo");
            System.out.println("=============================\n");
        }

        @DisplayName("setPosition updates automatically when registered with an updater")
        @Test
        void setPositionUpdatesAutomaticallyWhenRegistered() {
            servo.setPosition(0.5);
            assertEquals(1, counter.getTotalUpdates());

            servo.setPosition(0.75);
            assertEquals(2, counter.getTotalUpdates());

            System.out.println();
            final double[] pos = { 
                0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90,
                0.93, 0.94, 0.95, 0.96, 0.97, 0.99, 0.99, 1.00
            };
            for(int i = 2; i < 7; i++) {
                servo.setPosition(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                servo.setPosition(pos[2 + 2 * (i - 1)]);
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }

        @DisplayName("setPosition delays for a different position when bulk caching is off")
        @Test
        void setPositionDelaysForDifferentPositionInOff() {
            servo.setPosition(0.5);
            assertEquals(1, counter.getTotalUpdates());

            servo.setPosition(0.5);
            assertEquals(1, counter.getTotalUpdates(), "Total updates did not change");

            servo.setPosition(0.75);
            assertEquals(2, counter.getTotalUpdates());

            System.out.println();
            final double[] pos = { 
                0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90,
                0.93, 0.94, 0.95, 0.96, 0.97, 0.99, 0.99, 1.00
            };
            for(int i = 2; i < 7; i++) {
                servo.setPosition(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());

                servo.setPosition(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates(), "Total updates did not change");
                
                servo.setPosition(pos[2 + 2 * (i - 1)]);
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("setPosition delays the exact same even on MANUAL")
        @Test
        void setPositionDelaysForDifferentPositionInManual() {
            lynx.setBulkCachingMode(LynxModuleHardwareFake.BulkCachingMode.MANUAL);
            servo.setPosition(0.5);
            assertEquals(1, counter.getTotalUpdates());

            servo.setPosition(0.5);
            assertEquals(1, counter.getTotalUpdates(), "Total updates did not change");

            servo.setPosition(0.75);
            assertEquals(2, counter.getTotalUpdates());

            System.out.println();
            final double[] pos = { 
                0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90,
                0.93, 0.94, 0.95, 0.96, 0.97, 0.99, 0.99, 1.00
            };
            for(int i = 2; i < 7; i++) {
                servo.setPosition(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());

                servo.setPosition(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates(), "Total updates did not change");
                
                servo.setPosition(pos[2 + 2 * (i - 1)]);
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("setPosition delays the exact same even on AUTO")
        @Test
        void setPositionDelaysForDifferentPositionInAuto() {
            lynx.setBulkCachingMode(LynxModuleHardwareFake.BulkCachingMode.AUTO);
            servo.setPosition(0.5);
            assertEquals(1, counter.getTotalUpdates());

            servo.setPosition(0.5);
            assertEquals(1, counter.getTotalUpdates(), "Total updates did not change");

            servo.setPosition(0.75);
            assertEquals(2, counter.getTotalUpdates());

            System.out.println();
            final double[] pos = { 
                0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90,
                0.93, 0.94, 0.95, 0.96, 0.97, 0.99, 0.99, 1.00
            };
            for(int i = 2; i < 7; i++) {
                servo.setPosition(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());

                servo.setPosition(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates(), "Total updates did not change");
                
                servo.setPosition(pos[2 + 2 * (i - 1)]);
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getPosition never simulates delay")
        @Test
        void getPositionUpdatesAutomaticallyWhenRegistered() {
            servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates());

            servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates());

            System.out.println();
            for(int i = 2; i < 7; i++) {
                servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates());
                
                servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates());
            }
        }

        @DisplayName("getPosition never simulates delay, even in OFF")
        @Test
        void getPositionDelaysForDifferentPositionInOff() {
            servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates());

            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");

            servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates());

            System.out.println();
            for(int i = 2; i < 7; i++) {
                servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates());

                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");
                
                servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getPosition never simulates delay, even in MANUAL")
        @Test
        void getPositionDelaysForDifferentPositionInManual() {
            lynx.setBulkCachingMode(LynxModuleHardwareFake.BulkCachingMode.MANUAL);
            servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates());

            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");

            servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates());

            System.out.println();
            for(int i = 2; i < 7; i++) {
                servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates());

                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");
                
                servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getPosition never simulates delay, even in AUTO")
        @Test
        void getPositionDelaysForDifferentPositionInAuto() {
            lynx.setBulkCachingMode(LynxModuleHardwareFake.BulkCachingMode.AUTO);
            servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates());

            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");

            servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
            servo.getPosition();
            assertEquals(0, counter.getTotalUpdates());

            System.out.println();
            for(int i = 2; i < 7; i++) {
                servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates());

                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");
                
                servo.getData().position += 0.05; // Change position in case getPosition() needs change to simulate delay
                servo.getPosition();
                assertEquals(0, counter.getTotalUpdates());
            }
        }
        
        // TODO: Test PWM methods with an updater
    }
}