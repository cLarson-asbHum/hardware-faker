/*
 * AnalogInputFakeUnitTest.java
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
import clarson.ftc.faker.function.TimedVoltageGetter;
import clarson.ftc.faker.function.VoltageGetter;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.AbstractTwoWayUpdateable;
import clarson.ftc.faker.util.EasyTimedVoltageGetter;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;
import java.util.function.BiFunction;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;
import static clarson.ftc.faker.test.TestUtil.*;
import static org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit.VOLTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class AnalogInputFakeUnitTest {
    @DisplayName("Can Construct")
    @Test
    void canConstruct() {
        assertDoesNotThrow(() -> new AnalogInputFake(() -> 0.0));
        assertDoesNotThrow(() -> new AnalogInputFake(() -> 1.0));
        assertDoesNotThrow(() -> new AnalogInputFake(() -> 3.141));

        assertDoesNotThrow(() -> new AnalogInputFake((units) -> units.convert(3.141, VOLTS)));
        assertDoesNotThrow(() -> new AnalogInputFake((units) -> units.convert(0.0, VOLTS)));
        assertDoesNotThrow(() -> new AnalogInputFake((units) -> units.convert(0.0, VOLTS)));

        assertDoesNotThrow(() -> new EasyTimedVoltageGetter(() -> 3.141));
        assertDoesNotThrow(() -> new EasyTimedVoltageGetter(() -> 0.592));
    }

    static enum InputFormula {
        ZERO,
        ONE,
        INCREMENT,
        INCREMENT_TIME,

        SIN,
        SIN_TIME
    }

    TimedVoltageGetter getSupplier(InputFormula inputFormula) {
        switch(inputFormula) {
            case ZERO: return new EasyTimedVoltageGetter(() -> 0.0); // Volts

            case ONE: return new EasyTimedVoltageGetter(() -> 1.0); // Volts

            case INCREMENT: return new EasyTimedVoltageGetter(new VoltageGetter() {
                double v = 0;
                @Override public double getVoltage(VoltageUnit unit) {
                    v += 0.1415;
                    return unit.convert(v, VOLTS);
                }
            });

            case INCREMENT_TIME: return new EasyTimedVoltageGetter(new BiFunction<VoltageUnit, Double, Double>() {
                double elapsed = 0;

                @Override
                public Double apply(VoltageUnit unit, Double deltaTime) {
                    elapsed += deltaTime;
                    return unit.convert(elapsed, VOLTS);
                }
            });

            case SIN: return new EasyTimedVoltageGetter(new VoltageGetter() {
                double v = 0;
                @Override public double getVoltage(VoltageUnit unit) {
                    v += 0.1415;
                    return unit.convert(Math.sin(v), VOLTS);
                }
            });

            case SIN_TIME: return new EasyTimedVoltageGetter(new BiFunction<VoltageUnit, Double, Double>() {
                double elapsed = 0;

                @Override
                public Double apply(VoltageUnit unit, Double deltaTime) {
                    elapsed += deltaTime;
                    return unit.convert(Math.sin(elapsed), VOLTS);
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
        AnalogInputFake analog;
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

            analog = new AnalogInputFake(getSupplier(inputFormula), controller);
        }
    
        @DisplayName("getVoltage returns the expected value only after updating")
        @Test
        void getVoltageReturnsExpectedValueOnlyOnUpdate() {
            final TimedVoltageGetter expectedSupplier = getSupplier(inputFormula);

            final double expectedInit = expectedSupplier.getVoltage(VOLTS);
            assertEquals(expectedInit, analog.getVoltage(), "Expected initial value is correct");
            assertEquals(expectedInit, analog.getVoltage(), "Initial value doesn't change when with no update");

            for(int i = 1; i < 6; i++) {
                assumeTrue(0.0 != analog.update(1.0), "The analog updated");
                assumeTrue(0.0 != expectedSupplier.update(1.0), "The supplier updated");

                final double expectedValue = expectedSupplier.getVoltage(VOLTS);
                assertEquals(expectedValue, analog.getVoltage(), i + "-th value matched expected");
                assertEquals(expectedValue, analog.getVoltage(), i + "-th value did not change with no update");
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
        
        AnalogInputFake analog;
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
            // Creating the controller
            lynx = createLynxModule(lynxUsb);
            controller = new AnalogInputControllerFake(lynx);
            analog = new AnalogInputFake(getSupplier(inputFormula), controller);
            lynxUsb.setAnalogInputs(new AnalogInputFake[] {
                analog, null, null, null
            });
            
            if(controller.getLynxModule() == null) {
                fail("Controller LynxModule was null");
            }

            updater.register(analog, controller.getLynxModule());
            updater.register(counter, controller.getLynxModule());
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
            analog.setUpdatingEnabled(false);
            assertEquals(false, analog.isUpdatingEnabled());
            
            analog.setUpdatingEnabled(false);
            assertEquals(false, analog.isUpdatingEnabled());

            analog.setUpdatingEnabled(true);
            assertEquals(true, analog.isUpdatingEnabled());

            analog.setUpdatingEnabled(false);
            assertEquals(false, analog.isUpdatingEnabled());

            analog.setUpdatingEnabled(true);
            assertEquals(true, analog.isUpdatingEnabled());
        }
        
        @DisplayName("forget removes updater internally; remember adds it back")
        @Test
        void forgetRemovesRememberAdds() {
            analog.getVoltage();
            assertEquals(1, counter.getTotalUpdates(), "is registered initially");
            
            analog.forget(updater);
            analog.getVoltage();
            assertEquals(1, counter.getTotalUpdates(), "updater no longer updated by analog");

            analog.getVoltage();
            analog.getVoltage();
            analog.getVoltage();
            assertEquals(1, counter.getTotalUpdates(), "updater still no longer updated by analog");

            analog.remember(updater);
            analog.getVoltage();
            assertEquals(2, counter.getTotalUpdates(), "updater now is updated by analog");
            
            analog.getVoltage();
            analog.getVoltage();
            analog.getVoltage();
            assertEquals(5, counter.getTotalUpdates(), "updater still is updated by analog");
        }

        @DisplayName("getVoltage updates automatically when registered with an updater")
        @Test
        void getVoltageUpdatesAutomaticallyWhenRegistered() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedVoltageGetter expectedSupplier = getSupplier(inputFormula);
            
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length), "The Supplier did update");
            assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());

            expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
            final double secondVoltage = analog.getVoltage();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getVoltage(VOLTS), secondVoltage, "Value did change without needing an update");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != analog.update(1.0), "The Analog did update");
                // System.out.println("[get voltage returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }

        @DisplayName("getVoltage delays always when bulk caching is off")
        @Test
        void getVoltageDelaysWhenBulkCachingDisabled() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedVoltageGetter expectedSupplier = getSupplier(inputFormula);
            
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length), "The Supplier did update");
            assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());

            expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
            final double secondVoltage = analog.getVoltage();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getVoltage(VOLTS), secondVoltage, "Value did change without needing an update");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != analog.update(1.0), "The Analog did update");
                // System.out.println("[get voltage returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getVoltage delays (the correct time) only on clear cache in MANUAL")
        @Test
        void getVoltageDelaysOnlyWhenClearManual() {
            System.out.println("=============================");
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedVoltageGetter expectedSupplier = getSupplier(inputFormula);
            
            lynxUsb.setAnalogInputs(new AnalogInputFake[] {
                analog, null, null, null
            });
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

            counter.clearCount();
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length), "The Supplier did update");
            assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), "Expected initial value is correct");
            // Updater.updateAllOnce(Set.of(updater), Updater.UpdateDelaySource.ANALOG);
            System.out.println("=============================");
            assertEquals(1, counter.getTotalUpdates());

            assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), "Expected initial value didnt change");
            assertEquals(1, counter.getTotalUpdates());

            lynx.clearBulkCache();
            expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
            final double secondVoltage = analog.getVoltage();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getVoltage(VOLTS), secondVoltage, "Value did change after clearing cache");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != analog.update(1.0), "The Analog did update");
                // System.out.println("[get voltage returns] i = " + i + "   formula = " + inputFormula.name());
                
                lynx.clearBulkCache();
                expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value did not change");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                lynx.clearBulkCache();
                expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getVoltage delays only on repeat invokation in AUTOMATIC")
        @Test
        void getVoltageDelaysOnlyWhenRepeatedInAuto() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedVoltageGetter expectedSupplier = getSupplier(inputFormula);
            final AnalogInputFake inputAnalog2 = new AnalogInputFake(getSupplier(inputFormula), controller);
            
            updater.register(analog, lynx);
            updater.register(inputAnalog2, lynx);
            lynxUsb.setAnalogInputs(new AnalogInputFake[] {
                analog, inputAnalog2, null, null 
            });
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);

            counter.clearCount();
            System.out.println("*****************************");
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length), "The Supplier did update");
            assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());
            
            final double secondVoltage = inputAnalog2.getVoltage();
            assertEquals(1, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getVoltage(VOLTS), secondVoltage, "Value not did change after using other method");

            expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
            final double thirdVoltage = analog.getVoltage();
            System.out.println("*****************************");
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getVoltage(VOLTS), thirdVoltage, "Value did change after repeating");

            final double fourthVoltage = inputAnalog2.getVoltage();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getVoltage(VOLTS), fourthVoltage, "Value not did change after using other method");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != analog.update(1.0), "The Analog did update");
                // System.out.println("[get voltage returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals(expectedSupplier.getVoltage(VOLTS), inputAnalog2.getVoltage(), i + "-th value did not change");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.ANALOG.length);
                assertEquals(expectedSupplier.getVoltage(VOLTS), analog.getVoltage(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
    }

}