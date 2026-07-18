/*
 * DistanceSensorFakeUnitTest.java
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

import clarson.ftc.faker.DistanceSensorFake;
import clarson.ftc.faker.function.DistanceGetter;
import clarson.ftc.faker.function.TimedDistanceGetter;
import clarson.ftc.faker.util.AbstractTwoWayUpdateable;
import clarson.ftc.faker.util.EasyTimedDistanceGetter;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.Updater;

import static clarson.ftc.faker.test.TestUtil.*;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import static org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.MM;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.function.BiFunction;
import java.util.function.DoubleSupplier;

import static java.util.concurrent.TimeUnit.SECONDS;

class DistanceSensorFakeUnitTest {
    @DisplayName("Can construct")
    @Test
    void canConstruct() {

        assertDoesNotThrow(() -> new DistanceSensorFake((units) -> 0.0));
        assertDoesNotThrow(() -> new DistanceSensorFake((units) -> 31.4));
        
        assertDoesNotThrow(() -> new DistanceSensorFake(new EasyTimedDistanceGetter((unused) -> 0.0)));
        assertDoesNotThrow(() -> new DistanceSensorFake(new EasyTimedDistanceGetter((unused) -> 10)));
    }
    
    public static enum InputFormula {
        ALWAYS_0,
        ALWAYS_1,
        INCREMENT,
        POWER_OF_TWO,
        SIN
    }

    static TimedDistanceGetter getSupplier(InputFormula inputFormula) {
        switch (inputFormula) {
            case ALWAYS_0:
                return new EasyTimedDistanceGetter(() -> 0.0);

            case ALWAYS_1:
                return new EasyTimedDistanceGetter(() -> 1.0);

            case INCREMENT:
                return new EasyTimedDistanceGetter(new DoubleSupplier() {
                    private double val = 0;

                    @Override
                    public double getAsDouble() {
                        return (val)++;
                    }
                });

            case POWER_OF_TWO:
                return new EasyTimedDistanceGetter(new DoubleSupplier() {
                    private int val = 0;

                    @Override
                    public double getAsDouble() {
                        return 1L << (val++);
                    }
                });

            case SIN:
                return new EasyTimedDistanceGetter(new BiFunction<DistanceUnit, Double, Double>() {
                    private double elapsed = 0;

                    @Override
                    public Double apply(DistanceUnit unit, Double deltaSec) {
                        final double val = Math.sin(elapsed);
                        elapsed += deltaSec;
                        return val;
                    }
                });

            default:
                fail("Unregonized inputFormula \"" + inputFormula.name() + "\"");
                return null;
        }
    }

    @ParameterizedClass
    @EnumSource(InputFormula.class)
    @Nested
    class ConstructionDependent {
        private DistanceSensorFake sensor;

        @Parameter
        private InputFormula inputFormula;

        @BeforeEach
        void constructSensor() {
            sensor = new DistanceSensorFake(getSupplier(inputFormula));
        }
    
        @DisplayName("getDistance returns the expected value only after update")
        @Test
        void getDistanceReturnsExpectedValueOnlyOnUpdate() {
            // NOTE: All the input formulas only get new values after updating
            //       This is the intended usage, as otherwise a sensor gets a new value every
            //       call to getDistance(MM), which does make as much sense.

            // NOTE: The sensor is not registered with an updater, and so update() must be called manually
            //       Were it connected to an updater, it would update automatically. A separate test 
            //       exists for this.
            final TimedDistanceGetter expectedSupplier = getSupplier(inputFormula);

            final double expectedInit = expectedSupplier.getDistance(MM);
            assertEquals(expectedInit, sensor.getDistance(MM), "Expected initial value is correct");
            assertEquals(expectedInit, sensor.getDistance(MM), "Initial value doesn't change when no update called");

            System.out.println();
            for(int i = 1; i < 6; i++) {
                assumeTrue(0.0 != sensor.update(1.0), "The Sensor did update");
                assumeTrue(0.0 != expectedSupplier.update(1.0), "The Supplier did update");

                final double expectedValue = expectedSupplier.getDistance(MM);
                System.out.println("[get state returns] i = " + i + "   formula = " + inputFormula.name());
                assertEquals(expectedValue, sensor.getDistance(MM), i + "-th value matches expected");
                assertEquals(expectedValue, sensor.getDistance(MM), i + "-th value did not change");
            }
        }


        @Nested
        class UpdaterDependent {
            private ModularUpdater updater = new ModularUpdater();

            private final MockUpdateable counter = new MockUpdateable();

            @BeforeEach
            void registerWithUpdater() {
                updater.register(sensor);
                updater.register(counter);
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
                final DistanceSensorFake countersensor = new DistanceSensorFake(
                    new EasyTimedDistanceGetter((unit, deltaTime) -> {
                        // NOTE: updating external Updateables in an update() method is ***strongly*** discouraged.
                        //       We do it here only because we need to check when something is udpated.
                        counter.update(deltaTime);
                        return unit.fromMm(0.0); // unused
                    })
                );
                assertEquals(1 - 1, counter.getTotalCalls(), "The counter is called once by construction");

                // Set updating is true
                countersensor.setUpdatingEnabled(true);
                countersensor.update(1.0); // The specific value is not important, as long as deltaSEc != 0
                assertEquals(2 - 1, counter.getTotalCalls(), "Counter is called upon (first) update");

                assumeTrue(counter.getTotalCalls() == 1, "The counter value deos not change when nothing happens");

                countersensor.update(1.0);
                countersensor.update(1.0);
                countersensor.update(1.0);
                assertEquals(5 - 1, counter.getTotalCalls(), "counter is still called even when updates are in succesion");

                // Is updating is false
                countersensor.setUpdatingEnabled(false);
                countersensor.update(1.0);
                assertEquals(5 - 1, counter.getTotalCalls(), "counter is not called on (first) disabled update");
                
                countersensor.update(1.0);
                countersensor.update(1.0);
                countersensor.update(1.0);
                assertEquals(5 - 1, counter.getTotalCalls(), "update does not modify the setUpdatingEnabled countersensor");
            }

            @DisplayName("forget removes updater internally; remember adds it back")
            @Test
            void forgetRemovesRememberAdds() {
                sensor.getDistance(MM);
                assertEquals(1, counter.getTotalUpdates(), "is registered initially");
                
                sensor.forget(updater);
                sensor.getDistance(MM);
                assertEquals(1, counter.getTotalUpdates(), "updater no longer updated by sensor");

                sensor.getDistance(MM);
                sensor.getDistance(MM);
                sensor.getDistance(MM);
                assertEquals(1, counter.getTotalUpdates(), "updater still no longer updated by sensor");

                sensor.remember(updater);
                sensor.getDistance(MM);
                assertEquals(2, counter.getTotalUpdates(), "updater now is updated by sensor");
                
                sensor.getDistance(MM);
                sensor.getDistance(MM);
                sensor.getDistance(MM);
                assertEquals(5, counter.getTotalUpdates(), "updater still is updated by sensor");
            }

            @DisplayName("getDistance updates automatically when registered with an updater")
            @Test
            void getDistanceUpdatesAutomaticallyWhenRegistered() {
                // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
                final TimedDistanceGetter expectedSupplier = getSupplier(inputFormula);

                assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.I2C.length), "The Supplier did update");
                assertEquals(expectedSupplier.getDistance(MM), sensor.getDistance(MM), "Expected initial value is correct");
                assertEquals(1, counter.getTotalUpdates());

                expectedSupplier.update(Updater.UpdateDelaySource.I2C.length);
                final double secondState = sensor.getDistance(MM);
                assertEquals(2, counter.getTotalUpdates());
                assertEquals(expectedSupplier.getDistance(MM), secondState, "Value did change without needing an update");

                System.out.println();
                for(int i = 2; i < 7; i++) {
                    // assumeTrue(0.0 != sensor.update(1.0), "The Sensor did update");
                    // System.out.println("[get state returns] i = " + i + "   formula = " + inputFormula.name());
                    
                    expectedSupplier.update(Updater.UpdateDelaySource.I2C.length);
                    assertEquals(expectedSupplier.getDistance(MM), sensor.getDistance(MM), i + "-th value matches expected");
                    assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                    
                    expectedSupplier.update(Updater.UpdateDelaySource.I2C.length);
                    assertEquals(expectedSupplier.getDistance(MM), sensor.getDistance(MM), i + "-th value did change");
                    assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
                }
            }

            @DisplayName("getDistance delays the simulation by the correct amount of time")
            @Test
            void getDistanceDelaysByTheCorrectTime() {
                sensor.getDistance(MM);
                assertEquals(Updater.UpdateDelaySource.I2C.length, counter.getLastDeltaSec(), "false delta sec digital");

                sensor.getDistance(MM);
                sensor.getDistance(MM);
                sensor.getDistance(MM);
                assertEquals(Updater.UpdateDelaySource.I2C.length, counter.getLastDeltaSec(), "same set delta sec digital");
                
                sensor.getDistance(MM);
                assertEquals(Updater.UpdateDelaySource.I2C.length, counter.getLastDeltaSec(), "true set delta sec digital");
                
                sensor.getDistance(MM);
                sensor.getDistance(MM);
                sensor.getDistance(MM);
                assertEquals(Updater.UpdateDelaySource.I2C.length, counter.getLastDeltaSec(), "same set delta sec digital");

                sensor.getDistance(MM);
                assertEquals(Updater.UpdateDelaySource.I2C.length, counter.getLastDeltaSec(), "change delta sec digital");

            }

            @DisplayName("getDistance delays always when bulk caching is off")
            @Test
            void getDistanceDelaysWhenBulkCachingDisabled() {
                // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
                final TimedDistanceGetter expectedSupplier = getSupplier(inputFormula);

                assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.I2C.length), "The Supplier did update");
                assertEquals(expectedSupplier.getDistance(MM), sensor.getDistance(MM), "Expected initial value is correct");
                assertEquals(1, counter.getTotalUpdates());

                expectedSupplier.update(Updater.UpdateDelaySource.I2C.length);
                final double secondState = sensor.getDistance(MM);
                assertEquals(2, counter.getTotalUpdates());
                assertEquals(expectedSupplier.getDistance(MM), secondState, "Value did change without needing an update");

                System.out.println();
                for(int i = 2; i < 7; i++) {
                    // assumeTrue(0.0 != sensor.update(1.0), "The Sensor did update");
                    // System.out.println("[get state returns] i = " + i + "   formula = " + inputFormula.name());
                    
                    expectedSupplier.update(Updater.UpdateDelaySource.I2C.length);
                    assertEquals(expectedSupplier.getDistance(MM), sensor.getDistance(MM), i + "-th value matches expected");
                    assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                    
                    expectedSupplier.update(Updater.UpdateDelaySource.I2C.length);
                    assertEquals(expectedSupplier.getDistance(MM), sensor.getDistance(MM), i + "-th value did change");
                    assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
                }
            }
        }

    }
}