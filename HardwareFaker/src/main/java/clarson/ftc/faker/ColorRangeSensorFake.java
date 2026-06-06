/*
 * ColorRangeSensorFake.java
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

    @SimulateDelay(ALWAYS)
    @Override
    public NormalizedRGBA getNormalizedColors() {
        final int argb = argb(); // Simulates delay
        final int red = parseColorComponent(argb, ColorByte.RED);
        final int green = parseColorComponent(argb, ColorByte.GREEN);
        final int blue = parseColorComponent(argb, ColorByte.BLUE);

        // The following is taken and modified from REV Robotics's 
        // com.qualcomm.hardware.broadcom.BroadcomColorSensorImpl.java class
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