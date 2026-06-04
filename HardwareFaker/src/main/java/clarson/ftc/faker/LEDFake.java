/*
 * LEDFake.java
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

package clarson.ftc.faker;

import java.util.HashSet;

import com.qualcomm.robotcore.hardware.LED;

import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.wrapper.DigitalChannelData;
import clarson.ftc.faker.DigitalChannelImplFake;

import java.util.HashSet;
import java.util.Set;

public class LEDFake extends LED implements TwoWayUpdateable {
    public static final boolean DEFAULT_INITIALLY_ON = false;

    private static DigitalChannelImplFake lastChannel = null;
    private final DigitalChannelImplFake channel;
    
    public LEDFake() {
        this(new DigitalChannelImplFake());
    }

    public LEDFake(DigitalChannelControllerFake controller) {
        this(new DigitalChannelImplFake(new DigitalChannelData(DEFAULT_INITIALLY_ON), controller));
    }

    public LEDFake(DigitalChannelControllerFake controller, int port) {
        this(controller, port, DEFAULT_INITIALLY_ON);
    }
    
    public LEDFake(DigitalChannelControllerFake controller, int port, boolean isInitiallyOn) {
        this(new DigitalChannelImplFake(
            new DigitalChannelData(isInitiallyOn), 
            controller, 
            port
        ));
    }

    private LEDFake(DigitalChannelImplFake newChannel) {
        super(newChannel.getController(), newChannel.getPortNumber());
        this.channel = newChannel;
    }

    @Override
    public void remember(Updater updater) {
        channel.remember(updater);
    }

    @Override
    public void forget(Updater updater) {
        channel.forget(updater);
    }

    @Override
    public boolean setUpdatingEnabled(boolean newUpdatingEnabled) {
        return channel.setUpdatingEnabled(newUpdatingEnabled);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return channel.isUpdatingEnabled();
    }

    @Override
    public double update(double deltaSec) {
        return channel.update(deltaSec);
    }
}