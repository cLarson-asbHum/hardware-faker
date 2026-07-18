/*
 * ColorRangeSensorFakeUnitTest.java
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

import clarson.ftc.faker.ColorRangeSensorFake;
import clarson.ftc.faker.LEDFake;
import clarson.ftc.faker.function.ColorGetter;
import clarson.ftc.faker.function.ColorRangeGetter;
import clarson.ftc.faker.function.DistanceGetter;
import clarson.ftc.faker.function.TimedDistanceGetter;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.AbstractTwoWayUpdateable;
import clarson.ftc.faker.util.EasyColorRangeGetter;
import clarson.ftc.faker.util.EasyTimedDistanceGetter;

import static clarson.ftc.faker.test.TestUtil.*;

import com.qualcomm.robotcore.hardware.NormalizedRGBA;

import java.util.function.*;

import static java.util.concurrent.TimeUnit.SECONDS;

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

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import static org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.MM;

class ColorRangeSensorFakeUnitTest {
    @DisplayName("Can construct")
    @Test
    void canConstruct() {
        assertDoesNotThrow(() -> new ColorRangeSensorFake((units) -> 0.0, () -> 0xaabbcc));
        assertDoesNotThrow(() -> new ColorRangeSensorFake((units) -> 31.4, () -> 0x3243f6));

        final ColorRangeGetter easyCrGetter = new EasyColorRangeGetter(
            (units) -> units.fromUnit(DistanceUnit.INCH, 4.13),
            () -> 0x01234567
        );

        assertDoesNotThrow(() -> new ColorRangeSensorFake(easyCrGetter));

        final LEDFake ledFake = new LEDFake();
        assertDoesNotThrow(() -> new ColorRangeSensorFake(easyCrGetter, ledFake));
    }   

    static enum DistFormula {
        ALWAYS_0,
        ALWAYS_1,
        INCREMENT,
        POWER_OF_TWO,
        SIN
    }

    private static TimedDistanceGetter getDistGetter(DistFormula distFormula) {
        switch (distFormula) {
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
                fail("Unrecognized distFormula \"" + distFormula.name() + "\"");
                return null;
        }
    }

    @ParameterizedClass
    @EnumSource(DistFormula.class)
    @Nested
    class ConstructionDependent {
        private ColorRangeSensorFake sensor;
        private LEDFake light;

        @Parameter
        private DistFormula distFormula;

        private int expectedAlpha = 0;
        private int expectedR = 0;
        private int expectedG = 0;
        private int expectedB = 0;

        private void setExpectedColorBytes(int r, int g, int b) {
            setExpectedColorShorts(r * 257, g * 257, b * 257);
        }
        
        private void setExpectedColorShorts(int r, int g, int b) {
            this.expectedAlpha = (r + g + b ) / 3;
            this.expectedR = r;
            this.expectedG = g;
            this.expectedB = b;
        }

        private int getExpectedArgb() {
            return getExpectedColor() | ((expectedAlpha & 0xff_00) << 16);
        }

        private int getExpectedColor() {
            return ColorGetter.colorIntFromShorts(expectedR, expectedG, expectedB);
        }

        private void assertExpectedColor() {

            assertEquals(0x014589, ColorGetter.colorIntFromShorts(0x0123, 0x4567, 0x89ab));

            setExpectedColorBytes(255, 128, 64);
            assertEquals((int) 0x95_ff_80_40, getExpectedArgb());
            assertEquals((int)    0xff_80_40, getExpectedColor());

            setExpectedColorShorts(0x1200, 0x3400, 0x5600);
            assertEquals((int) 0x34_12_34_56, getExpectedArgb());
            assertEquals((int)    0x12_34_56, getExpectedColor());

            setExpectedColorBytes(128, 128, 128);
            assertEquals((int) 0x80_80_80_80, getExpectedArgb());
            assertEquals((int)    0x80_80_80, getExpectedColor());
            
            setExpectedColorBytes(0, 0, 0);
            assertEquals((int) 0x00_00_00_00, getExpectedArgb());
            assertEquals((int)    0x00_00_00, getExpectedColor());
        }

        private ColorRangeGetter createCrGetterWithTimedDist(TimedDistanceGetter timed, ColorGetter color) {
            return new ColorRangeGetter() {
                final TimedDistanceGetter dist = timed;
                int r = color.redShort();
                int g = color.greenShort();
                int b = color.blueShort();

                @Override
                public double update(double deltaSec) {
                    dist.update(deltaSec);
                    r = color.redShort();
                    g = color.greenShort();
                    b = color.blueShort();
                    return 1.0;
                }

                @Override
                public boolean isUpdatingEnabled() {
                    return dist.isUpdatingEnabled();
                }

                @Override
                public boolean setUpdatingEnabled(boolean newValue) {
                    return dist.setUpdatingEnabled(newValue);
                }

                @Override
                public double getDistance(DistanceUnit units) {
                    return dist.getDistance(units);
                }

                @Override
                public int getColor() {
                    return ColorGetter.colorIntFromShorts(r, g, b);
                }

                @Override
                public int redShort() {
                    return r;
                }
                
                @Override
                public int greenShort() {
                    return g;
                }
                
                @Override
                public int blueShort() {
                    return b;
                }
            };
        }

        @BeforeEach
        void construct() {
            light = new LEDFake();
            sensor = new ColorRangeSensorFake(
                createCrGetterWithTimedDist(getDistGetter(distFormula), new ColorGetter() {
                    @Override public int getColor() {
                        return getExpectedColor();
                    }
                    @Override public int redShort() {
                        return expectedR;
                    }
                    @Override public int greenShort() {
                        return expectedG;
                    }
                    @Override public int blueShort() {
                        return expectedB;
                    }
                }),
                light
            );

            // If the powInt and getExpectedArgb methods don't work, none of the tests will
            assertExpectedColor();
        }

        @Nested 
        class Distance {
            @DisplayName("getDistance returns the expected value only after update")
            @Test
            void getDistanceReturnsExpectedValueOnlyOnUpdate() {
                // NOTE: All the input formulas only get new values after updating
                //       This is the intended usage, as otherwise a sensor gets a new value every
                //       call to getDistance(MM), which does make as much sense.

                // NOTE: The sensor is not registered with an updater, and so update() must be called manually
                //       Were it connected to an updater, it would update automatically. A separate test 
                //       exists for this.
                final TimedDistanceGetter expectedSupplier = getDistGetter(distFormula);

                final double expectedInit = expectedSupplier.getDistance(MM);
                assertEquals(expectedInit, sensor.getDistance(MM), "Expected initial value is correct");
                assertEquals(expectedInit, sensor.getDistance(MM), "Initial value doesn't change when no update called");

                System.out.println();
                for(int i = 1; i < 6; i++) {
                    assumeTrue(0.0 != sensor.update(1.0), "The Sensor did update");
                    assumeTrue(0.0 != expectedSupplier.update(1.0), "The Supplier did update");

                    final double expectedValue = expectedSupplier.getDistance(MM);
                    System.out.println("[get state returns] i = " + i + "   formula = " + distFormula.name());
                    assertEquals(expectedValue, sensor.getDistance(MM), i + "-th value matches expected");
                    assertEquals(expectedValue, sensor.getDistance(MM), i + "-th value did not change");
                }
            }
        }

        @Nested
        class RawArgb {
            
            @DisplayName("argb gets the expected raw color int only after update")
            @Test
            void argbGetsTheExpectedRawColorIntOnlyAfterUpdate() {
                setExpectedColorShorts(0, 0, 0);
                // assertNotEquals((int) getExpectedArgb(), sensor.argb(), "initial isn't accidentally accurate");
                sensor.update(1.0);
                assertEquals((int) getExpectedArgb(), sensor.argb(), "argb integer is half-opaque black");
                
                setExpectedColorShorts(0x100, 0x200, 0x300);
                assertNotEquals((int) getExpectedArgb(), sensor.argb(), "no update, no change");
                sensor.update(1.0);
                assertEquals((int) getExpectedArgb(), sensor.argb(), "argb integer has differing rgb and a values");
                
                setExpectedColorShorts(0xff00, 0xff00, 0xff00);
                assertNotEquals((int) getExpectedArgb(), sensor.argb(), "no update, no change");
                sensor.update(1.0);
                assertEquals((int) getExpectedArgb(), sensor.argb(), "argb handles negatives correctly");
            }
    
            @DisplayName("Red is correct and is an unsigned byte")
            @Test
            void redIsCorrectAndIsAnUnsignedByte() {
                setExpectedColorShorts(0, 0, 0);
                sensor.update(1.0);
                assertEquals(0, sensor.red(), "red integer is initially correct");
                
                setExpectedColorShorts(0x100, 0x200, 0x300);
                assertNotEquals(0x100, sensor.red(), "no update, no change");
                sensor.update(1.0);
                assertEquals(0x100, sensor.red(), "red integer changes when rgb do");
                
                setExpectedColorShorts(0xff00, 0xff00, 0xff00);
                assertNotEquals(0xff00, sensor.red(), "no update, no change");
                sensor.update(1.0);
                assertEquals(0xff00, sensor.red(), "red handles negatives correctly");
            }
    
            @DisplayName("Green is correct and is an unsigned byte")
            @Test
            void greenIsCorrectAndIsAnUnsignedByte() {
                
                setExpectedColorShorts(0, 0, 0);
                sensor.update(1.0);
                assertEquals(0, sensor.green(), "green integer is initially correct");
                
                setExpectedColorShorts(0x100, 0x200, 0x300);
                assertNotEquals(0x200, sensor.green(), "no update, no change");
                sensor.update(1.0);
                assertEquals(0x200, sensor.green(), "green integer changes when rgb do");
                
                setExpectedColorShorts(0xff00, 0xff00, 0xff00);
                assertNotEquals(0xff00, sensor.green(), "no update, no change");
                sensor.update(1.0);
                assertEquals(0xff00, sensor.green(), "green handles negatives correctly");
            }
    
            @DisplayName("Blue is correct and is an unsigned byte")
            @Test
            void blueIsCorrectAndIsAnUnsignedByte() {
                setExpectedColorShorts(0, 0, 0);
                sensor.update(1.0);
                assertEquals(0, sensor.blue(), "blue integer is initially correct");
                
                setExpectedColorShorts(0x100, 0x200, 0x300);
                assertNotEquals(0x300, sensor.blue(), "no update, no change");
                sensor.update(1.0);
                assertEquals(0x300, sensor.blue(), "blue integer changes when rgb do");
                
                setExpectedColorShorts(0xff00, 0xff00, 0xff00);
                assertNotEquals(0xff00, sensor.blue(), "no update, no change");
                sensor.update(1.0);
                assertEquals(0xff00, sensor.blue(), "blue handles negatives correctly");
            }

        }

        @Nested
        class Normalized {
            @DisplayName("getNormalizedColors scales all components from 0-255 to 0-1 range")
            @Test
            void getNormalizedColorsScalesAllComponents() {
                setExpectedColorBytes(255, 255, 255);
                sensor.update(1.0);
                final NormalizedRGBA norm1 = sensor.getNormalizedColors();
                assertEquals(255.0f / 255.0f, norm1.red,   "red component for white is 1.0");
                assertEquals(255.0f / 255.0f, norm1.green, "green component for white is 1.0");
                assertEquals(255.0f / 255.0f, norm1.blue,  "blue component for white is 1.0");

                setExpectedColorBytes(2, 3, 4);
                sensor.update(1.0);
                final NormalizedRGBA norm2 = sensor.getNormalizedColors();
                assertEquals(2.0f / 255.0f, norm2.red,   "red component is only what its set to");
                assertEquals(3.0f / 255.0f, norm2.green, "green component is only what its set to");
                assertEquals(4.0f / 255.0f, norm2.blue,  "blue component is only what its set to");
            }
        }

        @Nested 
        class Gain {
            @DisplayName("getGain gets the value obtained from setGain")
            @Test
            void getGainEqualsSetGain() {
                sensor.setGain(3.141f);
                assertEquals(3.141f, sensor.getGain());
                assertEquals(3.141f, sensor.getGain());
                
                sensor.setGain(3.141f);
                assertEquals(3.141f, sensor.getGain());
                
                sensor.setGain(1.0f);
                assertEquals(1.0f, sensor.getGain());
                
                sensor.setGain(3000f);
                assertEquals(3000f, sensor.getGain());
            }
    
            @DisplayName("Initial gain is 1")
            @Test
            void initialGainIs1() {
                assertEquals(1.0, sensor.getGain());
            }
    
            @DisplayName("setGain scales (normalized) RGB")
            @Test
            void setGainScalesRGBButNotAlpha() {
                setExpectedColorShorts(0x0012, 0x0034, 0x0056);
                sensor.update(1.0);
                final NormalizedRGBA norm1 = sensor.getNormalizedColors();
                assertEquals((float) ((double) 0x0012 / (double) 0xffff), norm1.red, "Null hypothesis red");
                assertEquals((float) ((double) 0x0034 / (double) 0xffff), norm1.green, "Null hypothesis green");
                assertEquals((float) ((double) 0x0056 / (double) 0xffff), norm1.blue, "Null hypothesis blue");
                
                sensor.setGain(2.0f);
                final NormalizedRGBA norm2 = sensor.getNormalizedColors();
                assertEquals(2.0f * (float) ((double) 0x0012 / (double) 0xffff), norm2.red, "Gain of 2.0 | red");
                assertEquals(2.0f * (float) ((double) 0x0034 / (double) 0xffff), norm2.green, "Gain of 2.0 | green");
                assertEquals(2.0f * (float) ((double) 0x0056 / (double) 0xffff), norm2.blue, "Gain of 2.0 | blue");
                
                sensor.setGain(31415.9f);
                final NormalizedRGBA norm3 = sensor.getNormalizedColors();
                assertEquals(1.0f, norm3.red, "Excessive gain stops at 1.0 | red");
                assertEquals(1.0f, norm3.green, "Excessive gain stops at 1.0 | green");
                assertEquals(1.0f, norm3.blue, "Excessive gain stops at 1.0 | blue");
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
                // The following few lines are a hacked together way to get updates to a fake
                // sensor to also update our MockUpdateable (which we can count the number of updates on)
                final TimedDistanceGetter countingDist = new EasyTimedDistanceGetter((unit, deltaTime) -> {
                    // NOTE: updating external Updateables in an update() method is ***strongly*** discouraged.
                    //       We do it here only because we need to check when something is updated.
                    counter.update(deltaTime);
                    return unit.fromMm(0.0); // unused
                });
                final ColorRangeGetter countingGetter = 
                        createCrGetterWithTimedDist(countingDist, () -> 0 /* unused */);
                final ColorRangeSensorFake countingSensor = new ColorRangeSensorFake(countingGetter);
                assertEquals(1 - 1, counter.getTotalCalls(), "The counter is called once by construction");

                // Set updating is true
                countingSensor.setUpdatingEnabled(true);
                countingSensor.update(1.0); // The specific value is not important, as long as deltaSEc != 0
                assertEquals(2 - 1, counter.getTotalCalls(), "Counter is called upon (first) update");

                assumeTrue(counter.getTotalCalls() == 1, "The counter value does not change when nothing happens");

                countingSensor.update(1.0);
                countingSensor.update(1.0);
                countingSensor.update(1.0);
                assertEquals(5 - 1, counter.getTotalCalls(), "counter is still called even when updates are in succession");

                // Is updating is false
                countingSensor.setUpdatingEnabled(false);
                countingSensor.update(1.0);
                assertEquals(5 - 1, counter.getTotalCalls(), "counter is not called on (first) disabled update");
                
                countingSensor.update(1.0);
                countingSensor.update(1.0);
                countingSensor.update(1.0);
                assertEquals(5 - 1, counter.getTotalCalls(), "update does not modify the setUpdatingEnabled countingSensor");
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
                final TimedDistanceGetter expectedSupplier = getDistGetter(distFormula);

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
                    // System.out.println("[get state returns] i = " + i + "   formula = " + distFormula.name());
                    
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

            @DisplayName("getDistance delays always when bulk caching is off, manual, and auto")
            @Test
            void getDistanceDelaysWhenBulkCachingDisabled() {
                // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
                final TimedDistanceGetter expectedSupplier = getDistGetter(distFormula);

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
                    // System.out.println("[get state returns] i = " + i + "   formula = " + distFormula.name());
                    
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