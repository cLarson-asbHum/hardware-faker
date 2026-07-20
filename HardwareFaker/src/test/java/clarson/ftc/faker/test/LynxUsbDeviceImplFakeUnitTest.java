/*
 * LynxUsbDeviceImplFakeUnitTest.java
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

import clarson.ftc.faker.AnalogInputFake;
import clarson.ftc.faker.DcMotorControllerExFake;
import clarson.ftc.faker.DcMotorImplExFake;
import clarson.ftc.faker.DigitalChannelControllerFake;
import clarson.ftc.faker.DigitalChannelImplFake;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.util.EasyTimedStateGetter;
import clarson.ftc.faker.util.UnsupportedLynxUsbCommandException;
import clarson.ftc.faker.wrapper.DigitalChannelData;
import clarson.ftc.faker.wrapper.MotorData;
import com.qualcomm.hardware.lynx.commands.core.LynxGetBulkInputDataCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetBulkInputDataResponse;
import com.qualcomm.hardware.lynx.commands.LynxCommand;
import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.LynxNackException;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;
import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier;
import java.util.function.DoubleSupplier;
import java.util.regex.Pattern;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static clarson.ftc.faker.test.TestUtil.*;
import static com.qualcomm.hardware.lynx.commands.standard.LynxNack.StandardReasonCode;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class LynxUsbDeviceImplFakeUnitTest {
    
    
    @DisplayName("Can construct")
    @Test
    void canConstruct() {
        assertDoesNotThrow(() -> new LynxUsbDeviceImplFake());   
    }

    @DisplayName("Can set Hardware Arrays")
    @Test
    void canSetHardwareArrays() {
        final LynxUsbDeviceImplFake lynx = new LynxUsbDeviceImplFake();
        assertDoesNotThrow(() -> {
            lynx.setMotors(new DcMotorImplExFake[] {
                new DcMotorImplExFake(312, 576.6),
                new DcMotorImplExFake(1600, 100, 123.4),
                new DcMotorImplExFake(312, -576.6, 1000),
                null
            });

            lynx.setDigitalChannels(new DigitalChannelImplFake[] {
                new DigitalChannelImplFake(),
                new DigitalChannelImplFake(() -> true),
                null,
                new DigitalChannelImplFake(false),
                null,
                null,
                new DigitalChannelImplFake(new EasyTimedStateGetter((deltaSec) -> deltaSec % 1 < 0.5)),
                null
            });
        });
    }

    @DisplayName("Can arm with no modules")
    @Test
    void canArm() {
        final LynxUsbDeviceImplFake lynx = new LynxUsbDeviceImplFake();
        assertDoesNotThrow(() -> lynx.armOrPretend());
        assertEquals(RobotArmingStateNotifier.ARMINGSTATE.ARMED, lynx.getArmingState());
    }

    @DisplayName("Construction Dependent")
    @Nested
    class PostConstruct {
        LynxUsbDeviceImplFake lynx;
        LynxModule module;
        DcMotorImplExFake[] motors = new DcMotorImplExFake[] {
            new DcMotorImplExFake(312, 576.6),
            null, // Check for a NullPointerException
            new DcMotorImplExFake(1600, 100, 123.4),
            new DcMotorImplExFake(312, -576.6, 1000)
        };

        DigitalChannelControllerFake digitalController = new DigitalChannelControllerFake(null);
        DigitalChannelImplFake[] channels = new DigitalChannelImplFake[8];
        AnalogInputFake[] analogs = {
            null,
            null,
            new AnalogInputFake(() -> Math.random()),
            new AnalogInputFake(new DoubleSupplier() {
                double i = 0;
                public double getAsDouble() {
                    i += 0.1415926536;
                    return i;
                }
            }),
        };

        @BeforeEach
        void createLynxUsbDevice() {
            lynx = new LynxUsbDeviceImplFake();

            channels = new DigitalChannelImplFake[] {
                new DigitalChannelImplFake(new DigitalChannelData(), digitalController, 0),
                new DigitalChannelImplFake(new DigitalChannelData(() -> true), digitalController, 1),
                null,
                new DigitalChannelImplFake(new DigitalChannelData(true), digitalController, 3),

                null,
                null,
                new DigitalChannelImplFake(new DigitalChannelData(new EasyTimedStateGetter((deltaSec) -> deltaSec % 1 < 0.5)), digitalController, 6),
                null
            };
            
            lynx.setMotors(motors);            
            lynx.setDigitalChannels(channels);
            lynx.setAnalogInputs(analogs);
        }

        @DisplayName("Create LynxModule") 
        @Nested
        class CreateModule {
            @Timeout(value = 1, unit = SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
            @Test
            void canCreateDescriptionBuilder() {
                assertDoesNotThrow(() -> {
                    new LynxModuleDescription.Builder(-1, true)
                        .setUserModule()
                        // .setSystemSynthetic()
                        .build();
                });
            }

            @Timeout(value = 1, unit = SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
            @Test
            void canAddLynxModule() {
                // fail("Exit tests");
                try {
                    module = lynx.getOrAddModule(
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
            }
        
            @Timeout(value = 1, unit = SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
            @DisplayName("Can arm with module")
            @Test
            void canArmWithModule() {
                final LynxUsbDeviceImplFake lynx = new LynxUsbDeviceImplFake();

                try {
                    module = lynx.getOrAddModule(
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

                assertDoesNotThrow(() -> lynx.armOrPretend());
                assertEquals(RobotArmingStateNotifier.ARMINGSTATE.ARMED, lynx.getArmingState());
            }
        }

        @DisplayName("Miscellaneous Transmit")
        @Nested 
        class MiscTransmit {
            @Timeout(value = 1, unit = SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
            @BeforeEach
            void addLynxModule() {
                // fail("Exit tests");
                try {
                    module = lynx.getOrAddModule(
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
            }

            @DisplayName("Responds with LynxNack on Unrecognized")
            @Test
            void nacksOnUnrecognzed() {
                // Getting the test command. The default response only avoids a NullPointerException
                final LynxMessage defaultResponse = new LynxMessage(module) {
                    @Override public boolean isDangerous() {
                        return false;
                    }

                    @Override public void fromPayloadByteArray(byte[] unused) {}

                    @Override public byte[] toPayloadByteArray() {
                        return null;
                    }

                    @Override public int getCommandNumber() {
                        return 0;
                    }
                };
            
                final LynxCommand<LynxMessage> unrecognized = new LynxCommand<>(module, defaultResponse) {
                    @Override public boolean isDangerous() {
                        return false;
                    }

                    @Override public void fromPayloadByteArray(byte[] unused) {}

                    @Override public byte[] toPayloadByteArray() {
                        return null;
                    }

                    @Override public int getCommandNumber() {
                        return 0;
                    }
                };
            
                // Assert that nothing has been received
                assertTrue(unrecognized.isAckable());
                assertFalse(unrecognized.isNackReceived());
                assertEquals(null, unrecognized.getNackReceived());

                // Making sure that the method doesn't misevaluate this message
                // It should only throw if it cannot be responded to
                assertDoesNotThrow(() -> {
                    lynx.transmit(unrecognized);
                });

                // Assert that a nack has been received and correct
                assertTrue(unrecognized.isNackReceived());
                assertNotEquals(null, unrecognized.getNackReceived());
                assertEquals(
                    StandardReasonCode.COMMAND_ROUTING_ERROR, 
                    unrecognized.getNackReceived().getNackReasonCodeAsEnum()
                );
            }

            @Disabled("Cannot yet craft a supported but unrespondable command")
            @DisplayName("Throws on Unrespondable LynxMessage")
            @Test
            void throwsOnUnrespondable() {
                // Making sure that we can actually do bulk gets and sends 
                final GetBulkData nestedTested = new GetBulkData();
                assumeFalse(doesThrow(nestedTested::getResponse));

                // Creating our command to transmit
                final LynxGetBulkInputDataCommand unrecognized = new LynxGetBulkInputDataCommand(module) {
                    // @Override
                    // public boolean isResponseExpected() {
                    //     return false;
                    // }
                };
            
                Exception e = assertThrowsExactly(UnsupportedLynxUsbCommandException.class, () -> {
                    lynx.transmit(unrecognized);
                });

                // Asserting that the message is the unrespondable message.
                // We test this by matching it against its String.format 
                // template, turned into a Regex 
                assertFalse(Pattern.matches(
                    "^" + LynxUsbDeviceImplFake.UN_NACKABLE_MSG.replace("%s", ".+?") + "$",
                    e.getMessage()
                ));
                assertTrue(Pattern.matches(
                    "^" + LynxUsbDeviceImplFake.UNRESPONDABLE_MSG.replace("%s", ".+?") + "$",
                    e.getMessage()
                ));
            }
            
            @DisplayName("Throws on Unnackable LynxMessage")
            @Test
            void throwsOnUnnackable() {
                final LynxMessage unrecognized = new LynxMessage(module) {
                    @Override
                    public boolean isDangerous() {
                        return false;
                    }

                    @Override 
                    public void fromPayloadByteArray(byte[] unused) {
                        // Do nothing.
                    }

                    @Override 
                    public byte[] toPayloadByteArray() {
                        return null;
                    }

                    @Override
                    public int getCommandNumber() {
                        return 0;
                    }
                };
            
                Exception e = assertThrowsExactly(UnsupportedLynxUsbCommandException.class, () -> {
                    lynx.transmit(unrecognized);
                });

                // Asserting that the message is the unnackable message.
                // We test this by matching it against its String.format 
                // template, turned into a Regex 
                assertTrue(Pattern.matches(
                    "^" + LynxUsbDeviceImplFake.UN_NACKABLE_MSG.replace("%s", ".+?") + "$",
                    e.getMessage()
                ));
                assertFalse(Pattern.matches(
                    "^" + LynxUsbDeviceImplFake.UNRESPONDABLE_MSG.replace("%s", ".+?") + "$",
                    e.getMessage()
                ));
            }
        } 
    
        @DisplayName("Get Bulk Data")
        @Nested
        class GetBulkData {
            @Timeout(value = 1, unit = SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
            @BeforeEach
            void addLynxModule() {
                // fail("Exit tests");
                try {
                    module = lynx.getOrAddModule(
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

                
                digitalController.setLynxModule((LynxModuleHardwareFake) module);
            }

            // NOTE: Hardware is added in the createLynxUsbDevice() method
            @DisplayName("Bulk Data Command Gets response")
            @Test
            void getResponse() throws InterruptedException, LynxNackException {
                final LynxGetBulkInputDataCommand command = new LynxGetBulkInputDataCommand(module);

                assertDoesNotThrow(() -> module.validateCommand(command));

                assertTrue(command.isResponseExpected());
                assertTrue(command.isAckable());
                assertFalse(command.isAckOrResponseReceived());

                final LynxGetBulkInputDataResponse response = command.sendReceive();

                // Make sure the response was recieved at the command level
                assertTrue(command.isAckOrResponseReceived());
                assertNotEquals(null, response);
            }

            private DcMotorControllerExFake getController(DcMotorImplExFake motor) {
                return (DcMotorControllerExFake) (motor.getController());
            }

            @DisplayName("Bulk Response is correct")
            @Test
            void isResponseCorrect() throws InterruptedException, LynxNackException {
                // Add some variation to the motors and channels
                motors[0].setVelocity(312);
                motors[2].setVelocity(628);
                motors[2].addAngularVelOffset(2 * Math.PI / 100 * 100); // 100 ticks / sec
                motors[3].setTargetPositionTolerance(2);
                motors[3].setTargetPosition(250);
                motors[3].setMode(DcMotorImplExFake.RunMode.RUN_TO_POSITION);

                // Get the command, verify, and response
                final LynxGetBulkInputDataCommand command = new LynxGetBulkInputDataCommand(module);

                assertDoesNotThrow(() -> module.validateCommand(command));

                assertTrue(command.isResponseExpected());
                assertTrue(command.isAckable());
                assertFalse(command.isAckOrResponseReceived());

                final LynxGetBulkInputDataResponse response = command.sendReceive();

                // Make sure the response was recieved at the command level
                assertTrue(command.isAckOrResponseReceived());
                assertNotEquals(null, response);

                //#region DEV START: Logging the payload as bytes
                // [00]  uint8_t  digitalInputs;      // DIGITAL_START

                // [01]  int32_t  motor0position_enc; // ENCODER_START
                // [05]  int32_t  motor1position_enc;
                // [09]  int32_t  motor2position_enc;
                // [13]  int32_t  motor3position_enc;

                // [17]  uint8_t  motorStatus;        // STATUS_START

                // [18]  int16_t  motor0velocity_cps; // VELOCITY_START
                // [20]  int16_t  motor1velocity_cps;
                // [22]  int16_t  motor2velocity_cps;
                // [24]  int16_t  motor3velocity_cps;

                // [26]  int16_t  analog0_mV;         // ANALOG_START
                // [28]  int16_t  analog1_mV;
                // [30]  int16_t  analog2_mV;
                // [32]  int16_t  analog3_mV;
                int j = 0;
                for(byte b : response.toPayloadByteArray()) {
                    System.out.println("[is correct] At <" + j + ">: 0x" + Integer.toHexString(b));
                    j++;
                }
                //#endregion DEV END

                // Make sure the data is correct
                // If it's null, we check that everything is zero
                for(int i = 0; i < motors.length; i++) {
                    final DcMotorImplExFake motor = motors[i];
                    final int portNumber = i; 

                    // Check that the data is zero if the motor is null
                    if(motor == null) {
                        System.out.println("[is correct] port: " + portNumber);
                        System.out.println("[is correct] current port is null");
                        
                        assertEquals(0, response.getEncoder(portNumber));
                        assertEquals(0, response.getVelocity(portNumber));
                        continue; // Don't do the rest of the stuff
                    }

                    // Only runs if the motor is non-null:
                    final MotorData data = getController(motor).getData(0);
                    System.out.println("[is correct] port: " + portNumber);
                    System.out.println("[is correct] getCur: " + motor.getCurrentPosition());
                    assertWithin(data.position, response.getEncoder(portNumber), 0.5);
                    assertWithin(data.getActualVelocity(), response.getVelocity(portNumber), 0.5);

                    if(motor.getMode() == DcMotorImplExFake.RunMode.RUN_TO_POSITION) {
                        assertEquals(
                            Math.abs(data.targetPosition - data.position) < data.tolerance, 
                            response.isAtTarget(portNumber)
                        );
                    }

                    // NOTE: Not testing the isOverCurrent as that relies on a flawed current reading
                }

                for(int i = 0; i < channels.length; i++) {
                    final DigitalChannelImplFake channel = channels[i];
                    final int portNumber = i; 

                    // Check that the data is zero if the channel is null
                    if(channel == null) {
                        System.out.println("[is correct] port: " + portNumber);
                        System.out.println("[is correct] current port is null");
                        
                        assertEquals(false, response.getDigitalInput(portNumber));
                        continue; // Don't do the rest of the stuff
                    }

                    // Only runs if the channel is non-null:
                    final DigitalChannelData data = 
                            ((DigitalChannelControllerFake) channel.getController()).getData(i);
                    System.out.println("[is correct] port: " + portNumber);
                    System.out.println("[is correct] state: " + channel.getState());
                    assertEquals(data.lastState, response.getDigitalInput(portNumber));

                    // NOTE: Not testing the isOverCurrent as that relies on a flawed current reading
                }

                for(int i = 0; i < analogs.length; i++) {
                    final AnalogInputFake analog = analogs[i];
                    final int portNumber = i; 

                    // Check that the data is zero if the analog is null
                    if(analog == null) {
                        System.out.println("[is correct] port: " + portNumber);
                        System.out.println("[is correct] current port is null");
                        
                        assertEquals(0, response.getAnalogInput(portNumber));
                        continue; // Don't do the rest of the stuff
                    }

                    // Only runs if the analog is non-null:
                    final double expectedMv = VoltageUnit.VOLTS.toMilliVolts(analog.getLastVoltage());
                    System.out.println("[is correct] port: " + portNumber);
                    System.out.println("[is correct] mV: " + expectedMv);
                    assertEquals((int) expectedMv, response.getAnalogInput(portNumber));

                    // NOTE: Not testing the isOverCurrent as that relies on a flawed current reading
                }
            }
        }

    }
}