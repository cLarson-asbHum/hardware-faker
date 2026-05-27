package clarson.ftc.faker.test.util;

import clarson.ftc.faker.util.EasyTimedStateGetter;
import clarson.ftc.faker.util.TimedStateGetter;

import static clarson.ftc.faker.test.TestUtil.*;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

class EasyTimedStateGetterUnitTest {
    @DisplayName("Can Construct")
    @Test
    void canConstruct() {
        assertDoesNotThrow(() -> new EasyTimedStateGetter(() -> true));
        assertDoesNotThrow(() -> new EasyTimedStateGetter(() -> false));
        assertDoesNotThrow(() -> new EasyTimedStateGetter(() -> Math.random() < 0.5));

        assertDoesNotThrow(() -> new EasyTimedStateGetter((deltaSec) -> deltaSec < 0.5));
        assertDoesNotThrow(() -> new EasyTimedStateGetter((deltaSec) -> deltaSec == 0));
        assertDoesNotThrow(() -> new EasyTimedStateGetter((deltaSec) -> false));
    }

    private static final boolean UNUSED = false;

    @DisplayName("Initial isUpdating is true")
    @Test
    void initialIsUpdatingIsTrue() {
        for(int i = 0; i < 10; i++) {
            final TimedStateGetter state = new EasyTimedStateGetter(() -> UNUSED);
            assertEquals(true, state.isUpdatingEnabled(), "initial isUpdatingEnabled is true even for the " + i + "-th construction");
        }
    }

    @DisplayName("SetUpdatingEnabled matches with isUpdatingEnabled")
    @Test
    void setUpdatingEnabledMatchesGetter() {
        final TimedStateGetter booleanSupplierState = new EasyTimedStateGetter(() -> UNUSED);
        final TimedStateGetter generatorState = new EasyTimedStateGetter((deltaSec) -> UNUSED);

        booleanSupplierState.setUpdatingEnabled(false);
        generatorState.setUpdatingEnabled(false);
        assertEquals(false, booleanSupplierState.isUpdatingEnabled(), "initial set to false");
        assertEquals(false, generatorState.isUpdatingEnabled(), "initial set to false");


        booleanSupplierState.setUpdatingEnabled(false);
        generatorState.setUpdatingEnabled(false);
        assertEquals(false, booleanSupplierState.isUpdatingEnabled(), "setting to false twice does nothing");
        assertEquals(false, generatorState.isUpdatingEnabled(), "setting to false twice does nothing");


        booleanSupplierState.setUpdatingEnabled(true);
        generatorState.setUpdatingEnabled(true);
        assertEquals(true, booleanSupplierState.isUpdatingEnabled(), "changing to true is good");
        assertEquals(true, generatorState.isUpdatingEnabled(), "changing to true is good");


        assertEquals(true, booleanSupplierState.isUpdatingEnabled(), "not changing anything changes nothing");
        assertEquals(true, generatorState.isUpdatingEnabled(), "not changing anything changes nothing");

        booleanSupplierState.setUpdatingEnabled(false);
        generatorState.setUpdatingEnabled(false);
        assertEquals(false, booleanSupplierState.isUpdatingEnabled(), "going back to false returns false");
        assertEquals(false, generatorState.isUpdatingEnabled(), "going back to false returns false");
    }

    /**
     * Count the number of times it's getAsBoolean() method has been solved
     */
    private class CountingBooleanSupplier implements BooleanSupplier {
        private int totalCalls = 0;

        @Override
        public boolean getAsBoolean() {
            totalCalls++;
            return UNUSED;
        }

        public int getTotalCalls() {
            return this.totalCalls;
        }
    }

    @DisplayName("update accurately respects setUpdatingEnabled")
    @Test
    void updateRespectedSetUpdatingEnabled() {
        final CountingBooleanSupplier supplier = new CountingBooleanSupplier();
        final TimedStateGetter state = new EasyTimedStateGetter(supplier);

        assertEquals(1, supplier.getTotalCalls(), "The supplier is called once by construction");

        // Set updating is true
        state.setUpdatingEnabled(true);
        state.update(1.0); // The specific value is not important, as long as deltaSEc != 0
        assertEquals(2, supplier.getTotalCalls(), "Supplier is called upon (first) update");

        assumeTrue(supplier.getTotalCalls() == 2, "The supplier value deos not change when nothing happens");

        state.update(1.0);
        state.update(1.0);
        state.update(1.0);
        assertEquals(5, supplier.getTotalCalls(), "supplier is still called even when updates are in succesion");

        // Is updating is false
        state.setUpdatingEnabled(false);
        state.update(1.0);
        assertEquals(5, supplier.getTotalCalls(), "supplier is not called on (first) disabled update");
        
        state.update(1.0);
        state.update(1.0);
        state.update(1.0);
        assertEquals(5, supplier.getTotalCalls(), "update does not modify the setUpdatingEnabled state");
    }

    @DisplayName("update returns 0 upon failure and 1 upon success") 
    @Test
    void updateReturns0UponFailureAnd1ForSuccess() {
        final CountingBooleanSupplier supplier = new CountingBooleanSupplier();
        final TimedStateGetter state = new EasyTimedStateGetter(supplier);

        state.setUpdatingEnabled(true);
        assertEquals(0, state.update(0.0), "0 is returned for a failure with a deltaSec of 0");
        assertEquals(1, state.update(3.141), "1 is returned for an enabled postive-time invocation");
        assertEquals(1, state.update(-2.728), "1 is returned for an enabled negative-time  invocation");

        state.setUpdatingEnabled(false);
        assertEquals(0, state.update(0.0), "0.0 seconds on a disabled update returns 0");
        assertEquals(0, state.update(1.0), "Positive seconds on a disabled update also returns 0");
        assertEquals(0, state.update(-1.0), "Negative seconds on a disabled update also returns 0");
    }
}