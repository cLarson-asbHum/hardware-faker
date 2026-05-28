package clarson.ftc.faker.test;


import clarson.ftc.faker.DigitalChannelControllerFake;
import clarson.ftc.faker.DigitalChannelImplFake;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.updater.Updateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.AbstractTwoWayUpdateable;
import clarson.ftc.faker.util.EasyTimedStateGetter;
import clarson.ftc.faker.util.TimedStateGetter;
import clarson.ftc.faker.util.UpdateableSupplier;
import clarson.ftc.faker.wrapper.DigitalChannelData;

import static clarson.ftc.faker.test.TestUtil.*;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

import static java.util.concurrent.TimeUnit.SECONDS;

class DigitalChannelImplFakeUnitTest {
    @DisplayName("Can Construct")
    @Test
    void canConstruct() {
        assertDoesNotThrow(() -> new DigitalChannelImplFake());
        assertDoesNotThrow(() -> new DigitalChannelImplFake(false));
        assertDoesNotThrow(() -> new DigitalChannelImplFake(true));
        assertDoesNotThrow(() -> new DigitalChannelImplFake()); // Just checking again

        assertDoesNotThrow(() -> new DigitalChannelImplFake(() -> true));
        assertDoesNotThrow(() -> new DigitalChannelImplFake(() -> false));
        assertDoesNotThrow(() -> new DigitalChannelImplFake(() -> Math.random() < 0.5));
    }

    @DisplayName("Initial Values are Correct")
    @Test
    void intialValuesAreCorrect() {
        final DigitalChannelImplFake channel1 = new DigitalChannelImplFake();
        assertEquals(false, channel1.getState());
        assertEquals(DigitalChannel.Mode.OUTPUT, channel1.getMode());

        final DigitalChannelImplFake channel2 = new DigitalChannelImplFake(true);
        assertEquals(true, channel2.getState());
        assertEquals(DigitalChannel.Mode.OUTPUT, channel2.getMode());
        

        final DigitalChannelImplFake channel3 = new DigitalChannelImplFake(() -> true);
        assertEquals(true, channel3.getState());
        assertEquals(DigitalChannel.Mode.INPUT, channel3.getMode());
    }
    

    @DisplayName("Set mode = getMode")
    @Test
    void setModeEqualsGetMode() {
        final DigitalChannelImplFake channel = new DigitalChannelImplFake();
        final DigitalChannelData data = channel.getData();

        // Input first
        channel.setMode(DigitalChannel.Mode.INPUT);

        assertEquals(DigitalChannel.Mode.INPUT, data.mode, "data wrapper mode is input");
        assertEquals(DigitalChannel.Mode.INPUT, channel.getMode(), "getMode is input");
        
        // Output first
        channel.setMode(DigitalChannel.Mode.OUTPUT);

        assertEquals(DigitalChannel.Mode.OUTPUT, data.mode, "data wrapper mode is output");
        assertEquals(DigitalChannel.Mode.OUTPUT, channel.getMode(), "getMode is output");
        
        // Trying to set it to the same thing, just in case
        channel.setMode(DigitalChannel.Mode.OUTPUT);

        assertEquals(DigitalChannel.Mode.OUTPUT, data.mode, "data wrapper mode stays same");
        assertEquals(DigitalChannel.Mode.OUTPUT, channel.getMode(), "getMode stays same");
        
        // Trying to set it back to original
        channel.setMode(DigitalChannel.Mode.INPUT);

        assertEquals(DigitalChannel.Mode.INPUT, data.mode, "data wrapper mode goes back");
        assertEquals(DigitalChannel.Mode.INPUT, channel.getMode(), "getMode goes back");
    }
    

    // Note to self: OUTPUT is like a SwitchableLight: it is the output relative to 
    //               the user program. Similarly, INPUT is input relative to the user
    //               program, e.g. a touch sensor (aka a button).
    @ParameterizedClass
    @ValueSource(booleans = { false, true })
    @Nested
    class OutputChannels {
        private DigitalChannelImplFake channel;
        private DigitalChannelData data;
        private DigitalChannelControllerFake controller;

        @Parameter
        private boolean initialState;

        @BeforeEach
        void construct() {
            // Creating the contoller
            try {
                controller = new DigitalChannelControllerFake();
            } catch(RobotCoreException | InterruptedException exc) {
                fail(exc);
            }
        

            // Constructing the channel and getting its data wrapper
            channel = new DigitalChannelImplFake(
                new DigitalChannelData(initialState), // Implied Mode.OUTPUT
                controller
            );
            data = channel.getData();

            // Verifying that the mode is output and the state is correct
            if(data.mode != DigitalChannel.Mode.OUTPUT) {
                fail("Data wrapper mode was not OUTPUT");
            }

            if(data.lastState != initialState) {
                fail("Data wrapper initial state was not " + initialState);
            }
        }

        @DisplayName("setSTate updates the lastState property in the data wrapper")
        @Test
        void outputSetUpdatesLastState() {
            channel.setState(false);
            assertEquals(false, data.lastState);
            
            channel.setState(false);
            assertEquals(false, data.lastState);

            channel.setState(true);
            assertEquals(true, data.lastState);

            channel.setState(false);
            assertEquals(false, data.lastState);

            channel.setState(true);
            assertEquals(true, data.lastState);
        }

        @DisplayName("getState returns the last state set by setState")
        @Test
        void getStateReturnsTheLastState() {
            channel.setState(false);
            assertEquals(false, channel.getState());
            
            channel.setState(false);
            assertEquals(false, channel.getState());

            channel.setState(true);
            assertEquals(true, channel.getState());

            channel.setState(false);
            assertEquals(false, channel.getState());

            channel.setState(true);
            assertEquals(true, channel.getState());
        }
    }

    private static enum InputFormula {
        ALWAYS_TRUE,  // Always returns true
        ALWAYS_FALSE, // Always returns false
        ALTERNATE,    // STarts at true, and after updtaing, alternates between false and true
        CALL_NUMBER_IS_POWER_OF_TWO; // Starts at 0 (false), goes to 1 (true) on update, then 2 (true), then 3 (false), ...
    }

    @ParameterizedClass
    @EnumSource(InputFormula.class)
    @Nested
    class InputChannels {
        private DigitalChannelImplFake channel;
        private DigitalChannelData data;
        private DigitalChannelControllerFake controller;

        @Parameter
        private InputFormula inputFormula;

        static TimedStateGetter getSupplier(InputFormula inputFormula) {
            switch (inputFormula) {
                case ALWAYS_TRUE:
                    return new EasyTimedStateGetter(() -> true);
                    
                case ALWAYS_FALSE:
                    return new EasyTimedStateGetter(() -> false);

                case ALTERNATE:
                    return new EasyTimedStateGetter(new BooleanSupplier() {
                        private boolean lastState = false;

                        @Override
                        public boolean getAsBoolean() {
                            lastState = !lastState;
                            return lastState;
                        }

                    });

                case CALL_NUMBER_IS_POWER_OF_TWO:
                    return new EasyTimedStateGetter(new BooleanSupplier() {
                        private int totalCalls = 0;

                        @Override
                        public boolean getAsBoolean() {
                            if(totalCalls == 0) {
                                return false;
                            }

                            // Getting it down to having no ending zeros
                            int last = totalCalls;
                            for(int i = 0; i < 32 && (last & 1) == 0; i++) {
                                last = last >>> 1;
                            }

                            totalCalls++;
                            return last == 1;
                        }
                    });

                default:
                    fail("Unregonized inputFormula \"" + inputFormula.name() + "\"");
                    return null;
            }
        }

        @BeforeEach
        void constructChannel() {
            // Creating the contoller
            try {
                controller = new DigitalChannelControllerFake();
            } catch(RobotCoreException | InterruptedException exc) {
                fail(exc);
            }
        

            // Constructing the channel and getting its data wrapper
            channel = new DigitalChannelImplFake(
                new DigitalChannelData(getSupplier(inputFormula)),
                controller
            );
            data = channel.getData();

            // Verifying that the mode is output and the state is correct
            if(data.mode != DigitalChannel.Mode.INPUT) {
                fail("Data wrapper mode was not INPUT");
            }
        }
    
        @DisplayName("getState returns the expected value only after update")
        @Test
        void getSTateReturnsExpectedValueOnlyOnUpdate() {
            // NOTE: All the input formulas only get new values after updating
            //       This is the intended usage, as otherwise a channel gets a new value every
            //       call to getState(), which does make as much sense.

            // NOTE: The channel is not registered with an updater, and so update() must be called manually
            //       Were it connected to an updater, it would update automatically. A separate test 
            //       exists for this.
            final TimedStateGetter expectedSupplier = getSupplier(inputFormula);

            final boolean expectedInit = expectedSupplier.getState();
            assertEquals(expectedInit, channel.getState(), "Expected initial value is correct");
            assertEquals(expectedInit, channel.getState(), "Initial value doesn't change when no update called");

            System.out.println();
            for(int i = 1; i < 6; i++) {
                assumeTrue(0.0 != channel.update(1.0), "The Channel did update");
                assumeTrue(0.0 != expectedSupplier.update(1.0), "The Supplier did update");

                final boolean expectedValue = expectedSupplier.getState();
                System.out.println("[get state returns] i = " + i + "   formula = " + inputFormula.name());
                assertEquals(expectedValue, channel.getState(), i + "-th value matches expected");
                assertEquals(expectedValue, channel.getState(), i + "-th value did not change");
            }
        }

    }

    @Nested
    class UpdaterDependent {
        private ModularUpdater updater = new ModularUpdater();
        private DigitalChannelImplFake channel;
        private DigitalChannelData data;
        private DigitalChannelControllerFake controller;

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
            controller = new DigitalChannelControllerFake(lynx);
        

            // Constructing the channel and getting its data wrapper
            channel = new DigitalChannelImplFake(new DigitalChannelData(), controller);
            data = channel.getData();

            lynxUsb.setDigitalChannels(new DigitalChannelImplFake[] {
                channel, null, null, null, null,null,null, null
            });
        }

        @BeforeEach
        void registerWithUpdater() {
            construct();
            
            if(controller.getLynxModule() == null) {
                fail("Controller LynxModule was null");
            }

            updater.register(channel, controller.getLynxModule());
            updater.register(counter, controller.getLynxModule());
        }

        @DisplayName("setUpdatingEnabled and isUpdatingEnabled match")
        @Test
        void setAndGetUpdatingEnabledMatch() {
            channel.setUpdatingEnabled(false);
            assertEquals(false, channel.isUpdatingEnabled());
            
            channel.setUpdatingEnabled(false);
            assertEquals(false, channel.isUpdatingEnabled());

            channel.setUpdatingEnabled(true);
            assertEquals(true, channel.isUpdatingEnabled());

            channel.setUpdatingEnabled(false);
            assertEquals(false, channel.isUpdatingEnabled());

            channel.setUpdatingEnabled(true);
            assertEquals(true, channel.isUpdatingEnabled());
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
            final DigitalChannelImplFake counterchannel = new DigitalChannelImplFake(
                new DigitalChannelData(new EasyTimedStateGetter((deltaTime) -> {
                    // NOTE: updating external Updateables in an update() method is ***strongly*** discouraged.
                    //       We do it here only because we need to check when something is udpated.
                    counter.update(deltaTime);
                    return false; // unused
                })),
                controller
            );
            assertEquals(1 - 1, counter.getTotalCalls(), "The counter is called once by construction");

            // Set updating is true
            counterchannel.setUpdatingEnabled(true);
            counterchannel.update(1.0); // The specific value is not important, as long as deltaSEc != 0
            assertEquals(2 - 1, counter.getTotalCalls(), "Counter is called upon (first) update");

            assumeTrue(counter.getTotalCalls() == 1, "The counter value deos not change when nothing happens");

            counterchannel.update(1.0);
            counterchannel.update(1.0);
            counterchannel.update(1.0);
            assertEquals(5 - 1, counter.getTotalCalls(), "counter is still called even when updates are in succesion");

            // Is updating is false
            counterchannel.setUpdatingEnabled(false);
            counterchannel.update(1.0);
            assertEquals(5 - 1, counter.getTotalCalls(), "counter is not called on (first) disabled update");
            
            counterchannel.update(1.0);
            counterchannel.update(1.0);
            counterchannel.update(1.0);
            assertEquals(5 - 1, counter.getTotalCalls(), "update does not modify the setUpdatingEnabled counterchannel");
        }

        @DisplayName("forget removes updater internally; remember adds it back")
        @Test
        void forgetRemovesRememberAdds() {
            channel.setState(false /* unused */);
            assertEquals(1, counter.getTotalUpdates(), "is registered initially");
            
            channel.forget(updater);
            channel.setState(false /* unused */);
            assertEquals(1, counter.getTotalUpdates(), "updater no longer updated by channel");

            channel.setState(false /* unused */);
            channel.setState(false /* unused */);
            channel.setState(false /* unused */);
            assertEquals(1, counter.getTotalUpdates(), "updater still no longer updated by channel");

            channel.remember(updater);
            channel.setState(false /* unused */);
            assertEquals(2, counter.getTotalUpdates(), "updater now is updated by channel");
            
            channel.setState(false /* unused */);
            channel.setState(false /* unused */);
            channel.setState(false /* unused */);
            assertEquals(5, counter.getTotalUpdates(), "updater still is updated by channel");
        }

        @DisplayName("getState updates automatically when registered with an updater")
        @Test
        void getStateUpdatesAutomaticallyWhenRegistered() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedStateGetter expectedSupplier = InputChannels.getSupplier(InputFormula.ALTERNATE);
            final DigitalChannelImplFake inputChannel = new DigitalChannelImplFake(
                new DigitalChannelData(InputChannels.getSupplier(InputFormula.ALTERNATE)),
                controller
            );

            updater.register(inputChannel, inputChannel.getController().getLynxModule());
            
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length), "The Supplier did update");
            assertEquals(expectedSupplier.getState(), inputChannel.getState(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());

            expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
            final boolean secondState = inputChannel.getState();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getState(), secondState, "Value did change without needing an update");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != inputChannel.update(1.0), "The Channel did update");
                // System.out.println("[get state returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getState(), inputChannel.getState(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getState(), inputChannel.getState(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }

        @DisplayName("setState delays the simulation by the correct amount of time")
        @Test
        void setStateDelaysByTheCorrectTime() {
            channel.setMode(DigitalChannel.Mode.OUTPUT);
            channel.setState(false);
            assertEquals(Updater.UpdateDelaySource.DIGITAL.length, counter.getLastDeltaSec(), "false delta sec digital");

            channel.setState(false);
            channel.setState(false);
            channel.setState(false);
            assertEquals(Updater.UpdateDelaySource.DIGITAL.length, counter.getLastDeltaSec(), "same set delta sec digital");
            
            channel.setState(true);
            assertEquals(Updater.UpdateDelaySource.DIGITAL.length, counter.getLastDeltaSec(), "true set delta sec digital");
            
            channel.setState(true);
            channel.setState(true);
            channel.setState(true);
            assertEquals(Updater.UpdateDelaySource.DIGITAL.length, counter.getLastDeltaSec(), "same set delta sec digital");

            channel.setState(false);
            assertEquals(Updater.UpdateDelaySource.DIGITAL.length, counter.getLastDeltaSec(), "change delta sec digital");

        }

        @DisplayName("getState delays always when bulk caching is off")
        @Test
        void getStateDelaysWhenBulkCachingDisabled() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedStateGetter expectedSupplier = InputChannels.getSupplier(InputFormula.ALTERNATE);
            final DigitalChannelImplFake inputChannel = new DigitalChannelImplFake(
                new DigitalChannelData(InputChannels.getSupplier(InputFormula.ALTERNATE)),
                controller
            );

            updater.register(inputChannel, inputChannel.getController().getLynxModule());
            
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length), "The Supplier did update");
            assertEquals(expectedSupplier.getState(), inputChannel.getState(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());

            expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
            final boolean secondState = inputChannel.getState();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getState(), secondState, "Value did change without needing an update");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != inputChannel.update(1.0), "The Channel did update");
                // System.out.println("[get state returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getState(), inputChannel.getState(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getState(), inputChannel.getState(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getState delays (the correct time) only on clear cache in MANUAL")
        @Test
        void getStateDelaysOnlyWhenClearManual() {
            System.out.println("=============================");
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedStateGetter expectedSupplier = InputChannels.getSupplier(InputFormula.ALTERNATE);
            final DigitalChannelImplFake inputChannel = new DigitalChannelImplFake(
                new DigitalChannelData(InputChannels.getSupplier(InputFormula.ALTERNATE)),
                controller
            );
            
            updater.register(inputChannel, lynx);
            lynxUsb.setDigitalChannels(new DigitalChannelImplFake[] {
                channel, inputChannel, null, null, null,null,null, null
            });
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

            counter.clearCount();
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length), "The Supplier did update");
            assertEquals(expectedSupplier.getState(), inputChannel.getState(), "Expected initial value is correct");
            // Updater.updateAllOnce(Set.of(updater), Updater.UpdateDelaySource.DIGITAL);
            System.out.println("=============================");
            assertEquals(1, counter.getTotalUpdates());

            assertEquals(expectedSupplier.getState(), inputChannel.getState(), "Expected initial value didnt change");
            assertEquals(1, counter.getTotalUpdates());

            lynx.clearBulkCache();
            expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
            final boolean secondState = inputChannel.getState();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getState(), secondState, "Value did change after clearing cache");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != inputChannel.update(1.0), "The Channel did update");
                // System.out.println("[get state returns] i = " + i + "   formula = " + inputFormula.name());
                
                lynx.clearBulkCache();
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getState(), inputChannel.getState(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals(expectedSupplier.getState(), inputChannel.getState(), i + "-th value did not change");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                lynx.clearBulkCache();
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getState(), inputChannel.getState(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
        
        @DisplayName("getState delays only on repeat invokation in AUTOMATIC")
        @Test
        void getStateDelaysOnlyWhenRepeatedInAuto() {
            // Note that the expected supplier is NOTE registered with the updater, and thus must be updated manually
            final TimedStateGetter expectedSupplier = InputChannels.getSupplier(InputFormula.ALTERNATE);
            final DigitalChannelImplFake inputChannel1 = new DigitalChannelImplFake(
                new DigitalChannelData(InputChannels.getSupplier(InputFormula.ALTERNATE)),
                controller
            );
            final DigitalChannelImplFake inputChannel2 = new DigitalChannelImplFake(
                new DigitalChannelData(InputChannels.getSupplier(InputFormula.ALTERNATE)),
                controller
            );
            
            updater.register(inputChannel1, lynx);
            updater.register(inputChannel2, lynx);
            lynxUsb.setDigitalChannels(new DigitalChannelImplFake[] {
                channel, inputChannel1, inputChannel2, null, null,null,null, null
            });
            lynx.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);

            counter.clearCount();
            System.out.println("*****************************");
            assumeTrue(0.0 != expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length), "The Supplier did update");
            assertEquals(expectedSupplier.getState(), inputChannel1.getState(), "Expected initial value is correct");
            assertEquals(1, counter.getTotalUpdates());
            
            final boolean secondState = inputChannel2.getState();
            assertEquals(1, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getState(), secondState, "Value not did change after using other method");

            expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
            final boolean thirdState = inputChannel1.getState();
            System.out.println("*****************************");
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getState(), thirdState, "Value did change after repeating");

            final boolean fourthState = inputChannel2.getState();
            assertEquals(2, counter.getTotalUpdates());
            assertEquals(expectedSupplier.getState(), fourthState, "Value not did change after using other method");

            System.out.println();
            for(int i = 2; i < 7; i++) {
                // assumeTrue(0.0 != inputChannel1.update(1.0), "The Channel did update");
                // System.out.println("[get state returns] i = " + i + "   formula = " + inputFormula.name());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getState(), inputChannel1.getState(), i + "-th value matches expected");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                assertEquals(expectedSupplier.getState(), inputChannel2.getState(), i + "-th value did not change");
                assertEquals(1 + 2 * (i - 1), counter.getTotalUpdates());
                
                expectedSupplier.update(Updater.UpdateDelaySource.DIGITAL.length);
                assertEquals(expectedSupplier.getState(), inputChannel1.getState(), i + "-th value did change");
                assertEquals(2 + 2 * (i - 1), counter.getTotalUpdates());
            }
        }
    }
}
