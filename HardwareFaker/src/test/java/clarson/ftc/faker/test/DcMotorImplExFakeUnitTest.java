/*
 * DcMotorImplExFakeUnitTest.java
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

// import clarson.ftc.faker.DcMotorImplExFake;
import clarson.ftc.faker.DcMotorImplExFake;
import clarson.ftc.faker.DcMotorControllerExFake;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.util.AbstractTwoWayUpdateable;
import clarson.ftc.faker.wrapper.MotorData;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.Executable;

import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static clarson.ftc.faker.test.TestUtil.*; // Provides assertFloatEquals and doesThrow

import static java.util.concurrent.TimeUnit.SECONDS;

@DisplayName("DcMotorImplExFake")
class DcMotorImplExFakeUnitTest {
    @DisplayName("Can construct")
    @Test 
    void canConstruct() {
        assertDoesNotThrow(() -> new DcMotorImplExFake(312, 576.6));
        assertDoesNotThrow(() -> new DcMotorImplExFake(-1600, 100));
        assertDoesNotThrow(() -> new DcMotorImplExFake(-1600, 0));

        assertDoesNotThrow(() -> new DcMotorImplExFake(312, 100, 0));
        assertDoesNotThrow(() -> new DcMotorImplExFake(312, 100, 1200));
        assertDoesNotThrow(() -> new DcMotorImplExFake(312, 100, -1200));
        assertDoesNotThrow(() -> new DcMotorImplExFake(-312, 100, -1200));
    }

    @DisplayName("Initial settings are BRAKE, WITHOUT_ENCODER, FORWARD, and power 0")
    @Test 
    void checkInitialSettings() {
        final DcMotorEx motor = new DcMotorImplExFake(0, 0);
        assertEquals(DcMotor.RunMode.RUN_WITHOUT_ENCODER, motor.getMode());
        assertEquals(DcMotor.ZeroPowerBehavior.BRAKE, motor.getZeroPowerBehavior());
        assertEquals(DcMotor.Direction.FORWARD, motor.getDirection());
        assertFloatEquals(0, motor.getPower(), 1e-13);
        assertTrue(motor.isMotorEnabled());
    }

    @DisplayName("Construction Dependent")
    @ParameterizedClass
    @ValueSource(strings = { "FORWARD"  , "REVERSE"  })
    @Nested
    class ConstructionDependent {
        private DcMotorImplExFake motor;
        private double rpm = 312;
        private double ticksPerRev = 576.6;
        private double ticksPerSec = rpm / 60 * ticksPerRev;
        private double radsToTicks = ticksPerRev / (2 * Math.PI);
        
        @Parameter
        private String directionName;

        @BeforeEach 
        void constructMotor() {
            System.out.println("[constructMotor] direction: " + directionName);

            motor = new DcMotorImplExFake(rpm, ticksPerRev);
            if(directionName.equals("FORWARD")) {
                motor.setDirection(DcMotor.Direction.FORWARD);
            } else if(directionName.equals("REVERSE")) {
                motor.setDirection(DcMotor.Direction.REVERSE);
            }
            // motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        
        @DisplayName("DcMotorSimple Inherited")
        @Nested
        class DcMotorSimpleInherited {
            @DisplayName("Initial Power 0")
            @Test
            void initialPowerIsConstant() {
                assertFloatEquals(0, motor.getPower(), 1e-13); // Tolerance because -0.0 != 0.0, I guess
            }

            @DisplayName("Set Power Arg = Get Power")
            @Test
            void setPowerArgumentStored() {
                final double[] powers = { 0, 1.0, 0.0, -1.0, 0.5, 0.25, -0.5, -0.25 };
                for(final double power : powers) {
                    motor.setPower(power);
                    assertEquals(power, motor.getPower());
                }
            }

            @Disabled("This is covered by test DcMotorImplExFakeUnitTest.checkInitialSettings")
            @DisplayName("Initial Direction Forward")
            @Test 
            void initialDirectionForward() {
                assertEquals(DcMotor.Direction.FORWARD, motor.getDirection());
            }

            @DisplayName("Set Direction Arg = Get Direction") 
            @Test
            void setDirectionArgumentStored() {
                final DcMotor.Direction[] dirs = DcMotor.Direction.values();
                for(final DcMotor.Direction dir : dirs) {
                    motor.setDirection(dir);
                    assertEquals(dir, motor.getDirection());
                }
            }

            @DisplayName("Update moves motor forward")
            @Test
            void updateMotorForward() {
                motor.setDirection(DcMotor.Direction.FORWARD);
                motor.setPower(1.0);
                
                // Seeing that the returned delta is as expected
                final double maxTickSpeed = rpm * ticksPerRev / 60;
                final double seconds = 0.016;
                assertEquals(maxTickSpeed * seconds, motor.update(seconds));

                // Seeing that the returned delta is, in fact, a delta and not the accumulated
                assertNotEquals(3 * maxTickSpeed * seconds, motor.update(seconds * 2));

                // Making sure the delta is accurate with varying input
                assertEquals(1.5 * maxTickSpeed * seconds, motor.update(seconds * 1.5));

                // Making sure the delta is still accurate after a power change
                motor.setPower(-0.33);
                assertEquals(-0.33 * maxTickSpeed * seconds, motor.update(seconds));
            }

            @DisplayName("Update moves motor backward")
            @Test
            void updateMotorBackward() {
                motor.setDirection(DcMotor.Direction.REVERSE);
                motor.setPower(1.0);

                // Seeing that the returned delta is as expected
                final double maxTickSpeed = -rpm * ticksPerRev / 60;
                final double seconds = 0.016;
                assertEquals(maxTickSpeed * seconds, motor.update(seconds));

                // Seeing that the returned delta is, in fact, a delta and not the accumulated
                assertNotEquals(3 * maxTickSpeed * seconds, motor.update(seconds * 2));

                // Making sure the delta is accurate with varying input
                assertEquals(1.5 * maxTickSpeed * seconds, motor.update(seconds * 1.5));

                // Making sure the delta is still accurate after a power change
                motor.setPower(-0.33);
                assertEquals(-0.33 * maxTickSpeed * seconds, motor.update(seconds));
            }
        
            @DisplayName("Add angular vel only ever adds (unless negative)")
            @Test
            void addVelAddsVelocotyCorrectly() {
                final double maxTickSpeed = rpm / 60 * ticksPerRev;
                final double converson = ticksPerRev / (2 * Math.PI); // Radians to ticks

                // Positive Forward
                final double speed1 = 2 * Math.PI / 2;
                motor.setDirection(DcMotor.Direction.FORWARD);
                motor.setAngularVelOffset(0);
                motor.setPower(1.0);
                motor.addAngularVelOffset(speed1);
                assertEquals(maxTickSpeed + speed1 * converson, motor.update(1));
                
                // Negative Forward
                final double speed2 = -2 * Math.PI / 2;
                motor.setDirection(DcMotor.Direction.FORWARD);
                motor.setAngularVelOffset(0);
                motor.setPower(1.0);
                motor.addAngularVelOffset(speed2);
                assertEquals(maxTickSpeed + speed2 * converson, motor.update(1));
                
                // Positive Backward
                final double speed3 = 2 * Math.PI / 2;
                motor.setDirection(DcMotor.Direction.REVERSE);
                motor.setAngularVelOffset(0);
                motor.setPower(1.0);
                motor.addAngularVelOffset(speed3);
                assertEquals(-maxTickSpeed + speed3 * converson, motor.update(1));
                
                // Negative Backward
                final double speed4 = -2 * Math.PI / 2;
                motor.setDirection(DcMotor.Direction.REVERSE);
                motor.setAngularVelOffset(0);
                motor.setPower(1.0);
                motor.addAngularVelOffset(speed4);
                assertEquals(-maxTickSpeed + speed4 * converson, motor.update(1));
            }

            @Disabled("This test uses an obsolete feature of addAngularVel, which has been removed")
            @DisplayName("Add angular vel persists until setPower")
            @Test
            void verifyAddAnguarVelPersistence() {
                final double maxTickSpeed = rpm / 60 * ticksPerRev;
                
                motor.setDirection(DcMotor.Direction.FORWARD);
                motor.setPower(1.0);
                final double originalDeltaTick = motor.update(1);
                assertEquals(maxTickSpeed, originalDeltaTick);

                motor.addAngularVelOffset(2 * Math.PI);
                final double firstTransformedDeltaTick = motor.update(1);
                assertNotEquals(maxTickSpeed, firstTransformedDeltaTick);
                assertEquals(firstTransformedDeltaTick, motor.update(1));

                motor.addAngularVelOffset(2 * Math.PI);
                final double secondTransformedDeltaTick = motor.update(1);
                assertNotEquals(firstTransformedDeltaTick, secondTransformedDeltaTick);
                assertEquals(secondTransformedDeltaTick, motor.update(1));

                motor.setPower(1.0);
                final double resetDeltaTick = motor.update(1);
                assertNotEquals(firstTransformedDeltaTick, resetDeltaTick);
                assertNotEquals(secondTransformedDeltaTick, resetDeltaTick);
                assertEquals(originalDeltaTick, resetDeltaTick);
                assertEquals(resetDeltaTick, motor.update(1));
            }
        }
    
        @DisplayName("Current Alerts")
        @Nested
        class CurrentAlert {
            @DisplayName("Set Current Argument = Get Current")
            @Test
            void setCurrentAlterArgumentStored() {
                final double amps = 3.14;
                motor.setCurrentAlert(amps, CurrentUnit.AMPS);

                assertEquals(amps, motor.getCurrentAlert(CurrentUnit.AMPS));
                assertEquals(1000 * amps, motor.getCurrentAlert(CurrentUnit.MILLIAMPS));
                
                final double mamps = 3.14;
                motor.setCurrentAlert(mamps, CurrentUnit.MILLIAMPS);

                assertEquals(0.001 * amps, motor.getCurrentAlert(CurrentUnit.AMPS));
                assertEquals(mamps, motor.getCurrentAlert(CurrentUnit.MILLIAMPS));
            }

            @DisplayName("Current should alert when threshold < 0 ")
            @Test
            void currentAlertsWithNegativeThreshold() {
                motor.setCurrentAlert(-1, CurrentUnit.MILLIAMPS);
                motor.setPower(0);
                assertTrue(motor.isOverCurrent());
                motor.setPower(1);
                assertTrue(motor.isOverCurrent());
                motor.setPower(-1);
                assertTrue(motor.isOverCurrent());

                motor.setCurrentAlert(-124, CurrentUnit.AMPS);
                motor.setPower(0);
                assertTrue(motor.isOverCurrent());
                motor.setPower(1);
                assertTrue(motor.isOverCurrent());
                motor.setPower(-1);
                assertTrue(motor.isOverCurrent());
            }

            @DisplayName("Current alert activates only above when threshold >= 0")
            void nonNegativeCurrentAlertThreshold() {
                final double[] amps = { 0, 1, 2, 8, 13 };
                final double[] powers = {0, 1, -1, 0.333, -0.333};
                final boolean[][] results = {
                    { false,  true ,  true ,  true ,  true }, // 0 - 0
                    { false,  true ,  true ,  true ,  true }, // 1 - 0.076923
                    { false,  true ,  true ,  false,  false}, // 2 - 0.15384
                    { false,  true ,  true ,  false,  false}, // 8 - 0.61538
                    { false,  false,  false,  false,  false}  // 13
                };

                for(int ampIndex = 0; ampIndex < amps.length; ampIndex++) {
                    final double amp = amps[ampIndex];
                    for(int powerIndex = 0; powerIndex < powers.length; powerIndex++) {
                        final double power = powers[powerIndex];
                        motor.setPower(power);
                        assertEquals(results[ampIndex][powerIndex], motor.isOverCurrent());
                    }
                }
            }
        }

        @DisplayName("Set/Get Mode Checks")
        @Nested
        class BareBonesMode {
            @DisplayName("Set Mode = Get Mode")
            @Test
            void setModeArgumentStored() {
                final DcMotor.RunMode[] modes = { 
                    DcMotor.RunMode.RUN_WITHOUT_ENCODER, 
                    DcMotor.RunMode.RUN_USING_ENCODER, 
                    DcMotor.RunMode.RUN_TO_POSITION, 
                    DcMotor.RunMode.STOP_AND_RESET_ENCODER 
                };
                for(final DcMotor.RunMode mode : modes) {
                    motor.setTargetPosition(123456789); // Appeasing RUN_TO_POSITION to avoid exception
                    motor.setMode(mode);
                    assertEquals(mode, motor.getMode());
                }
            }

        }

        @DisplayName("Zero Power Behavior")
        @Nested
        class ZeroPowerBehavior {
            @DisplayName("Set ZPB = Get ZPB")
            @Test
            void setZeroPowerBehaviorArgumentStored() {
                for(final DcMotor.ZeroPowerBehavior behavior : DcMotor.ZeroPowerBehavior.values()) {
                    motor.setZeroPowerBehavior(behavior);
                    assertEquals(behavior, motor.getZeroPowerBehavior());
                }
            }

            @DisplayName("Get Power float true when behavior is FLOAT")
            @Test
            void getPowerFloatChecksGetBehavior() {
                motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                assertFalse(motor.getPowerFloat());

                motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
                assertTrue(motor.getPowerFloat());
            }
        }
    
        @DisplayName("Set Power and Velocity")
        @Nested
        class SetVelocityAndPower {
            @Disabled("A motor can read its encoder even if it doesn't *drive* with the encoder")
            @DisplayName("GetVelocity works only in encoder modes")
            @Test
            void getVelocity0WhenInRunWithoutEncoder() {
                motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motor.setPower(1.0);
                assertEquals(0, motor.getVelocity());
                
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setPower(1.0);
                if(motor.getDirection() == DcMotor.Direction.REVERSE) {
                    assertEquals(-1.0 * ticksPerSec, motor.getVelocity());
                } else {
                    assertEquals(1.0 * ticksPerSec, motor.getVelocity());
                }

                motor.setTargetPositionTolerance(0);
                motor.setTargetPosition(1000000); // Arbitrarily large number to ensure PIDF >= 1.0
                motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                motor.setPower(1.0);
                motor.update(0.1);
                
                System.out.println("[get velocity works] motor.getPower: " + motor.getPower()); 
                System.out.println("[get velocity works] motor.getTargetPosition: " + motor.getTargetPosition()); 
                System.out.println("[get velocity works] motor.getVelocity: " + motor.getVelocity()); 
                
                assertTrue(() -> motor.getVelocity() > 0);
            }

            @DisplayName("GetVelocity gets the velocity scaled appropriately")
            @Test
            void getVelocityWithUnit() {
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                final double tickVel = 10;
                motor.setVelocity(tickVel);

                assertEquals(tickVel / radsToTicks, motor.getVelocity(AngleUnit.RADIANS));
                assertEquals(tickVel / radsToTicks * 180 / Math.PI, motor.getVelocity(AngleUnit.DEGREES));
            }

            @Disabled("This is incorrect (shown so emperically)")
            @DisplayName("Sets velocity only in RUN_USING_ENCODER")
            @Test
            void velocitySetOnlyWithEncoder() {
                motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motor.setPower(1.0);
                final double oldVel1 = motor.update(1);
                motor.setVelocity(ticksPerSec * 0.5); // IF it worked, equivalent to setPower to 
                assertEquals(oldVel1, motor.update(1));

                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setPower(1.0);
                final double oldVel2 = motor.getVelocity();
                motor.setVelocity(ticksPerSec * 0.5);
                assertNotEquals(oldVel2, motor.getVelocity());
                assertEquals(ticksPerSec * 0.5, motor.getVelocity());
            }

            @DisplayName("Clamps setVelocity argument")
            @Test
            void velocityClampsArgument() {
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                final double vel1 = 2 * ticksPerSec; // Ticks per sec is the maximum velocity
                motor.setVelocity(vel1);
                assertNotEquals(ticksPerSec, vel1);
                assertEquals(ticksPerSec, motor.getVelocity());
                
                final double vel2 = -2 * ticksPerSec; // Ticks per sec is the maximum velocity
                motor.setVelocity(vel2);
                assertNotEquals(-ticksPerSec, vel2);
                assertEquals(-ticksPerSec, motor.getVelocity());

                final double vel3 = 0.5 * ticksPerSec;
                motor.setVelocity(vel3);
                assertEquals(vel3, motor.getVelocity());
            }

            @DisplayName("RUN_WITHOUT_ENCODER affected by addAngularVelOffset")
            @Test
            void addAngularVelOffsetWithoutEncoder() {
                motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motor.setPower(1.0);

                final double unaffectedVel = motor.update(1);
                final double angVel = 1;
                motor.addAngularVelOffset(angVel);
                assertEquals(unaffectedVel + angVel * radsToTicks, motor.update(1));
            }

            @DisplayName("RUN_USING_ENCODER keeps velocity when not overpowered")
            @Test
            void addAngularVelOffsetWithEncoder() {
                // Checking the condition with positive setPower
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setPower(0.5);
                final double unaffectedVel11 = motor.getVelocity();
                motor.addAngularVelOffset(-ticksPerSec * 0.25 / radsToTicks);
                assertEquals(unaffectedVel11, motor.getVelocity());
                
                motor.setPower(0.5);
                final double unaffectedVel21 = motor.getVelocity();
                motor.addAngularVelOffset(ticksPerSec * 0.25 / radsToTicks);
                assertEquals(unaffectedVel21, motor.getVelocity());

                // Checking the condition with negative setPower
                motor.setPower(-0.5);
                final double unaffectedVel31 = motor.getVelocity();
                motor.addAngularVelOffset(ticksPerSec * 0.25 / radsToTicks);
                assertEquals(unaffectedVel31, motor.getVelocity());

                motor.setPower(-0.5);
                final double unaffectedVel41 = motor.getVelocity();
                motor.addAngularVelOffset(-ticksPerSec * 0.25 / radsToTicks);
                assertEquals(unaffectedVel41, motor.getVelocity());
                
                // Checking the condition with positive setVelocity
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setVelocity(ticksPerSec * 0.5);
                final double unaffectedVel12 = motor.getVelocity();
                motor.addAngularVelOffset(-ticksPerSec * 0.25 / radsToTicks);
                assertEquals(unaffectedVel12, motor.getVelocity());
                
                motor.setVelocity(ticksPerSec * 0.5);
                final double unaffectedVel22 = motor.getVelocity();
                motor.addAngularVelOffset(ticksPerSec * 0.25 / radsToTicks);
                assertEquals(unaffectedVel22, motor.getVelocity());

                // Checking the condition with negative setVelocity
                motor.setVelocity(ticksPerSec * -0.5);
                final double unaffectedVel32 = motor.getVelocity();
                motor.addAngularVelOffset(ticksPerSec * 0.25 / radsToTicks);
                assertEquals(unaffectedVel32, motor.getVelocity());

                motor.setVelocity(ticksPerSec * -0.5);
                final double unaffectedVel42 = motor.getVelocity();
                motor.addAngularVelOffset(-ticksPerSec * 0.25 / radsToTicks);
                assertEquals(unaffectedVel42, motor.getVelocity());
                
            }
        
            private double raisedSignum(double x) {
                return x == 0.0 ? 1 : Math.signum(x);
            }

            @ParameterizedTest
            @ValueSource(doubles = { 0.0, 0.5, -0.5 })
            @DisplayName("RUN_USING_ENCODER power can be overpowerd by two small offsets")
            void sumOfPartsOverpoweringPower(double power) {
                double actualSpeed = ticksPerSec;

                if(motor.getDirection() == DcMotor.Direction.REVERSE) {
                    actualSpeed = -ticksPerSec;
                }

                // Using power
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setPower(power);
                final double deltaRev = motor.update(1);
                assertFloatEquals(power * actualSpeed, deltaRev, 1e-10);

                final double fraction = -0.75 * (1 - power); // Cannot overcome power on its own
                motor.addAngularVelOffset(fraction * actualSpeed / radsToTicks);
                final double affectedDeltaRev = motor.update(1);
                assertFloatEquals(deltaRev, affectedDeltaRev, 1e-10);
                
                motor.addAngularVelOffset(fraction * actualSpeed / radsToTicks);
                final double affectedDeltaRev2 = motor.update(1);
                assertNotEquals(deltaRev, affectedDeltaRev2);
                assertFloatEquals(
                    actualSpeed * (1 + 2 * fraction), 
                    affectedDeltaRev2, 
                    1e-10
                );
            }
            
            @ParameterizedTest
            @ValueSource(doubles = { 0.0, 0.5, -0.5 })
            @DisplayName("RUN_USING_ENCODER velocity can be overpowerd by two small offsets")
            void sumOfPartsOverpoweringVelocity(double power) {
                // Using power
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                if(motor.getDirection() == DcMotor.Direction.REVERSE) {
                    motor.setVelocity(-power * ticksPerSec);
                } else {
                    motor.setVelocity(power * ticksPerSec);
                }
                final double deltaRev = motor.update(1);
                assertFloatEquals(power * ticksPerSec, deltaRev, 1e-10);

                final double fraction = -0.75 * (1 - power); // Cannot overcome power on its own
                motor.addAngularVelOffset(fraction * ticksPerSec / radsToTicks);
                final double affectedDeltaRev = motor.update(1);
                assertFloatEquals(deltaRev, affectedDeltaRev, 1e-10);
                
                motor.addAngularVelOffset(fraction * ticksPerSec / radsToTicks);
                final double affectedDeltaRev2 = motor.update(1);
                assertNotEquals(deltaRev, affectedDeltaRev2);
                assertFloatEquals(
                    ticksPerSec * (1 + 2 * fraction), 
                    affectedDeltaRev2, 
                    1e-10
                );
            }
        }
    
        @DisplayName("Simple Position Manipulation")
        @Nested
        class GetCurrentPositionAndStopAndReset {
            @DisplayName("Current Position equals accumulated delta")
            @Test
            void currentPositionEqualsAccumulatedDelta() {
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setVelocity(210.9);

                final double timeStep = 1;
                double accumulated = 0;

                for(int i = 0; i < 10; i++) {
                    accumulated += motor.update(timeStep);
                    if(motor.getDirection() == DcMotor.Direction.REVERSE) { 
                        assertEquals((int) Math.round(accumulated), -motor.getCurrentPosition());
                    } else {
                        assertEquals((int) Math.round(accumulated), motor.getCurrentPosition());
                    }
                }
            }

            @DisplayName("STOP_AND_RESET_ENCODER resets the position")
            @Test
            void stopAndResetDoesWhatItSays() { // no way!!1! 😮
                // Getting the position waaaay up above 0
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setPower(1.0);
                motor.update(10000);
                final double oldPosition = motor.getCurrentPosition();
                motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                assertNotEquals(oldPosition, motor.getCurrentPosition());
                assertEquals(0, motor.getCurrentPosition());
                
                // Getting the position waaaay up below 0
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setPower(-1.0);
                motor.update(10000);
                final double oldPosition2 = motor.getCurrentPosition();
                motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                assertNotEquals(oldPosition2, motor.getCurrentPosition());
                assertEquals(0, motor.getCurrentPosition());
            }
        }
    
        @DisplayName("Run To Position")
        @Nested
        class RunToPosition {
            @DisplayName("Set Target = Get Target")
            @Test
            void setTargetPositionArgumentStored() {
                // No, i did NOT* just swipe my finger across the number keys!
                final int target = 123456098;
                motor.setTargetPosition(target);
                assertEquals(target, motor.getTargetPosition());
            }

            @DisplayName("RUN_TO_POSITION throws without target")
            @Test
            void runToPositionThrowsWithNoTarget() {
                assertThrows(RuntimeException.class, () -> {
                    motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                });
            }
            
            @DisplayName("RUN_TO_POSITION throws without target after run")
            @Test
            void runToPositionThrowsWithNoTargetAfterRun() {
                // Doing a run. We want to assume that the running is successful.
                assumeTrue(!doesThrow(this::reachesPosition));
                motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER); // The specific mode need only be not RUN_TO_POSITION

                // Testing that no target throws
                assertThrows(RuntimeException.class, () -> {
                    motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                });
            }

            @DisplayName("RUN_TO_POSITION reaches position on time")
            @Test
            void reachesPosition() {
                final int target = (int) Math.round(0.25 * ticksPerRev);

                // Making sure RUN_TO_POSITION reaches the specified target within 3 seconds.
                // This is done with a 30 millisecond update rate to simulate real life latency,
                // and to "reach the position", the current position must get within 5 ticks.
                final double maxSeconds = 3.0;
                final double updateDelaySeconds = 0.030; // 30 milliseconds
                final int tolerance = 5; // Ticks

                motor.setTargetPositionTolerance(tolerance);
                motor.setTargetPosition(target);
                motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                motor.setPower(1.0);

                double t = 0;
                while(
                    motor.isBusy() 
                    && Math.abs(motor.getCurrentPosition() - target) >= tolerance 
                    && t < maxSeconds
                ) {
                    assertEquals(DcMotor.RunMode.RUN_TO_POSITION, motor.getMode());
                    motor.update(updateDelaySeconds);
                    t += updateDelaySeconds;
                }

                System.out.println("[reaches on time] t: " + t);
                System.out.println("[reaches on time] currentPos: " + motor.getCurrentPosition());
                System.out.println("[reaches on time] target: " + target);
                System.out.println("[reaches on time] tolerance: " + tolerance);
                System.out.println("[reaches on time] isBusy: " + motor.isBusy());
                assertTrue(t < maxSeconds); // Making sure the loop didn't timeout.
                assertTrue(Math.abs(motor.getCurrentPosition() - target) < tolerance); // Did the motor *actually* reach the position?
            }
            
            
            @DisplayName("RUN_TO_POSITION reaches position on time with negative power")
            @Test
            void reachesPositionWithNegativePower() {
                final int target = (int) Math.round(0.25 * ticksPerRev);

                // Making sure RUN_TO_POSITION reaches the specified target within 3 seconds.
                // This is done with a 30 millisecond update rate to simulate real life latency,
                // and to "reach the position", the current position must get within 5 ticks.
                final double maxSeconds = 3.0;
                final double updateDelaySeconds = 0.030; // 30 milliseconds
                final int tolerance = 5; // Ticks

                motor.setTargetPositionTolerance(tolerance);
                motor.setTargetPosition(target);
                motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                motor.setPower(-1.0);

                double t = 0;
                while(
                    motor.isBusy() 
                    && Math.abs(motor.getCurrentPosition() - target) >= tolerance 
                    && t < maxSeconds
                ) {
                    assertEquals(DcMotor.RunMode.RUN_TO_POSITION, motor.getMode());
                    motor.update(updateDelaySeconds);
                    t += updateDelaySeconds;
                }

                System.out.println("[reaches on time negative] t: " + t);
                System.out.println("[reaches on time negative] currentPos: " + motor.getCurrentPosition());
                System.out.println("[reaches on time negative] target: " + target);
                System.out.println("[reaches on time negative] tolerance: " + tolerance);
                System.out.println("[reaches on time negative] isBusy: " + motor.isBusy());
                assertTrue(t < maxSeconds); // Making sure the loop didn't timeout.
                assertTrue(Math.abs(motor.getCurrentPosition() - target) < tolerance); // Did the motor *actually* reach the position?
            }
            

            @DisplayName("isBusy true when RUN_TO_POSITION active and not within position")
            @Test
            void isBusyTrueIfAndOnlyIf() {
                final int target = (int) Math.round(0.25 * ticksPerRev);

                // Making sure RUN_TO_POSITION reaches the specified target within 30 seconds.
                // This is done with a 30 millisecond update rate to simulate real life latency;
                // and to "reach the position", the current position must get within 5 ticks.
                final double maxSeconds = 30.0;
                final double updateDelaySeconds = 0.030; // 30 milliseconds
                final int tolerance = 5; // Ticks

                assertFalse(motor.isBusy());

                motor.setTargetPositionTolerance(tolerance);
                motor.setTargetPosition(target);
                motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                motor.setPower(1.0);

                double t = 0;
                while(
                    Math.abs(motor.getCurrentPosition() - target) >= tolerance 
                    && t < maxSeconds
                ) {
                    assertEquals(DcMotor.RunMode.RUN_TO_POSITION, motor.getMode());
                    assertTrue(motor.isBusy());
                    motor.update(updateDelaySeconds);
                    t += updateDelaySeconds;
                }
                
                System.out.println("[isBusy true when] t: " + t);
                System.out.println("[isBusy true when] currentPos: " + motor.getCurrentPosition());
                System.out.println("[isBusy true when] target: " + target);
                System.out.println("[isBusy true when] tolerance: " + tolerance);
                System.out.println("[isBusy true when] isBusy: " + motor.isBusy());

                assertTrue(t < maxSeconds); // Making sure the loop didn't timeout.
                assertFalse(motor.isBusy());

                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                assertFalse(motor.isBusy());
            }
        }
    
        @DisplayName("Enabling/Disabling")
        @Nested
        class EnableDisable {
            @DisplayName("Set Motor Enabled = Get Motor")
            @Test
            void setMotorEnabledArgumentStored() {
                motor.setMotorEnable();
                assertTrue(motor.isMotorEnabled());

                motor.setMotorDisable();
                assertFalse(motor.isMotorEnabled());
            }

            @DisplayName("Disabling motor prevents setting power/veloicty")
            @Test
            void disableDisablesSetPower() {
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setPower(1.0);
                final double unaffectedPowerDeltaTick = motor.update(1.0);
                motor.setVelocity(0.5 * ticksPerRev);
                final double unaffectedVelDeltaTick = motor.update(1.0);

                motor.setMotorDisable();
                final double noPowerDiabledDelta = motor.update(1.0);
                motor.setPower(1.0);
                final double poweredDisabledDelta = motor.update(1.0);
                motor.setVelocity(0.5 * ticksPerRev);
                final double velocityDisabledDelta = motor.update(1.0);

                assertNotEquals(0, unaffectedPowerDeltaTick);
                assertNotEquals(unaffectedPowerDeltaTick, noPowerDiabledDelta);
                assertNotEquals(unaffectedPowerDeltaTick, poweredDisabledDelta);
                // assertNotEquals(unaffectedVelDeltaTick, velocityDisabledDelta);
                assertEquals(noPowerDiabledDelta, poweredDisabledDelta);
                assertEquals(noPowerDiabledDelta, velocityDisabledDelta);
                assertEquals(poweredDisabledDelta, velocityDisabledDelta); // Should always pass... hopefully.

            }

            @DisplayName("Disabled motor affected by addAngularVelOffset")
            @Test 
            void disabledAffectedByAddAngularVel() {
                final SetVelocityAndPower nestedTested = new SetVelocityAndPower();
                assumeTrue(!doesThrow(nestedTested::addAngularVelOffsetWithEncoder));

                motor.setMotorDisable();
                final double delatTick = motor.update(1.0); // Should be 0
                final double speed1 = 1;
                motor.addAngularVelOffset(speed1);
                assertEquals(delatTick + speed1 * radsToTicks, motor.update(1.0));

                final double delatTick2 = motor.update(1.0);
                final double speed2 = -1;
                motor.addAngularVelOffset(speed2);
                assertEquals(delatTick2 + speed2 * radsToTicks, motor.update(1.0));
            }
        
            @DisplayName("Renabled can set power")
            @Test
            void renabledStillWorks() {
                motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motor.setPower(1.0);

                // Assume it can be disabled in the frist place
                assumeFalse(doesThrow(this::disableDisablesSetPower));
                
                motor.setMotorEnable();
                assertTrue(motor.isMotorEnabled());
                motor.setPower(1.0);
                assertNotEquals(0, motor.update(1.0));
            }
        }
    }

    
    @Nested
    class UpdaterDependent {
        private double rpm = 1000;
        private double ticksPerRev = 1000;

        private ModularUpdater updater = new ModularUpdater();
        private DcMotorImplExFake motor;
        private MotorData data;
        private DcMotorControllerExFake controller;

        private LynxUsbDeviceImplFake lynxUsb = new LynxUsbDeviceImplFake();
        private LynxModuleHardwareFake lynx = null;
   
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

        void construct() {
            // Creating the contoller
            lynx = createLynxModule(lynxUsb);
            controller = new DcMotorControllerExFake(lynx);
        

            // Constructing the motor and getting its data wrapper
            motor = new DcMotorImplExFake(new MotorData(rpm, ticksPerRev), controller);
            data = motor.getData();

            lynxUsb.setMotors(new DcMotorImplExFake[] {
                motor, null, null, null
            });
        }

        @BeforeEach
        void registerWithUpdater() {
            construct();
            
            if(controller.getLynxModule() == null) {
                fail("Controller LynxModule was null");
            }

            updater.register(motor, controller.getLynxModule());
            updater.register(counter, controller.getLynxModule());
        }

        @DisplayName("setUpdatingEnabled and isUpdatingEnabled match")
        @Test
        void setAndGetUpdatingEnabledMatch() {
            motor.setUpdatingEnabled(false);
            assertEquals(false, motor.isUpdatingEnabled());
            
            motor.setUpdatingEnabled(false);
            assertEquals(false, motor.isUpdatingEnabled());

            motor.setUpdatingEnabled(true);
            assertEquals(true, motor.isUpdatingEnabled());

            motor.setUpdatingEnabled(false);
            assertEquals(false, motor.isUpdatingEnabled());

            motor.setUpdatingEnabled(true);
            assertEquals(true, motor.isUpdatingEnabled());
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

        @DisplayName("update respects setUpdatingEnabled")
        @Test
        void updateRespectsSetUpdatingEnabled() {
            motor.setPower(1.0);
            assumeTrue(rpm * ticksPerRev / 60 == data.unaffectedVelocity, "setting the power actually updates the vel");

            motor.setUpdatingEnabled(true);
            motor.update(1.0);
            assertEquals(ticksPerRev * rpm / 60, data.position, "The position updated on true updatingEnabled");

            final double pos1 = data.position;
            motor.update(1.0);
            assertEquals(ticksPerRev * rpm / 60, data.position - pos1, "Updating enabled didn't change");

            motor.setUpdatingEnabled(true);
            motor.setUpdatingEnabled(true);
            motor.setUpdatingEnabled(true);
            motor.setUpdatingEnabled(true);
            final double pos2 = data.position;
            motor.update(1.0);
            assertFloatEquals(ticksPerRev * rpm / 60, data.position - pos2, 1e-10, "Repeating doesn't do anything funky");


            motor.setUpdatingEnabled(false);
            final double pos3 = data.position;
            motor.update(1.0);
            assertFloatEquals(pos3, data.position, 1e-10, "Switching to false disables position chang updating");
            assertTrue(0 < data.unaffectedVelocity, "Velocity is still positive");

            assertFloatEquals(pos3, data.position, 1e-10, "Not changing the status didn't change anything");
            assertTrue(0 < data.unaffectedVelocity, "Velocity is still positive");

            motor.setUpdatingEnabled(false);
            motor.setUpdatingEnabled(false);
            motor.setUpdatingEnabled(false);
            motor.setUpdatingEnabled(false);
            motor.update(1.0);
            assertFloatEquals(pos3, data.position, 1e-10, "Repeated disabling does nothing");
            assertTrue(0 < data.unaffectedVelocity, "Velocity is still positive");

            motor.setUpdatingEnabled(true);
            final double pos4 = data.position;
            motor.update(1.0);
            assertFloatEquals(ticksPerRev * rpm / 60, data.position - pos4, 1e-10, "Repeating doesn't do anything funky");

        }

        @DisplayName("forget removes updater internally; remember adds it back")
        @Test
        void forgetRemovesRememberAdds() {
            motor.getCurrentPosition();
            assertEquals(1, counter.getTotalUpdates(), "is registered initially");
            
            motor.forget(updater);
            motor.getCurrentPosition();
            assertEquals(1, counter.getTotalUpdates(), "updater no longer updated by motor");

            motor.getCurrentPosition();
            motor.getCurrentPosition();
            motor.getCurrentPosition();
            assertEquals(1, counter.getTotalUpdates(), "updater still no longer updated by motor");

            motor.remember(updater);
            motor.getCurrentPosition();
            assertEquals(2, counter.getTotalUpdates(), "updater now is updated by motor");
            
            motor.getCurrentPosition();
            motor.getCurrentPosition();
            motor.getCurrentPosition();
            assertEquals(5, counter.getTotalUpdates(), "updater still is updated by motor");
        }

        @DisplayName("Current Position equals accumulated delta on off")
        @Test
        void currentPositionEqualsAccumulatedDeltaOnOff() {
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.OFF);

            final double speed = 0.76 * rpm * ticksPerRev / 60; /* Ticks per sec */
            motor.getData().unaffectedVelocity = speed;

            double accumulated = 0;
            for(int i = 0; i < 10; i++) {
                lynx.clearBulkCache();
                accumulated += speed * Updater.UpdateDelaySource.MOTOR.length;
                System.out.println("[cur pos on off] accumulated: " + accumulated);
                // We multiply by 100 because the time step is small emought to round to zero
                assertEquals((int) Math.round(accumulated), motor.getCurrentPosition());
            }
        }

        @DisplayName("Current Position equals accumulated delta on manual (when cleared)")
        @Test
        void currentPositionEqualsAccumulatedDeltaOnManual() {
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

            final double speed = 0.76 * rpm * ticksPerRev / 60; /* Ticks per sec */
            motor.getData().unaffectedVelocity = speed;

            double accumulated = 0;
            for(int i = 0; i < 10; i++) {
                lynx.clearBulkCache();
                accumulated += speed * Updater.UpdateDelaySource.MOTOR.length;
                System.out.println("[cur pos on manual] accumulated: " + accumulated);
                // We multiply by 100 because the time step is small emought to round to zero
                assertEquals((int) Math.round(accumulated), motor.getCurrentPosition());
            }
        }
        @DisplayName("Current Position equals accumulated delta on automatic")
        @Test
        void currentPositionEqualsAccumulatedDeltaOnAuto() {
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);

            final double speed = 0.76 * rpm * ticksPerRev / 60; /* Ticks per sec */
            motor.getData().unaffectedVelocity = speed;

            double accumulated = 0;
            for(int i = 0; i < 10; i++) {
                accumulated += speed * Updater.UpdateDelaySource.MOTOR.length;
                System.out.println("[cur pos on auto] accumulated: " + accumulated);
                assertEquals((int) Math.round(accumulated), motor.getCurrentPosition());
            }
        }

        @DisplayName("getCurrentPosition updates automatically when registered with an updater")
        @Test
        void getCurrentPositionUpdatesAutomaticallyWhenRegistered() {
            // Number of ticks that progress per update (at power == 1.0):
            final double pStep = rpm * ticksPerRev / 60 * Updater.UpdateDelaySource.MOTOR.length;
            motor.setPower(1.0);

            counter.clearCount();
            assertEquals((int) Math.round(pStep), motor.getCurrentPosition(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());

            assertEquals((int) Math.round(2 * pStep), motor.getCurrentPosition(), "Value did change without needing an update");
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                assertEquals((int) Math.round(pStep * (1 + 2 * i - 2)), motor.getCurrentPosition(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals((int) Math.round(pStep * (2 + 2 * i - 2)), motor.getCurrentPosition(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }

        @DisplayName("getCurrentPosition delays always when bulk caching is off")
        @Test
        void getCurrentPositionDelaysWhenBulkCachingDisabled() {
            // Number of ticks that progress per update (at power == 1.0):
            final double pStep = rpm * ticksPerRev / 60 * Updater.UpdateDelaySource.MOTOR.length;
            motor.setPower(1.0);

            counter.clearCount();
            assertEquals((int) Math.round(pStep), motor.getCurrentPosition(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());

            assertEquals((int) Math.round(2 * pStep), motor.getCurrentPosition(), "Value did change without needing an update");
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                assertEquals((int) Math.round(pStep * (1 + 2 * i - 2)), motor.getCurrentPosition(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals((int) Math.round(pStep * (2 + 2 * i - 2)), motor.getCurrentPosition(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getCurrentPosition delays (the correct time) only on clear cache in MANUAL")
        @Test
        void getCurrentPositionDelaysOnlyWhenClearManual() {
            // Number of ticks that progress per update (at power == 1.0):
            final double pStep = rpm * ticksPerRev / 60 * Updater.UpdateDelaySource.MOTOR.length;
            motor.setPower(1.0);

            System.out.println("=============================");
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

            counter.clearCount();
            assertEquals((int) Math.round(pStep), motor.getCurrentPosition(), "Expected initial value is correct");
            System.out.println("=============================");
            assertEquals(1, counter.getTotalUpdates());

            assertEquals((int) Math.round(pStep), motor.getCurrentPosition(), "Expected initial value didnt change");
            assertEquals(1, counter.getTotalUpdates());

            lynx.clearBulkCache();
            final double secondState = motor.getCurrentPosition();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals((int) Math.round(2 * pStep), secondState, "Value did change after clearing cache");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                lynx.clearBulkCache();
                assertEquals((int) Math.round(pStep * (1 + 2 * i - 2)), motor.getCurrentPosition(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals((int) Math.round(pStep * (1 + 2 * i - 2)), motor.getCurrentPosition(), i + "-th value did not change");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                lynx.clearBulkCache();
                assertEquals((int) Math.round(pStep * (2 + 2 * i - 2)), motor.getCurrentPosition(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getCurrentPosition delays only on repeat invokation in AUTOMATIC")
        @Test
        void getCurrentPositionDelaysOnlyWhenRepeatedInAuto() {
            final DcMotorImplExFake motor2 = new DcMotorImplExFake(
                new MotorData(rpm, ticksPerRev, 0),
                controller
            );
            
            updater.register(motor2, lynx);
            lynxUsb.setMotors(new DcMotorImplExFake[] { motor, motor2, null, null });
            
            // Number of ticks that progress per update (at power == 1.0):
            final double pStep = rpm * ticksPerRev / 60 * Updater.UpdateDelaySource.MOTOR.length;
            motor.setPower(1.0);
            motor2.setPower(1.0);
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);

            // Reseting the counts and positions because previous methods may have accidnetally changed them
            counter.clearCount();
            motor.getData().position = 0;
            motor2.getData().position = 0;

            // Performing the tests
            System.out.println("*****************************");
            assertEquals((int) Math.round(pStep), motor.getCurrentPosition(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());
            
            final double secondState = motor2.getCurrentPosition();
            assertEquals(1, counter.getTotalUpdates());
            assertEquals((int) Math.round(pStep), secondState, "Value not did change after using other method");

            final double thirdState = motor.getCurrentPosition();
            System.out.println("*****************************");
            assertEquals(2, counter.getTotalUpdates());
            assertEquals((int) Math.round(2 * pStep), thirdState, "Value did change after repeating");

            final double fourthState = motor2.getCurrentPosition();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals((int) Math.round(2 * pStep), fourthState, "Value not did change after using other method");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                assertEquals((int) Math.round(pStep * (1 + 2 * (i - 1))), motor.getCurrentPosition(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals((int) Math.round(pStep * (1 + 2 * (i - 1))), motor2.getCurrentPosition(), i + "-th value did not change");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals((int) Math.round(pStep * (2 + 2 * (i - 1))), motor.getCurrentPosition(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
    
        @DisplayName("getVelocity is accurately represented in all Bulk Cache modes")
        @Test
        void getVelocityAccuratelyRepresentedOnAllModes() {
            final double[] vels = { 210.9, 0.0, -314.2 };
            for(final double vel : vels) {
                motor.setVelocity(vel);
            
                // Doing the tests
                // NOTE: Because bulk caching stores the vels as shorts, OFF is the only one which is a double.
                lynx.setBulkCachingMode(LynxModule.BulkCachingMode.OFF);
                assertEquals(vel,                               motor.getVelocity(), "Velocity is correct on OOF"); // intentional typo ????
                assertEquals(vel / ticksPerRev * 360,           motor.getVelocity(AngleUnit.DEGREES), "Velocity is correct on OOF");
                assertEquals(vel / ticksPerRev * (2 * Math.PI), motor.getVelocity(AngleUnit.RADIANS), "Velocity is correct on OOF");

                lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
                lynx.clearBulkCache();
                assertEquals(Math.round(vel),                               motor.getVelocity(), "Velocity is correct on MANUAL");
                assertEquals(Math.round(vel) / ticksPerRev * 360,           motor.getVelocity(AngleUnit.DEGREES), "Velocity is correct on MANUAL");
                assertEquals(Math.round(vel) / ticksPerRev * (2 * Math.PI), motor.getVelocity(AngleUnit.RADIANS), "Velocity is correct on MANUAL");

                lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
                lynx.clearBulkCache();
                assertEquals(Math.round(vel),                               motor.getVelocity(), "Velocity is correct on AUTO");
                assertEquals(Math.round(vel) / ticksPerRev * 360,           motor.getVelocity(AngleUnit.DEGREES), "Velocity is correct on AUTO");
                assertEquals(Math.round(vel) / ticksPerRev * (2 * Math.PI), motor.getVelocity(AngleUnit.RADIANS), "Velocity is correct on AUTO");

            }
        }
        
        @DisplayName("getVelocity updates automatically when registered with an updater")
        @Test
        void getVelocityUpdatesAutomaticallyWhenRegistered() {
            // Number of ticks that progress per update (at power == 1.0):
            motor.setPower(1.0);

            counter.clearCount();
            motor.getVelocity();
            assertEquals(1, counter.getTotalUpdates());

            motor.getVelocity();
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                motor.getVelocity();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());
                
                motor.getVelocity(AngleUnit.DEGREES);
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());

                motor.getVelocity(AngleUnit.RADIANS);
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());
            }
        }

        @DisplayName("getVelocity delays always when bulk caching is off")
        @Test
        void getVelocityDelaysWhenBulkCachingDisabled() {
            // Number of ticks that progress per update (at power == 1.0):
            motor.setPower(1.0);

            counter.clearCount();
            motor.getVelocity();
            assertEquals(1, counter.getTotalUpdates());

            motor.getVelocity(); // Should always be ticks / sec
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                
                motor.getVelocity();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());
                
                motor.getVelocity(AngleUnit.DEGREES);
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());

                motor.getVelocity(AngleUnit.RADIANS);
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getVelocity delays (the correct time) only on clear cache in MANUAL")
        @Test
        void getVelocityDelaysOnlyWhenClearManual() {
            motor.setPower(1.0);
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

            // Reseting the counts and positions because previous methods may have accidnetally changed them
            counter.clearCount();

            // Performing the tests
            motor.getVelocity();
            assertEquals(1, counter.getTotalUpdates());
            
            motor.getVelocity();
            assertEquals(1, counter.getTotalUpdates());

            lynx.clearBulkCache();
            motor.getVelocity();
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                lynx.clearBulkCache();
                motor.getVelocity();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());
                
                lynx.clearBulkCache();
                motor.getVelocity(AngleUnit.DEGREES);
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());
                
                motor.getVelocity(AngleUnit.RADIANS);
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());

                lynx.clearBulkCache();
                motor.getVelocity(AngleUnit.RADIANS);
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getVelocity delays only on repeat invokation in AUTOMATIC")
        @Test
        void getVelocityDelaysOnlyWhenRepeatedInAuto() {
            final DcMotorImplExFake motor2 = new DcMotorImplExFake(
                new MotorData(rpm, ticksPerRev, 0),
                controller
            );
            
            updater.register(motor2, lynx);
            lynxUsb.setMotors(new DcMotorImplExFake[] { motor, motor2, null, null });
            
            // Number of ticks that progress per update (at power == 1.0):
            motor.setPower(1.0);
            motor2.setPower(1.0);
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);

            // Reseting the counts and positions because previous methods may have accidnetally changed them
            counter.clearCount();
            motor.getData().position = 0;
            motor2.getData().position = 0;

            // Performing the tests
            motor.getVelocity();
            assertEquals(1, counter.getTotalUpdates());
            
            motor2.getVelocity();
            assertEquals(1, counter.getTotalUpdates());

            // ??? An automatic update occurred
            motor.getVelocity();
            assertEquals(2, counter.getTotalUpdates());

            motor2.getVelocity();
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                // ??? An automatic update occurred
                motor.getVelocity();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());

                motor2.getVelocity();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());
                
                // ??? An automatic update occurred
                motor.getVelocity(AngleUnit.DEGREES);
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());
                
                motor2.getVelocity(AngleUnit.DEGREES);
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());

                // ??? An automatic update occurred
                motor.getVelocity(AngleUnit.RADIANS);
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());

                motor2.getVelocity(AngleUnit.RADIANS);
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());
            }
        }
    
        @Timeout(value = 3, unit = SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
        @DisplayName("isBusy is read correctly on OFF")
        @Test
        void isBusyFunctionsAutomaticallyOnOff() {
            final int target = (int) Math.round(25 * ticksPerRev);

            // Making sure RUN_TO_POSITION reaches the specified target within 30 seconds.
            // This is done with a 30 millisecond update rate to simulate real life latency;
            // and to "reach the position", the current position must get within 5 ticks.
            final double maxSeconds = 30.0;
            final double updateDelaySeconds = Updater.UpdateDelaySource.MOTOR.length;
            final int tolerance = 5; // Ticks

            assertFalse(motor.isBusy());

            motor.setTargetPositionTolerance(tolerance);
            motor.setTargetPosition(target);
            motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motor.setPower(1.0);
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.OFF);

            double t = 0;
            while(
                Math.abs(motor.getData().position - target) >= tolerance 
                && t < maxSeconds
            ) {
                // assertEquals(DcMotor.RunMode.RUN_TO_POSITION, motor.getMode());
                // lynx.clearBulkCache();
                // System.out.println("[isBusy on manual] t: " + t);

                /*
                 *  Because the value of isBusy comes *after* the automatic update, 
                 *  the motor can move into tolerance of the target, and thus it could 
                 *  reasonably return false inside this iteration.
                 *  
                 *  Thus, we check that if it does return false, that it is returning false 
                 *  because the motor is within target. In other words:
                 *  
                 *  - If isBusy() returns true, continue the loop.
                 *  - If isBusy() returns false, assert that the motor is within tolerance 
                 *      of the target  
                 */
                final boolean isCurrentlyBusy = motor.isBusy();
                if(!isCurrentlyBusy) {
                    // Verify that it is returning false because it genuinely is no longer busy.
                    assertFalse(Math.abs(motor.getData().position - target) >= tolerance);
                } else {
                    // True is expected; continue without error.
                }
                t += updateDelaySeconds;
            }
            
            System.out.println("[isBusy on OFF] t: " + t);
            System.out.println("[isBusy on OFF] currentPos: " + motor.getCurrentPosition());
            System.out.println("[isBusy on OFF] target: " + target);
            System.out.println("[isBusy on OFF] tolerance: " + tolerance);
            System.out.println("[isBusy on OFF] isBusy: " + motor.isBusy());

            assertTrue(t < maxSeconds); // Making sure the loop didn't timeout.
            assertFalse(motor.isBusy());

            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            assertFalse(motor.isBusy());
        }
    
        @Timeout(value = 3, unit = SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
        @DisplayName("isBusy is read correctly on MANUAL")
        @Test
        void isBusyFunctionsAutomaticallyOnManual() {
            final int target = (int) Math.round(2.5 * ticksPerRev);

            // Making sure RUN_TO_POSITION reaches the specified target within 30 seconds.
            // This is done with a 30 millisecond update rate to simulate real life latency;
            // and to "reach the position", the current position must get within 5 ticks.
            final double maxSeconds = 5;
            final double updateDelaySeconds = Updater.UpdateDelaySource.MOTOR.length;
            final int tolerance = 5; // Ticks

            assertFalse(motor.isBusy());

            motor.setTargetPositionTolerance(tolerance);
            motor.setTargetPosition(target);
            motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motor.setPower(1.0);
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

            counter.clearCount();
            double t = 0;
            while(
                Math.abs(motor.getData().position - target) >= tolerance 
                && t < maxSeconds
            ) {
                // System.out.println("[isBusy on MANUAL] t: " + t);
                // System.out.println("[isBusy on MANUAL] counter calls: " + counter.getTotalCalls());
                // System.out.println("[isBusy on MANUAL] pos: " + motor.getData().position);
                
                /*
                 *  Because the value of isBusy comes *after* the automatic update, 
                 *  the motor can move into tolerance of the target, and thus it could 
                 *  reasonably return false inside this iteration.
                 *  
                 *  Thus, we check that if it does return false, that it is returning false 
                 *  because the motor is within target. In other words:
                 *  
                 *  - If isBusy() returns true, continue the loop.
                 *  - If isBusy() returns false, assert that the motor is within tolerance 
                 *      of the target  
                 */
                lynx.clearBulkCache();
                final boolean isCurrentlyBusy = motor.isBusy();
                if(!isCurrentlyBusy) {
                    // Verify that it is returning false because it genuinely is no longer busy.
                    assertFalse(Math.abs(motor.getData().position - target) >= tolerance);
                } else {
                    // True is expected; continue without error.
                }
                t += updateDelaySeconds;
            }
            
            System.out.println("[isBusy on MANUAL] t: " + t);
            System.out.println("[isBusy on MANUAL] currentPos: " + motor.getCurrentPosition());
            System.out.println("[isBusy on MANUAL] target: " + target);
            System.out.println("[isBusy on MANUAL] tolerance: " + tolerance);
            System.out.println("[isBusy on MANUAL] isBusy: " + motor.isBusy());

            lynx.clearBulkCache();
            assertTrue(t < maxSeconds); // Making sure the loop didn't timeout.
            assertFalse(motor.isBusy());

            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            assertFalse(motor.isBusy());
        }
    
        @Timeout(value = 3, unit = SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
        @DisplayName("isBusy is read correctly on AUTO")
        @Test
        void isBusyFunctionsAutomaticallyOnAuto() {
            final int target = (int) Math.round(2.5 * ticksPerRev);

            // Making sure RUN_TO_POSITION reaches the specified target within 30 seconds.
            // This is done with a 30 millisecond update rate to simulate real life latency;
            // and to "reach the position", the current position must get within 5 ticks.
            final double maxSeconds = 5;
            final double updateDelaySeconds = Updater.UpdateDelaySource.MOTOR.length;
            final int tolerance = 5; // Ticks

            assertFalse(motor.isBusy());

            motor.setTargetPositionTolerance(tolerance);
            motor.setTargetPosition(target);
            motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motor.setPower(1.0);
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);

            double t = 0;
            while(
                Math.abs(motor.getData().position - target) >= tolerance 
                && t < maxSeconds
            ) {
                /*
                 *  Because the value of isBusy comes *after* the automatic update, 
                 *  the motor can move into tolerance of the target, and thus it could 
                 *  reasonably return false inside this iteration.
                 *  
                 *  Thus, we check that if it does return false, that it is returning false 
                 *  because the motor is within target. In other words:
                 *  
                 *  - If isBusy() returns true, continue the loop.
                 *  - If isBusy() returns false, assert that the motor is within tolerance 
                 *      of the target  
                 */
                // lynx.clearBulkCache();
                final boolean isCurrentlyBusy = motor.isBusy();
                if(!isCurrentlyBusy) {
                    // Verify that it is returning false because it genuinely is no longer busy.
                    assertFalse(Math.abs(motor.getData().position - target) >= tolerance);
                } else {
                    // True is expected; continue without error.
                }
                t += updateDelaySeconds;
            }
            
            System.out.println("[isBusy on AUTO] t: " + t);
            System.out.println("[isBusy on AUTO] currentPos: " + motor.getCurrentPosition());
            System.out.println("[isBusy on AUTO] target: " + target);
            System.out.println("[isBusy on AUTO] tolerance: " + tolerance);
            System.out.println("[isBusy on AUTO] isBusy: " + motor.isBusy());

            lynx.clearBulkCache();
            assertTrue(t < maxSeconds); // Making sure the loop didn't timeout.
            assertFalse(motor.isBusy());

            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            assertFalse(motor.isBusy());
        }
        
        @DisplayName("isBusy delays always when bulk caching is off")
        @Test
        void isBusyDelaysWhenBulkCachingDisabled() {
            // Number of ticks that progress per update (at power == 1.0):
            motor.setPower(1.0);

            counter.clearCount();
            motor.isBusy();
            assertEquals(1, counter.getTotalUpdates());

            motor.isBusy(); // Should always be ticks / sec
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                
                motor.isBusy();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());
                
                motor.isBusy();
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());

                motor.isBusy();
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("isBusy delays (the correct time) only on clear cache in MANUAL")
        @Test
        void isBusyDelaysOnlyWhenClearManual() {
            motor.setPower(1.0);
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

            // Reseting the counts and positions because previous methods may have accidnetally changed them
            counter.clearCount();

            // Performing the tests
            motor.isBusy();
            assertEquals(1, counter.getTotalUpdates());
            
            motor.isBusy();
            assertEquals(1, counter.getTotalUpdates());

            lynx.clearBulkCache();
            motor.isBusy();
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                lynx.clearBulkCache();
                motor.isBusy();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());
                
                lynx.clearBulkCache();
                motor.isBusy();
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());
                
                motor.isBusy();
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());

                lynx.clearBulkCache();
                motor.isBusy();
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("isBusy delays only on repeat invokation in AUTOMATIC")
        @Test
        void isBusyDelaysOnlyWhenRepeatedInAuto() {
            final DcMotorImplExFake motor2 = new DcMotorImplExFake(
                new MotorData(rpm, ticksPerRev, 0),
                controller
            );
            
            updater.register(motor2, lynx);
            lynxUsb.setMotors(new DcMotorImplExFake[] { motor, motor2, null, null });
            
            // Number of ticks that progress per update (at power == 1.0):
            motor.setPower(1.0);
            motor2.setPower(1.0);
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);

            // Reseting the counts and positions because previous methods may have accidnetally changed them
            counter.clearCount();
            motor.getData().position = 0;
            motor2.getData().position = 0;

            // Performing the tests
            motor.isBusy();
            assertEquals(1, counter.getTotalUpdates());
            
            motor2.isBusy();
            assertEquals(1, counter.getTotalUpdates());

            // ??? An automatic update occurred
            motor.isBusy();
            assertEquals(2, counter.getTotalUpdates());

            motor2.isBusy();
            assertEquals(2, counter.getTotalUpdates());

            for(int i = 2; i < 7; i++) {
                // ??? An automatic update occurred
                motor.isBusy();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());

                motor2.isBusy();
                assertEquals(3 + 3 * (i - 2), counter.getTotalUpdates());
                
                // ??? An automatic update occurred
                motor.isBusy();
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());
                
                motor2.isBusy();
                assertEquals(4 + 3 * (i - 2), counter.getTotalUpdates());

                // ??? An automatic update occurred
                motor.isBusy();
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());

                motor2.isBusy();
                assertEquals(5 + 3 * (i - 2), counter.getTotalUpdates());
            }
        }
    
    }
}