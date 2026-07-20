/*
 * EasyTimedDistanceGetterUnitTest.java
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

package clarson.ftc.faker.test.util;

import clarson.ftc.faker.function.DistanceGetter;
import clarson.ftc.faker.function.TimedDistanceGetter;
import clarson.ftc.faker.util.EasyTimedDistanceGetter;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static clarson.ftc.faker.test.TestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class EasyTimedDistanceGetterUnitTest {
    @DisplayName("Can Construct")
    @Test
    void canConstruct() {
        assertDoesNotThrow(() -> new EasyTimedDistanceGetter((unused) -> 31.4));
        assertDoesNotThrow(() -> new EasyTimedDistanceGetter((unused) -> 0.0));
        assertDoesNotThrow(() -> new EasyTimedDistanceGetter((unused) -> Math.random()));
        assertDoesNotThrow(() -> new EasyTimedDistanceGetter((units) -> units.fromInches(Math.random())));

        assertDoesNotThrow(() -> new EasyTimedDistanceGetter((unused, deltaSec) -> 0.0));
        assertDoesNotThrow(() -> new EasyTimedDistanceGetter((unused, deltaSec) -> deltaSec));
        assertDoesNotThrow(() -> new EasyTimedDistanceGetter((unused, deltaSec) -> 2 * deltaSec));
        assertDoesNotThrow(() -> new EasyTimedDistanceGetter((units, deltaSec) -> units.fromMm(2 * deltaSec)));
    }

    private static final double UNUSED = 0;

    @DisplayName("Initial isUpdating is true")
    @Test
    void initialIsUpdatingIsTrue() {
        for(int i = 0; i < 10; i++) {
            final TimedDistanceGetter distance = new EasyTimedDistanceGetter((units) -> UNUSED);
            assertEquals(true, distance.isUpdatingEnabled(), "initial isUpdatingEnabled is true even for the " + i + "-th construction");
        }
    }

    @DisplayName("SetUpdatingEnabled matches with isUpdatingEnabled")
    @Test
    void setUpdatingEnabledMatchesGetter() {
        final TimedDistanceGetter distanceGetterDistance = new EasyTimedDistanceGetter((units) -> UNUSED);
        final TimedDistanceGetter generatorDistance = new EasyTimedDistanceGetter((units, deltaSec) -> UNUSED);

        distanceGetterDistance.setUpdatingEnabled(false);
        generatorDistance.setUpdatingEnabled(false);
        assertEquals(false, distanceGetterDistance.isUpdatingEnabled(), "initial set to false");
        assertEquals(false, generatorDistance.isUpdatingEnabled(), "initial set to false");


        distanceGetterDistance.setUpdatingEnabled(false);
        generatorDistance.setUpdatingEnabled(false);
        assertEquals(false, distanceGetterDistance.isUpdatingEnabled(), "setting to false twice does nothing");
        assertEquals(false, generatorDistance.isUpdatingEnabled(), "setting to false twice does nothing");


        distanceGetterDistance.setUpdatingEnabled(true);
        generatorDistance.setUpdatingEnabled(true);
        assertEquals(true, distanceGetterDistance.isUpdatingEnabled(), "changing to true is good");
        assertEquals(true, generatorDistance.isUpdatingEnabled(), "changing to true is good");


        assertEquals(true, distanceGetterDistance.isUpdatingEnabled(), "not changing anything changes nothing");
        assertEquals(true, generatorDistance.isUpdatingEnabled(), "not changing anything changes nothing");

        distanceGetterDistance.setUpdatingEnabled(false);
        generatorDistance.setUpdatingEnabled(false);
        assertEquals(false, distanceGetterDistance.isUpdatingEnabled(), "going back to false returns false");
        assertEquals(false, generatorDistance.isUpdatingEnabled(), "going back to false returns false");
    }

    /**
     * Count the number of times it's getAsBoolean() method has been solved
     */
    private class CountingDistanceGetter implements DistanceGetter {
        private int totalCalls = 0;

        @Override
        public double getDistance(DistanceUnit units) {
            totalCalls++;
            return units.fromInches(UNUSED);
        }

        public int getTotalCalls() {
            return this.totalCalls;
        }
    }

    @DisplayName("update accurately respects setUpdatingEnabled")
    @Test
    void updateRespectedSetUpdatingEnabled() {
        final CountingDistanceGetter supplier = new CountingDistanceGetter();
        final TimedDistanceGetter distance = new EasyTimedDistanceGetter(supplier);

        assertEquals(1, supplier.getTotalCalls(), "The supplier is called once by construction");

        // Set updating is true
        distance.setUpdatingEnabled(true);
        distance.update(1.0); // The specific value is not important, as long as deltaSEc != 0
        assertEquals(2, supplier.getTotalCalls(), "Supplier is called upon (first) update");

        assumeTrue(supplier.getTotalCalls() == 2, "The supplier value deos not change when nothing happens");

        distance.update(1.0);
        distance.update(1.0);
        distance.update(1.0);
        assertEquals(5, supplier.getTotalCalls(), "supplier is still called even when updates are in succesion");

        // Is updating is false
        distance.setUpdatingEnabled(false);
        distance.update(1.0);
        assertEquals(5, supplier.getTotalCalls(), "supplier is not called on (first) disabled update");
        
        distance.update(1.0);
        distance.update(1.0);
        distance.update(1.0);
        assertEquals(5, supplier.getTotalCalls(), "update does not modify the setUpdatingEnabled distance");
    }

    @DisplayName("update returns 0 upon failure and 1 upon success") 
    @Test
    void updateReturns0UponFailureAnd1ForSuccess() {
        final CountingDistanceGetter supplier = new CountingDistanceGetter();
        final TimedDistanceGetter distance = new EasyTimedDistanceGetter(supplier);

        distance.setUpdatingEnabled(true);
        assertEquals(0, distance.update(0.0), "0 is returned for a failure with a deltaSec of 0");
        assertEquals(1, distance.update(3.141), "1 is returned for an enabled postive-time invocation");
        assertEquals(1, distance.update(-2.728), "1 is returned for an enabled negative-time  invocation");

        distance.setUpdatingEnabled(false);
        assertEquals(0, distance.update(0.0), "0.0 seconds on a disabled update returns 0");
        assertEquals(0, distance.update(1.0), "Positive seconds on a disabled update also returns 0");
        assertEquals(0, distance.update(-1.0), "Negative seconds on a disabled update also returns 0");
    }

    @DisplayName("getDistance accurately respects the given units")
    @Test
    void getDistanceRespectsGivenUnits() {
        // The getter's units are specified by EasyTimedDistanceGetter.DEFAULT_UNIT
        final DistanceGetter getter = (units) -> units.fromInches(1);
        final TimedDistanceGetter timed = new EasyTimedDistanceGetter(getter);

        final double inDefault = timed.getDistance(EasyTimedDistanceGetter.DEFAULT_UNIT);
        final double inInch    = timed.getDistance(DistanceUnit.INCH);
        final double inMm      = timed.getDistance(DistanceUnit.MM);
        final double inCm      = timed.getDistance(DistanceUnit.CM);
        final double inMeter   = timed.getDistance(DistanceUnit.METER);

        assumeTrue(EasyTimedDistanceGetter.DEFAULT_UNIT == DistanceUnit.INCH, "The default units are inches");

        // assertNotEquals(inDefault, inInch, "Converts from default to inches");
        assertNotEquals(inDefault, inMm,    "Converts from default to mm");
        assertNotEquals(inDefault, inCm,      "Converts from default to cm");
        assertNotEquals(inDefault, inMeter,  "Converts from default to meters");
        
        assertEquals(1     * inDefault, inInch,  "Converts from default to inches");
        assertEquals(25.4  * inDefault, inMm,    "Converts from default to mm");
        assertEquals(2.54  * inDefault, inCm,    "Converts from default to cm");
        assertEquals(.0254 * inDefault, inMeter, "Converts from default to meters");
    }
}