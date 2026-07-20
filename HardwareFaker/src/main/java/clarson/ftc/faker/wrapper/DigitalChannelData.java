/*
 * DigitalChannelData.java
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

package clarson.ftc.faker.wrapper;

import clarson.ftc.faker.DigitalChannelImplFake;
import clarson.ftc.faker.function.TimedStateGetter;
import clarson.ftc.faker.updater.Updateable;
import clarson.ftc.faker.util.EasyTimedStateGetter;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import java.util.function.BooleanSupplier;

public class DigitalChannelData implements Updateable {
    /**
     * Copies all attributes of the given `DigitalChannelData` into a new 
     * `DigitalChannelData` object whose `actuator` field references the 
     * given channel. This effectively sets the `channel` field of the 
     * data to the channel. 
     * 
     * @param channel The channel to refer to in the new data
     * @param data Where to source the old fields, e.g. `lastState` and `stateGetter`
     * @return A new `DigitalChannelData` reference, with copied field references 
     * except for `channel`. 
     */
    public static DigitalChannelData copyForChannelData(DigitalChannelImplFake channel, DigitalChannelData data) {
        final DigitalChannelData result = new DigitalChannelData(channel);
        result.copyAvailableProperties(data);
        return result;
    }

    // Descriptor Fields - These unchageable fields describe properties of the channel itself
    public final DigitalChannelImplFake channel;
    
    // Other Fields
    // NOTE: The values here are not the *actual* default values. See constructors for details.
    public boolean lastState = false;
    public TimedStateGetter stateGetter = null;
    public DigitalChannel.Mode mode = DigitalChannel.Mode.INPUT;

    /**
     * Constructs a data wrapper with a null `channel` field. `getState()`
     * will always return the last set state. The initial state is set to 
     * false.
     */
    public DigitalChannelData() {
        this((DigitalChannelImplFake) null);
    }

    /**
     * Constructs a data wrapper a null `channel` field. It is assumed to be
     * in the `OUTPUT` state. `getState()` will always return the last set 
     * value.
     * 
     * @param initialState The first value of the `lastState` field.
     */
    public DigitalChannelData(boolean initialState) {
        this(null, initialState);
    }

    /**
     * Constructs a data wrapper a null `channel` field. It is assumed to be
     * in the `INPUT` state. `getState()` will return the value of stateGetter
     * that was obtained after the last call to update. The initial state is set 
     * to lastValue, although this is unlikely to be important
     * 
     * @param stateGetter What supplies the value of `getState()` in the `INPUT` mode
     */
    public DigitalChannelData(BooleanSupplier stateGetter) {
        this(new EasyTimedStateGetter(stateGetter));
    }


    /**
     * Constructs a data wrapper a null `channel` field. It is assumed to be
     * in the `INPUT` state. `getState()` will return the value of stateGetter.
     * The initial state is set to lastValue, although this is unlikely to be 
     * important
     * 
     * @param stateGetter What supplies the value of `getState()` in the `INPUT` mode
     */
    public DigitalChannelData(TimedStateGetter stateGetter) {
        this(null, stateGetter, false, DigitalChannel.Mode.INPUT);
    }

    /**
     * Constructs a data wrapper for the given channel, assuming it to be 
     * in the `OUTPUT` state. `getState()` will always return the last set 
     * state. The initial state is set to false.
     * 
     * @param channel  Value of the `channel` field
     */
    public DigitalChannelData(DigitalChannelImplFake channel) {
        this(channel, false);
    }

    /**
     * Constructs a data wrapper for the given channel, assuming it to be 
     * in the `OUTPUT` state. `getState()` will always return the last set 
     * value.
     * 
     * @param channel Value of the `channel` field
     * @param initialState The first value of the `lastState` field.
     */
    public DigitalChannelData(DigitalChannelImplFake channel, boolean initialState) {
        this(channel, null /* <- Placeholder value */, initialState, DigitalChannel.Mode.OUTPUT);

        // I hate having no flexible constructor bodies.
        this.stateGetter = new TimedStateGetter() {
            public boolean isUpdatingEnabled = true;

            @Override
            public boolean getAsBoolean() {
                return DigitalChannelData.this.lastState;
            }

            @Override
            public double update(double unused) {
                return 0;
            }

            @Override
            public boolean isUpdatingEnabled() {
                return isUpdatingEnabled;
            }

            @Override
            public boolean setUpdatingEnabled(boolean newValue) {
                final boolean oldValue = isUpdatingEnabled;
                this.isUpdatingEnabled = newValue;
                return oldValue != newValue;
            }
        };
    
    }
    
    /**
     * Constructs a data wrapper for the given channel, assuming it to be 
     * in the `INPUT` state. `getState()` will return the value of stateGetter
     * that was obtained after the last call to update. The initial state is set 
     * to lastValue, although this is unlikely to be important.
     * 
     * @param channel Value of the `channel` field
     * @param stateGetter What supplies the value of `getState()` in the `INPUT` mode
     */
    public DigitalChannelData(DigitalChannelImplFake channel, BooleanSupplier stateGetter) {
        this(channel, new EasyTimedStateGetter(stateGetter));
    }

    /**
     * Constructs a data wrapper for the given channel, assuming it to be 
     * in the `INPUT` state. `getState()` will return the value of stateGetter.
     * The initial state is set to lastValue, although this is unlikely to be 
     * important
     * 
     * @param channel Value of the `channel` field
     * @param stateGetter What supplies the value of `getState()` in the `INPUT` mode
     */
    public DigitalChannelData(DigitalChannelImplFake channel, TimedStateGetter stateGetter) {
        this(channel, stateGetter, false, DigitalChannel.Mode.INPUT);
    }

    public DigitalChannelData(
        DigitalChannelImplFake channel, 
        final TimedStateGetter stateGetter, 
        boolean initialState,
        DigitalChannel.Mode mode
    ) {
        this.channel = channel;
        this.lastState = initialState;
        this.mode = mode;
        this.stateGetter = stateGetter;
    }

    /**
     * Queries for the most applicable data. For DigitalChannels in the `INPUT` 
     * mode, this calls the `getState()` method of the state getter (alias for 
     * `getAsBoolean()`). For channels in the `OUTPUT` mode, this gets the value 
     * of `lastState`, which is set by `DigitalChannelControllerFake.setState()`. 
     * 
     * If the mode is `INPUT`, then `lastState` will be set to the return value of
     * this method.
     * 
     * @return The state of the channel. New data when in `INPUT`; last set value 
     * in `OUTPUT`
     */
    public boolean getState() {
        if(this.mode == DigitalChannel.Mode.OUTPUT) {
            return this.lastState;
        }

        return lastState = this.stateGetter.getState();
    }

    /**
     * Sets all settable properties on this `DigitalChannelData` to the 
     * corresponding value in the given data. This, therefore, copies the given 
     * data to this object except for all primitive final properties.
     * 
     * @param source Where to get all values to copy.
     */
    public void copyAvailableProperties(DigitalChannelData source) {
        this.lastState = source.lastState;
        this.stateGetter = source.stateGetter;
        this.mode = source.mode;
    }

    @Override
    public double update(double deltaSec) {
        return stateGetter.update(deltaSec);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return stateGetter.isUpdatingEnabled();
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        return stateGetter.setUpdatingEnabled(newUpdatingEnabled);
    }
}