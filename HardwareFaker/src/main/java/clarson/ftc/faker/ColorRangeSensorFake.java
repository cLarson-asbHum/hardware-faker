/*
 * ColorRangeSensorFake.java
 * 
 * This source file contains methods which are under the copyright ownership of 
 * REV Robotics LLC. Such methods are subject to the BSD 3-clause license.
 * 
 * Except where noted, this file is subject to the following license:
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

import androidx.annotation.ColorInt;

import clarson.ftc.faker.function.ColorGetter;
import clarson.ftc.faker.function.ColorRangeGetter;
import clarson.ftc.faker.function.DistanceGetter;
import clarson.ftc.faker.updater.SimulateDelay;
import clarson.ftc.faker.updater.TwoWayUpdateable;
import clarson.ftc.faker.updater.Updater;
import clarson.ftc.faker.util.NoImplementationError;
import clarson.ftc.faker.wrapper.SensorFakeBase;

import static clarson.ftc.faker.updater.UpdatesWhen.ALWAYS;
import static clarson.ftc.faker.updater.UpdatesWhen.CONDITIONAL;

import com.qualcomm.robotcore.hardware.ColorRangeSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.SwitchableLight;
import com.qualcomm.robotcore.util.Range;

import java.util.Set;
import java.util.HashSet;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class ColorRangeSensorFake extends SensorFakeBase<ColorRangeGetter> implements ColorRangeSensor, TwoWayUpdateable {
    private final Set<Updater> rememberedUpdaters = new HashSet<>();
    private final NormalizedRGBA colors = new NormalizedRGBA();
    private final SwitchableLight light;
    private double softwareGain = 1.0;

    public ColorRangeSensorFake(DistanceGetter distanceGetter, ColorGetter colorGetter) {
        this(new ColorRangeGetter() {
            private boolean isEnabled = true;

            @Override
            public double getDistance(DistanceUnit unit) {
                return distanceGetter.getDistance(unit);
            }

            @Override
            public int getColor() {
                return colorGetter.getColor();
            }

            @Override
            public boolean isUpdatingEnabled() {
                return isEnabled;
            }

            @Override
            public boolean setUpdatingEnabled(boolean newValue) {
                final boolean oldValue = isEnabled;
                isEnabled = newValue;
                return oldValue != newValue;
            }

            @Override
            public double update(double deltaSec) {
                return 0;
            }
        });
    }

    public ColorRangeSensorFake(ColorRangeGetter crGetter) {
        this(crGetter, new LEDFake());
    }

    public ColorRangeSensorFake(ColorRangeGetter crGetter, SwitchableLight light) {
        super(crGetter);
        this.light = light;
    }

    @Override
    public I2cAddr getI2cAddress() {
        return null;
    }

    @Override
    public void setI2cAddress(I2cAddr newAddress) {
        // ...and I said, "Your majesty, you cannot whip-the-nay-nay"
    }

    /**
     * Turns on or off the led on this color range sensor. This method simualtes
     * delay only when the light changes state; that is, it either goes from on 
     * to off, or from off to on.
     * 
     * If a SwitchableLight was provided at construction time, it's `enableLight()`
     * method will be invoked only when the status of the light changes.
     * 
     * NOTE: Any delay simulated by the switchable light will be simulated in
     * addition to the delay from this method. 
     * 
     * @param shouldBeEnabled - True for on; false for off
     */
    @SimulateDelay(CONDITIONAL)
    @Override
    public void enableLed(boolean shouldBeEnabled) {
        final boolean wasEnabled = light.isLightOn();

        if(shouldBeEnabled != wasEnabled) {
            Updater.updateAllOnce(rememberedUpdaters, Updater.UpdateDelaySource.I2C);
            light.enableLight(shouldBeEnabled);
        }
    }

    @SimulateDelay(ALWAYS)
    @Override
    @ColorInt
    public int argb() {
        Updater.updateAllOnce(rememberedUpdaters, Updater.UpdateDelaySource.I2C);
        return super.underlyingGetter.getColor();
    }

    protected enum ColorByte {
        ALPHA(3),
        RED(2),
        GREEN(1),
        BLUE(0);

        public final int bitIndex;

        private ColorByte(int byteIndex) {
            this.bitIndex = (8 * byteIndex);
        }
    }

    /**
     * Gets the given component of the given ARGB color specified by a byte index.
     * The byteIndex is treated little-endian, so byteIndex 0 gets blue, 1 gets 
     * green, 2 red, and 3 alpha. Byte indices outside the range [0, 3] will throw 
     * an exception 
     * 
     * @param argb The Android color int to parse the component from
     * @param byteIndex What component to get. See above for detail.
     * @return The componet of the given color. Guaranteed to be in range [0, 255].
     */
    protected int parseColorComponent(@ColorInt int argb, ColorByte colorByte) {
        final int mask = 0xff << colorByte.bitIndex;
        return (argb & mask) >> colorByte.bitIndex;
    }

    @SimulateDelay(ALWAYS)
    @Override
    public int alpha() {
        return parseColorComponent(argb(), ColorByte.ALPHA);
    }

    @SimulateDelay(ALWAYS)
    @Override
    public int red() {
        return parseColorComponent(argb(), ColorByte.RED);
    }

    @SimulateDelay(ALWAYS)
    @Override
    public int green() {
        return parseColorComponent(argb(), ColorByte.GREEN);
    }

    @SimulateDelay(ALWAYS)
    @Override
    public int blue() {
        return parseColorComponent(argb(), ColorByte.BLUE);
    }

    @Override
    public float getGain() {
        return (float) softwareGain;
    }

    /*
     * The following method, "getNormalizedColors" contains code modified from the 
     * original source code (see NOTICE) and is subject to the following terms:
     * 
     * Copyright (c) 2019 REV Robotics LLC
     *
     * All rights reserved.
     *
     * Redistribution and use in source and binary forms, with or without modification,
     * are permitted (subject to the limitations in the disclaimer below) provided that
     * the following conditions are met:
     *
     * Redistributions of source code must retain the above copyright notice, this list
     * of conditions and the following disclaimer.
     *
     * Redistributions in binary form must reproduce the above copyright notice, this
     * list of conditions and the following disclaimer in the documentation and/or
     * other materials provided with the distribution.
     *
     * Neither the name of REV Robotics LLC nor the names of his contributors may be used to
     * endorse or promote products derived from this software without specific prior
     * written permission.
     *
     * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
     * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
     * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
     * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
     * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
     * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
     * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
     * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
     * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
     * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
     * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
     */
    @SimulateDelay(ALWAYS)
    @Override
    public NormalizedRGBA getNormalizedColors() {
        final int argb = argb(); // Simulates delay
        final int red = parseColorComponent(argb, ColorByte.RED);
        final int green = parseColorComponent(argb, ColorByte.GREEN);
        final int blue = parseColorComponent(argb, ColorByte.BLUE);

        this.colors.red   = Range.clip((float) (red   * this.softwareGain), 0f, 1f);
        this.colors.green = Range.clip((float) (green * this.softwareGain), 0f, 1f);
        this.colors.blue  = Range.clip((float) (blue  * this.softwareGain), 0f, 1f);      
        return this.colors;
    }

    @Override
    public void setGain(float newGain) {
        this.softwareGain = newGain;
    }

    @SimulateDelay(ALWAYS)
    @Override
    public double getLightDetected() {
        throw new NoImplementationError("Method ColorRangeSensor.getLightDetected() not currently implemented");
    }

    @SimulateDelay(ALWAYS)
    @Override
    public double getRawLightDetected() {
        throw new NoImplementationError("Method ColorRangeSensor.getRawLightDetected() not currently implemented");
    }

    @Override
    public double getRawLightDetectedMax() {
        throw new NoImplementationError("Method ColorRangeSensor.getRawLightDetectedMax() not currently implemented");
        // return maxLight;
    }

    @Override
    public String getDeviceName() {
        return "ColorRangeSensorFake";
    }

    @Override
    public String status() {
        return getDeviceName(); 
    }

    @SimulateDelay(ALWAYS)
    @Override
    public double getDistance(DistanceUnit units) {
        Updater.updateAllOnce(rememberedUpdaters, Updater.UpdateDelaySource.I2C);
        return underlyingGetter.getDistance(units);
    }

    @Override
    public void remember(Updater updater) {
        rememberedUpdaters.add(updater);
    } 

    @Override
    public void forget(Updater updater) {
        rememberedUpdaters.remove(updater);
    }
}