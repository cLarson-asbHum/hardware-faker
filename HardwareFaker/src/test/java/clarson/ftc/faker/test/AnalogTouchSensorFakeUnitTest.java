/*
 * AnalogTouchSensorFakeUnitTest.java
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

import clarson.ftc.faker.AnalogInputControllerFake;
import clarson.ftc.faker.AnalogInputFake;
import clarson.ftc.faker.AnalogTouchSensorFake;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.function.TimedDoubleSupplier;
import clarson.ftc.faker.util.EasyTimedDoubleSupplier;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.AbstractTwoWayUpdateable;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;
import com.qualcomm.robotcore.exception.RobotCoreException;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;
import static clarson.ftc.faker.test.TestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class AnalogTouchSensorFakeUnitTest {
    static final double TH = 0.1; // Touch threshold 

    @DisplayName("Can construct")
    @Test
    void canConstruct() {
        assertDoesNotThrow(() -> new AnalogTouchSensorFake(() -> 0, TH));
        assertDoesNotThrow(() -> new AnalogTouchSensorFake(() -> 1.0, TH));
        assertDoesNotThrow(() -> new AnalogTouchSensorFake(() -> Math.random(), TH));
    }

    static enum InputFormula {
        FALSE,
        TRUE,
        IS_EVEN_CALL,
        IS_EVEN_CALL_TIME,

        SIN,
        SIN_TIME
    }

    TimedDoubleSupplier getSupplier(InputFormula inputFormula) {
        switch(inputFormula) {
            case FALSE: return new EasyTimedDoubleSupplier(() -> 0.0);

            case TRUE: return new EasyTimedDoubleSupplier(() -> 1.0);

            case IS_EVEN_CALL: return new EasyTimedDoubleSupplier(new DoubleSupplier() {
                double v = 0;
                @Override public double getAsDouble() {
                    v += 0.1415;
                    return (v);
                }
            });

            case IS_EVEN_CALL_TIME: return new EasyTimedDoubleSupplier(new DoubleFunction<Double>() {
                double elapsed = 0;

                @Override
                public Double apply(double deltaTime) {
                    elapsed += deltaTime;
                    return (elapsed);
                }
            });

            case SIN: return new EasyTimedDoubleSupplier(new DoubleSupplier() {
                double v = 0;
                @Override public double getAsDouble() {
                    v += 0.1415;
                    return (Math.sin(v));
                }
            });

            case SIN_TIME: return new EasyTimedDoubleSupplier(new DoubleFunction<Double>() {
                double elapsed = 0;

                @Override
                public Double apply(double deltaTime) {
                    elapsed += deltaTime;
                    return (Math.sin(elapsed));
                }
            });

            default: 
                fail("Unrecognized input formula: " + inputFormula);
                return null;
        }
    }
    @ParameterizedClass
    @EnumSource(InputFormula.class)
    @Nested 
    class ConstructionDependent {
        AnalogTouchSensorFake sensor;
        AnalogInputControllerFake controller;

        @Parameter
        InputFormula inputFormula;

        @BeforeEach
        void construct() {
            try {
                controller = new AnalogInputControllerFake();
            } catch(RobotCoreException | InterruptedException exc) {
                fail(exc);
            }

            sensor = new AnalogTouchSensorFake(getSupplier(inputFormula), TH, controller);
        }
        
    
        @DisplayName("isPressed returns the expected value only after updating")
        @Test
        void isPressedReturnsExpectedValueOnlyOnUpdate() {
            final TimedDoubleSupplier expectedSupplier = getSupplier(inputFormula);

            final boolean expectedInit = expectedSupplier.getAsDouble() > TH;
            assertEquals(expectedInit, sensor.isPressed(), "Expected initial value is correct");
            assertEquals(expectedInit, sensor.isPressed(), "Initial value doesn't change when with no update");

            for(int i = 1; i < 6; i++) {
                assumeTrue(0.0 != sensor.update(1.0), "The sensor updated");
                assumeTrue(0.0 != expectedSupplier.update(1.0), "The supplier updated");

                final boolean expectedValue = expectedSupplier.getAsDouble() > TH;
                assertEquals(expectedValue, sensor.isPressed(), i + "-th value matched expected");
                assertEquals(expectedValue, sensor.isPressed(), i + "-th value did not change with no update");
            }
        }
    
        @DisplayName("getValue returns the expected value only after updating")
        @Test
        void getValueReturnsExpectedValueOnlyOnUpdate() {
            final TimedDoubleSupplier expectedSupplier = getSupplier(inputFormula);

            final double expectedInit = expectedSupplier.getAsDouble();
            assertEquals(expectedInit, sensor.getValue(), "Expected initial value is correct");
            assertEquals(expectedInit, sensor.getValue(), "Initial value doesn't change when with no update");

            for(int i = 1; i < 6; i++) {
                assumeTrue(0.0 != sensor.update(1.0), "The sensor updated");
                assumeTrue(0.0 != expectedSupplier.update(1.0), "The supplier updated");

                final double expectedValue = expectedSupplier.getAsDouble();
                assertEquals(expectedValue, sensor.getValue(), i + "-th value matched expected");
                assertEquals(expectedValue, sensor.getValue(), i + "-th value did not change with no update");
            }
        }

        @DisplayName("isPressed is true if greater than threshold; false otherwise")
        @Test 
        void getValueIs1or0DependingOnIsPressed() {
            for(int i = 0; i < 20; i++) {
                sensor.update(1.0);
                final double value = sensor.getValue();

                if(value > TH) {
                    assertTrue(sensor.isPressed(), i + "-th value is > TH; isPressed true");
                } else {
                    assertFalse(sensor.isPressed(), "-th value is <= TH; isPressed false");
                }
            }
        }
    }
 
    
    @ParameterizedClass
    @EnumSource(InputFormula.class)
    @Nested
    class UpdaterDependent {
        private ModularUpdater updater = new ModularUpdater();
        private LynxUsbDeviceImplFake lynxUsb = new LynxUsbDeviceImplFake();
        private LynxModuleHardwareFake lynx = null;
        
        AnalogTouchSensorFake sensor;
        AnalogInputControllerFake controller;

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

        @Parameter
        InputFormula inputFormula;

        @BeforeEach
        void constructAndRegister() {
            System.out.println("*****************************");

            // Creating the controller
            lynx = createLynxModule(lynxUsb);
            controller = new AnalogInputControllerFake(lynx);
            sensor = new AnalogTouchSensorFake(getSupplier(inputFormula), TH, controller);
            lynxUsb.setAnalogInputs(new AnalogInputFake[] {
                sensor.asAnalogInput(), null, null, null
            });
            
            if(controller.getLynxModule() == null) {
                fail("Controller LynxModule was null");
            }

            updater.register(sensor, controller.getLynxModule());
            updater.register(counter, controller.getLynxModule());
        }

        @AfterEach
        void print() {
                System.out.println("============================= auto");
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
            sensor.setUpdatingEnabled(false);
            assertEquals(false, sensor.isUpdatingEnabled());
            
            sensor.setUpdatingEnabled(false);
            assertEquals(false, sensor.isUpdatingEnabled());

            sensor.setUpdatingEnabled(true);
            assertEquals(true, sensor.isUpdatingEnabled());

            sensor.setUpdatingEnabled(false);
            assertEquals(false, sensor.isUpdatingEnabled());

            sensor.setUpdatingEnabled(true);
            assertEquals(true, sensor.isUpdatingEnabled());
        }
        
        @DisplayName("forget removes updater internally; remember adds it back")
        @Test
        void forgetRemovesRememberAdds() {
            sensor.getValue();
            System.out.println("[ForgetRemoves]");
            assertEquals(1, counter.getTotalUpdates(), "is registered initially");
            
            sensor.forget(updater);
            sensor.getValue();
            assertEquals(1, counter.getTotalUpdates(), "updater no longer updated by sensor");

            sensor.getValue();
            sensor.getValue();
            sensor.getValue();
            assertEquals(1, counter.getTotalUpdates(), "updater still no longer updated by sensor");

            sensor.remember(updater);
            sensor.getValue();
            assertEquals(2, counter.getTotalUpdates(), "updater now is updated by sensor");
            
            sensor.getValue();
            sensor.getValue();
            sensor.getValue();
            assertEquals(5, counter.getTotalUpdates(), "updater still is updated by sensor");
            System.out.println("=============================\n");
        }

        @DisplayName("getValue updates automatically when registered with an updater")
        @Test
        void getValueUpdatesAutomaticallyWhenRegistered() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedDoubleSupplier expectedSupplier = getSupplier(inputFormula);
            
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length), "The Supplier did update");
            assertEquals(expectedSupplier.getAsDouble(), sensor.getValue(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());

            expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
            final double secondValue = sensor.getValue();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getAsDouble(), secondValue, "Value did change without needing an update");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != sensor.update(1.0), "The DIGITAL did update");
                // System.out.println("[get value returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertFloatEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }

        @DisplayName("getValue delays always when bulk caching is off")
        @Test
        void getValueDelaysWhenBulkCachingDisabled() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedDoubleSupplier expectedSupplier = getSupplier(inputFormula);
            
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length), "The Supplier did update");
            assertEquals(expectedSupplier.getAsDouble(), sensor.getValue(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());

            expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
            final double secondValue = sensor.getValue();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getAsDouble(), secondValue, "Value did change without needing an update");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != sensor.update(1.0), "The DIGITAL did update");
                // System.out.println("[get value returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertFloatEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertFloatEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getValue delays (the correct time) only on clear cache in MANUAL")
        @Test
        void getValueDelaysOnlyWhenClearManual() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedDoubleSupplier expectedSupplier = getSupplier(inputFormula);
            
            lynxUsb.setAnalogInputs(new AnalogInputFake[] {
                sensor.asAnalogInput(), null, null, null
            });
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

            counter.clearCount();
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length), "The Supplier did update");
            assertEquals(expectedSupplier.getAsDouble(), sensor.getValue(), "Expected initial value is correct");
            // Updater.updateAllOnce(Set.of(updater), Updater.UpdateDelaySource.DIGITAL);
            assertEquals(1, counter.getTotalUpdates());

            assertEquals(expectedSupplier.getAsDouble(), sensor.getValue(), "Expected initial value didnt change");
            assertEquals(1, counter.getTotalUpdates());

            lynx.clearBulkCache();
            expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
            final double secondValue = sensor.getValue();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getAsDouble(), secondValue, "Value did change after clearing cache");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != sensor.update(1.0), "The DIGITAL did update");
                // System.out.println("[get value returns] i = " + i + "   formula = " + inputFormula.name());
                
                lynx.clearBulkCache();
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertFloatEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertFloatEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value did not change");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                lynx.clearBulkCache();
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertFloatEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getValue delays only on repeat invocation in AUTOMATIC")
        @Test
        void getValueDelaysOnlyWhenRepeatedInAuto() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedDoubleSupplier expectedSupplier = getSupplier(inputFormula);
            final AnalogTouchSensorFake inputAnalog2 = new AnalogTouchSensorFake(getSupplier(inputFormula), TH, controller);
            
            updater.register(sensor, lynx);
            updater.register(inputAnalog2, lynx);
            lynxUsb.setAnalogInputs(new AnalogInputFake[] {
                sensor.asAnalogInput(), inputAnalog2.asAnalogInput(), null, null 
            });
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);

            counter.clearCount();
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length), "The Supplier did update");
            assertEquals(expectedSupplier.getAsDouble(), sensor.getValue(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());
            
            final double secondValue = inputAnalog2.getValue();
            assertEquals(1, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getAsDouble(), secondValue, "Value not did change after using other method");

            expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
            final double thirdValue = sensor.getValue();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getAsDouble(), thirdValue, "Value did change after repeating");

            final double fourthValue = inputAnalog2.getValue();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getAsDouble(), fourthValue, "Value not did change after using other method");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != sensor.update(1.0), "The DIGITAL did update");
                // System.out.println("[get value returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertFloatEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertFloatEquals(expectedSupplier.getAsDouble(), inputAnalog2.getValue(), 1e-9, i + "-th value did not change");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertFloatEquals(expectedSupplier.getAsDouble(), sensor.getValue(), 1e-9, i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
    }

}