/*
 * CRServoImplExFakeUnitTest.java
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

import clarson.ftc.faker.CRServoImplExFake;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.ServoControllerExFake;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.util.AbstractTwoWayUpdateable;
import clarson.ftc.faker.wrapper.ContinuousServoData;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.CRServoImplEx;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;
import com.qualcomm.robotcore.hardware.PwmControl;
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
import static org.junit.jupiter.api.Assumptions.*;

class CRServoImplExFakeUnitTest {
    @DisplayName("Can construct")
    @Test 
    void canConstruct() {
        assertDoesNotThrow(() -> new CRServoImplExFake(312));
        assertDoesNotThrow(() -> new CRServoImplExFake(-1600));
        assertDoesNotThrow(() -> new CRServoImplExFake(-1600));

        assertDoesNotThrow(() -> new CRServoImplExFake(312, 0));
        assertDoesNotThrow(() -> new CRServoImplExFake(312, 1200));
        assertDoesNotThrow(() -> new CRServoImplExFake(312, -1200));
        assertDoesNotThrow(() -> new CRServoImplExFake(-312, -1200));
        
        assertDoesNotThrow(() -> new CRServoImplExFake(312, 0, new PwmRange(0, 100)));
        assertDoesNotThrow(() -> new CRServoImplExFake(312, 1200, new PwmRange(0, 100, 40)));
        assertDoesNotThrow(() -> new CRServoImplExFake(-312, -1200, new PwmRange(100, 98300)));
        assertDoesNotThrow(() -> new CRServoImplExFake(312, -1200, new PwmRange(1000, 98300, 40)));
    }

    @DisplayName("Initial settings are FORWARD, enabled, and power 0")
    @Test 
    void checkInitialSettings() {
        final CRServoImplEx servo = new CRServoImplExFake(180);
        assertEquals(CRServo.Direction.FORWARD, servo.getDirection());
        assertEquals(0, servo.getPower());
        assertTrue(servo.isPwmEnabled());
    }

    @ParameterizedClass
    @ValueSource(strings = {"FORWARD", "REVERSE"})
    @DisplayName("Construction Dependent")
    @Nested 
    class ConsructionDependent {
        private CRServoImplExFake servo;
        private double rpm = 180;
        private double maxSpeed = rpm / 60;
        private PwmRange maxPwmRange = new PwmRange(1000, 2000);
        
        @Parameter
        private String directionName;

        @BeforeEach 
        void constructMotor() {
            servo = new CRServoImplExFake(rpm, 0, maxPwmRange);
            servo.setPwmRange(new PwmRange(1000, 2000));

            if(directionName.equals("FORWARD")) {
                servo.setDirection(DcMotor.Direction.FORWARD);
            } else if(directionName.equals("REVERSE")) {
                servo.setDirection(DcMotor.Direction.REVERSE);
            }
        }

        @DisplayName("DcMotorSimple Inherited")
        @Nested
        class DcMotorSimpleInherited {
            @DisplayName("Initial Power 0")
            @Test
            void initialPowerIsConstant() {
                assertFloatEquals(0, servo.getPower(), 1e-7);
            }

            @DisplayName("Set Power Arg = Get Power")
            @Test
            void setPowerArgumentStored() {
                final double[] powers = { 0, 1.0, 0.0, -1.0, 0.5, 0.25, -0.5, -0.25 };
                for(final double power : powers) {
                    servo.setPower(power);
                    assertFloatEquals(power, servo.getPower(), 1e-13);
                }
            }

            /* @Disabled("This is covered by test CRServoImplExFakeUnitTest.checkInitialSettings")
            @DisplayName("Initial Direction Forward")
            @Test 
            void initialDirectionForward() {
                assertEquals(DcMotor.Direction.FORWARD, servo.getDirection());
            }
            */
            
            @DisplayName("Set Direction Arg = Get Direction") 
            @Test
            void setDirectionArgumentStored() {
                final DcMotor.Direction[] dirs = DcMotor.Direction.values();
                for(final DcMotor.Direction dir : dirs) {
                    servo.setDirection(dir);
                    assertEquals(dir, servo.getDirection());
                }
            }

            @DisplayName("Update moves servo forward")
            @Test
            void updateMotorForward() {
                servo.setDirection(DcMotor.Direction.FORWARD);
                servo.setPower(1.0);
                
                // Seeing that the returned delta is as expected
                final double maxTickSpeed = rpm / 60;
                final double seconds = 0.016;
                assertEquals(maxTickSpeed * seconds, servo.update(seconds));

                // Seeing that the returned delta is, in fact, a delta and not the accumulated
                assertNotEquals(3 * maxTickSpeed * seconds, servo.update(seconds * 2));

                // Making sure the delta is accurate with varying input
                assertEquals(1.5 * maxTickSpeed * seconds, servo.update(seconds * 1.5));

                // Making sure the delta is still accurate after a power change
                servo.setPower(-0.33);
                assertEquals(-0.33 * maxTickSpeed * seconds, servo.update(seconds));
            }

            @DisplayName("Update moves servo backward")
            @Test
            void updateMotorBackward() {
                servo.setDirection(DcMotor.Direction.REVERSE);
                servo.setPower(1.0);

                // Seeing that the returned delta is as expected
                final double maxTickSpeed = -rpm / 60;
                final double seconds = 0.016;
                assertEquals(maxTickSpeed * seconds, servo.update(seconds));

                // Seeing that the returned delta is, in fact, a delta and not the accumulated
                assertNotEquals(3 * maxTickSpeed * seconds, servo.update(seconds * 2));

                // Making sure the delta is accurate with varying input
                assertEquals(1.5 * maxTickSpeed * seconds, servo.update(seconds * 1.5));

                // Making sure the delta is still accurate after a power change
                servo.setPower(-0.33);
                assertEquals(-0.33 * maxTickSpeed * seconds, servo.update(seconds));
            }
        
            @Disabled("This test checks for angular velocity offset, but it is resisted by the CRServo")
            @DisplayName("Add angular vel only ever adds (unless negative)")
            @Test
            void addVelAddsVelocotyCorrectly() {
                final double maxTickSpeed = rpm / 60;
                final double converson = 1 / (2 * Math.PI); // Radians to ticks

                // Positive Forward
                final double speed1 = 2 * Math.PI / 2;
                servo.setDirection(DcMotor.Direction.FORWARD);
                servo.setPower(1.0);
                servo.setAngularVelOffset(0);
                servo.addAngularVelOffset(speed1);
                assertEquals(maxTickSpeed + speed1 * converson, servo.update(1));
                
                // Negative Forward
                final double speed2 = -2 * Math.PI / 2;
                servo.setDirection(DcMotor.Direction.FORWARD);
                servo.setPower(1.0);
                servo.setAngularVelOffset(0);
                servo.addAngularVelOffset(speed2);
                assertEquals(maxTickSpeed + speed2 * converson, servo.update(1));
                
                // Positive Backward
                final double speed3 = 2 * Math.PI / 2;
                servo.setDirection(DcMotor.Direction.REVERSE);
                servo.setPower(1.0);
                servo.setAngularVelOffset(0);
                servo.addAngularVelOffset(speed3);
                assertEquals(-maxTickSpeed + speed3 * converson, servo.update(1));
                
                // Negative Backward
                final double speed4 = -2 * Math.PI / 2;
                servo.setDirection(DcMotor.Direction.REVERSE);
                servo.setPower(1.0);
                servo.setAngularVelOffset(0);
                servo.addAngularVelOffset(speed4);
                assertEquals(-maxTickSpeed + speed4 * converson, servo.update(1));
            }

            @Disabled("This tests obsolete behavior that has since been removed")
            @DisplayName("Add angular vel persists until setPower")
            @Test
            void verifyAddAnguarVelPersistence() {
                final double maxTickSpeed = rpm / 60;
                
                servo.setDirection(DcMotor.Direction.FORWARD);
                servo.setPower(1.0);
                final double originalDeltaTick = servo.update(1);
                assertEquals(maxTickSpeed, originalDeltaTick);

                servo.addAngularVelOffset(2 * Math.PI);
                final double firstTransformedDeltaTick = servo.update(1);
                assertNotEquals(maxTickSpeed, firstTransformedDeltaTick);
                assertEquals(firstTransformedDeltaTick, servo.update(1));

                servo.addAngularVelOffset(2 * Math.PI);
                final double secondTransformedDeltaTick = servo.update(1);
                assertNotEquals(firstTransformedDeltaTick, secondTransformedDeltaTick);
                assertEquals(secondTransformedDeltaTick, servo.update(1));

                servo.setPower(1.0);
                final double resetDeltaTick = servo.update(1);
                assertNotEquals(firstTransformedDeltaTick, resetDeltaTick);
                assertNotEquals(secondTransformedDeltaTick, resetDeltaTick);
                assertEquals(originalDeltaTick, resetDeltaTick);
                assertEquals(resetDeltaTick, servo.update(1));
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
                servo.setPower(0.5);
                final double unaffectedPowerDeltaTick = servo.update(1.0);

                servo.setPwmDisable();
                final double noPowerDiabledDelta = servo.update(1.0);
                servo.setPower(1.0);
                final double poweredDisabledDelta = servo.update(1.0);

                assertNotEquals(0, unaffectedPowerDeltaTick);
                assertNotEquals(unaffectedPowerDeltaTick, noPowerDiabledDelta);
                assertNotEquals(unaffectedPowerDeltaTick, poweredDisabledDelta);
                assertEquals(noPowerDiabledDelta, poweredDisabledDelta);

            }

            @DisplayName("Disabled servo affected by addAngularVelOffset")
            @Test 
            void disabledAffectedByaddAngularVelOffset() {
                // final SetVelocityAndPower nestedTested = new AddVelocity();
                // assumeTrue(!doesThrow(nestedTested::addAngularVelOffsetWithEncoder));

                servo.setPwmDisable();
                final double delatTick = servo.update(1.0); // Should be 0
                final double speed1 = 1;
                servo.addAngularVelOffset(speed1);
                assertEquals(delatTick + speed1 / (2 * Math.PI), servo.update(1.0));

                final double delatTick2 = servo.update(1.0);
                final double speed2 = -1;
                servo.addAngularVelOffset(speed2);
                assertEquals(delatTick2 + speed2 / (2 * Math.PI), servo.update(1.0));
            }
        
            @DisplayName("Renabled can set power")
            @Test
            void renabledStillWorks() {
                servo.setPower(1.0);

                // Assume it can be disabled in the frist place
                assumeFalse(doesThrow(this::disableDisablesSetPower));
                
                servo.setPwmEnable();
                assertTrue(servo.isPwmEnabled());
                servo.setPower(1.0);
                assertNotEquals(0, servo.update(1.0));
            }
        
            @DisplayName("Brake is stopped when servo is disabled")
            @Test
            void brakeIsStopped() {
                final AddVelocity nestedTested = new AddVelocity();
                assumeFalse(doesThrow(nestedTested::brakingPreventsMovement));
                servo.setPower(0.0);

                // Disabling...
                servo.setPwmDisable();
                final double delatTick = servo.update(1.0); // Should be 0
                final double speed1 = 1;
                servo.addAngularVelOffset(speed1);
                assertEquals(delatTick + speed1 / (2 * Math.PI), servo.update(1.0));

                final double delatTick2 = servo.update(1.0);
                final double speed2 = -1;
                servo.addAngularVelOffset(speed2);
                assertEquals(delatTick2 + speed2 / (2 * Math.PI), servo.update(1.0));
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
                // NOTE: actualSpeed flipping is used as a hack for symmetrical PWM ranges
                //       Reversed servos will setPower with the opposite extrema, so symmetrical 
                //       ranges simply flip the sign. For asymmetrical ranges, if statements are 
                //       used without the "actualSpeed".
                final double actualSpeed = servo.getDirection() == DcMotor.Direction.REVERSE
                    ? -maxSpeed
                    : maxSpeed;
                servo.setPower(-1.0);
                assertFloatEquals(-actualSpeed, servo.update(1.0), 1e-13);
                servo.setPower(1.0);
                assertFloatEquals(actualSpeed, servo.update(1.0), 1e-13);

                final double start1 = 1200;
                final double end1 = 1800;
                servo.setPwmRange(new PwmRange(start1, end1));
                servo.setPower(-1.0);
                System.out.println("Attempting...\n");
                assertFloatEquals(-0.6 * actualSpeed, servo.update(1), 1e-10);
                servo.setPower(1.0);
                System.out.println("Made it! 🥳\n");
                assertFloatEquals(0.6 * actualSpeed, servo.update(1), 1e-10);

                final double start2 = 1200;
                final double end2 = 1600;
                servo.setPwmRange(new PwmRange(start2, end2));
                servo.setPower(-1.0); // Asymmetry means that reversed servos are WIERD
                if(servo.getDirection() == DcMotor.Direction.REVERSE) {
                    assertFloatEquals(0.2 * maxSpeed, servo.update(1), 1e-10);
                } else {
                    assertFloatEquals(-0.6 * maxSpeed, servo.update(1), 1e-10);
                }
                servo.setPower(1.0); // Asymmetry means that reversed servos are WIERD
                if(servo.getDirection() == DcMotor.Direction.REVERSE) {
                    assertFloatEquals(-0.6 * maxSpeed, servo.update(1), 1e-10);
                } else {
                    assertFloatEquals(0.2 * maxSpeed, servo.update(1), 1e-10);
                }
                
                final double start4 = 1000;
                final double end4 = 2000;
                servo.setPwmRange(new PwmRange(start4, end4));
                servo.setPower(1.0);
                assertFloatEquals(1 * actualSpeed, servo.update(1), 1e-10);
                servo.setPower(-1.0);
                assertFloatEquals(-1 * actualSpeed, servo.update(1), 1e-10);
                
                final double start3 = 1500;
                final double end3 = 1500;
                servo.setPwmRange(new PwmRange(start3, end3));
                servo.setPower(1.0);
                assertFloatEquals(0 * actualSpeed, servo.update(1), 1e-10);
                servo.setPower(-1.0);
                assertFloatEquals(-0 * actualSpeed, servo.update(1), 1e-10);
                
            }

            @DisplayName("Setting PWM superset range clips the extrema")
            @Test
            void pwmSuperSetClipsRange() {
                // NOTE: actualSpeed flipping is used as a hack for symmetrical PWM ranges
                //       Reversed servos will setPower with the opposite extrema, so symmetrical 
                //       ranges simply flip the sign. For asymmetrical ranges, if statements are 
                //       used without the "actualSpeed".
                final double actualSpeed = servo.getDirection() == DcMotor.Direction.REVERSE
                    ? -maxSpeed
                    : maxSpeed;
                servo.setPower(-1.0);
                assertFloatEquals(-actualSpeed, servo.update(1.0), 1e-13);
                servo.setPower(1.0);
                assertFloatEquals(actualSpeed, servo.update(1.0), 1e-13);

                final double start1 = 800;
                final double end1 = 2200;
                servo.setPwmRange(new PwmRange(start1, end1));
                servo.setPower(-1.0);
                assertFloatEquals(-actualSpeed, servo.update(1.0), 1e-13);
                servo.setPower(1.0);
                assertFloatEquals(actualSpeed, servo.update(1.0), 1e-13);

                
                final double start2 = 800;
                final double end2 = 2600;
                servo.setPwmRange(new PwmRange(start2, end2));
                servo.setPower(-1.0);
                assertFloatEquals(-actualSpeed, servo.update(1.0), 1e-13);
                servo.setPower(1.0);
                assertFloatEquals(actualSpeed, servo.update(1.0), 1e-13);

                final double start3 = 0;
                final double end3 = 2100;
                servo.setPwmRange(new PwmRange(start3, end3));
                servo.setPower(-1.0);
                assertFloatEquals(-actualSpeed, servo.update(1.0), 1e-13);
                servo.setPower(1.0);
                assertFloatEquals(actualSpeed, servo.update(1.0), 1e-13);
            }
        }

        @DisplayName("Add Velocity Misc.")
        @Nested
        class AddVelocity {
            @DisplayName("Braking prevents being offset")
            @Test
            void brakingPreventsMovement() { 
                servo.setPower(0.0);
                final double deltaRev = servo.update(1);
                assertEquals(0, Math.abs(deltaRev));

                servo.addAngularVelOffset(-0.5 * maxSpeed * 2 * Math.PI);
                assertEquals(0, deltaRev + servo.update(1));
            }

            @DisplayName("Braking can be overpowered")
            @Test
            void brakingCanBeOverpowered() {
                servo.setPower(0.0);
                final double deltaRev = servo.update(1);
                assertEquals(0, Math.abs(deltaRev));

                servo.addAngularVelOffset(2 * maxSpeed * 2 * Math.PI);
                final double affectedDeltaRev = servo.update(1);
                assertNotEquals(deltaRev, affectedDeltaRev);
                assertEquals(maxSpeed, affectedDeltaRev);
            }

            @DisplayName("Braking can be overpowered by two small offsets")
            @Test
            void sumOfPartsOverpowering() {
                servo.setPower(0.0);
                final double deltaRev = servo.update(1);
                assertFloatEquals(0, Math.abs(deltaRev), 1e-10);

                final double fraction = 0.7;
                servo.addAngularVelOffset(fraction * maxSpeed * 2 * Math.PI);
                final double affectedDeltaRev = servo.update(1);
                assertFloatEquals(deltaRev, affectedDeltaRev, 1e-10);
                
                servo.addAngularVelOffset(fraction * maxSpeed * 2 * Math.PI);
                final double affectedDeltaRev2 = servo.update(1);
                assertNotEquals(deltaRev, Math.abs(affectedDeltaRev2));
                assertFloatEquals(maxSpeed * (2 * fraction - 1), affectedDeltaRev2, 1e-1);
            }
        }
    
    }

    
    @Nested
    class UpdaterDependent {
        private ModularUpdater updater = new ModularUpdater();
        ServoControllerExFake controller;
        CRServoImplExFake servo = null;

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
            servo = new CRServoImplExFake(new ContinuousServoData(rpm, 0, maxPwm), controller);
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

        @DisplayName("setPower updates automatically when registered with an updater")
        @Test
        void setPowerUpdatesAutomaticallyWhenRegistered() {
            servo.setPower(0.5);
            assertEquals(1, counter.getTotalUpdates());

            servo.setPower(0.75);
            assertEquals(2, counter.getTotalUpdates());

            System.out.println();
            final double[] pos = { 
                0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90,
                0.93, 0.94, 0.95, 0.96, 0.97, 0.99, 0.99, 1.00
            };
            for(int i = 2; i < 7; i++) {
                servo.setPower(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                servo.setPower(pos[2 + 2 * (i - 1)]);
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }

        @DisplayName("setPower delays for a different position when bulk caching is off")
        @Test
        void setPowerDelaysForDifferentPositionInOff() {
            servo.setPower(0.5);
            assertEquals(1, counter.getTotalUpdates());

            servo.setPower(0.5);
            assertEquals(1, counter.getTotalUpdates(), "Total updates did not change");

            servo.setPower(0.75);
            assertEquals(2, counter.getTotalUpdates());

            System.out.println();
            final double[] pos = { 
                0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90,
                0.93, 0.94, 0.95, 0.96, 0.97, 0.99, 0.99, 1.00
            };
            for(int i = 2; i < 7; i++) {
                servo.setPower(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());

                servo.setPower(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates(), "Total updates did not change");
                
                servo.setPower(pos[2 + 2 * (i - 1)]);
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("setPower delays the exact same even on MANUAL")
        @Test
        void setPowerDelaysForDifferentPositionInManual() {
            lynx.setBulkCachingMode(LynxModuleHardwareFake.BulkCachingMode.MANUAL);
            servo.setPower(0.5);
            assertEquals(1, counter.getTotalUpdates());

            servo.setPower(0.5);
            assertEquals(1, counter.getTotalUpdates(), "Total updates did not change");

            servo.setPower(0.75);
            assertEquals(2, counter.getTotalUpdates());

            System.out.println();
            final double[] pos = { 
                0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90,
                0.93, 0.94, 0.95, 0.96, 0.97, 0.99, 0.99, 1.00
            };
            for(int i = 2; i < 7; i++) {
                servo.setPower(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());

                servo.setPower(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates(), "Total updates did not change");
                
                servo.setPower(pos[2 + 2 * (i - 1)]);
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("setPower delays the exact same even on AUTO")
        @Test
        void setPowerDelaysForDifferentPositionInAuto() {
            lynx.setBulkCachingMode(LynxModuleHardwareFake.BulkCachingMode.AUTO);
            servo.setPower(0.5);
            assertEquals(1, counter.getTotalUpdates());

            servo.setPower(0.5);
            assertEquals(1, counter.getTotalUpdates(), "Total updates did not change");

            servo.setPower(0.75);
            assertEquals(2, counter.getTotalUpdates());

            System.out.println();
            final double[] pos = { 
                0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90,
                0.93, 0.94, 0.95, 0.96, 0.97, 0.99, 0.99, 1.00
            };
            for(int i = 2; i < 7; i++) {
                servo.setPower(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());

                servo.setPower(pos[1 + 2 * (i - 1)]);
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates(), "Total updates did not change");
                
                servo.setPower(pos[2 + 2 * (i - 1)]);
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getPower never simulates delay")
        @Test
        void getPowerUpdatesAutomaticallyWhenRegistered() {
            servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
            servo.getPower();
            assertEquals(0, counter.getTotalUpdates());

            servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
            servo.getPower();
            assertEquals(0, counter.getTotalUpdates());

            System.out.println();
            for(int i = 2; i < 7; i++) {
                servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
                servo.getPower();
                assertEquals(0, counter.getTotalUpdates());
                
                servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
                servo.getPower();
                assertEquals(0, counter.getTotalUpdates());
            }
        }

        @DisplayName("getPower never simulates delay, even in OFF")
        @Test
        void getPowerDelaysForDifferentPositionInOff() {
            servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
            servo.getPower();
            assertEquals(0, counter.getTotalUpdates());

            servo.getPower();
            assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");

            servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
            servo.getPower();
            assertEquals(0, counter.getTotalUpdates());

            System.out.println();
            for(int i = 2; i < 7; i++) {
                servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
                servo.getPower();
                assertEquals(0, counter.getTotalUpdates());

                servo.getPower();
                assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");
                
                servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
                servo.getPower();
                assertEquals(0, counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getPower never simulates delay, even in MANUAL")
        @Test
        void getPowerDelaysForDifferentPositionInManual() {
            lynx.setBulkCachingMode(LynxModuleHardwareFake.BulkCachingMode.MANUAL);
            servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
            servo.getPower();
            assertEquals(0, counter.getTotalUpdates());

            servo.getPower();
            assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");

            servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
            servo.getPower();
            assertEquals(0, counter.getTotalUpdates());

            System.out.println();
            for(int i = 2; i < 7; i++) {
                servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
                servo.getPower();
                assertEquals(0, counter.getTotalUpdates());

                servo.getPower();
                assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");
                
                servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
                servo.getPower();
                assertEquals(0, counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getPower never simulates delay, even in AUTO")
        @Test
        void getPowerDelaysForDifferentPositionInAuto() {
            lynx.setBulkCachingMode(LynxModuleHardwareFake.BulkCachingMode.AUTO);
            servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
            servo.getPower();
            assertEquals(0, counter.getTotalUpdates());

            servo.getPower();
            assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");

            servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
            servo.getPower();
            assertEquals(0, counter.getTotalUpdates());

            System.out.println();
            for(int i = 2; i < 7; i++) {
                servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
                servo.getPower();
                assertEquals(0, counter.getTotalUpdates());

                servo.getPower();
                assertEquals(0, counter.getTotalUpdates(), "Total updates did not change");
                
                servo.getData().position += 0.05; // Change position in case getPower() needs change to simulate delay
                servo.getPower();
                assertEquals(0, counter.getTotalUpdates());
            }
        }
        
        // TODO: Test PWM methods with an updater
    }

}