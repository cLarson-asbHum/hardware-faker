/*
 * EasyColorRangeGetter.java
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

package clarson.ftc.faker.util;

import androidx.annotation.ColorInt;

import clarson.ftc.faker.function.ColorGetter;
import clarson.ftc.faker.function.ColorRangeGetter;
import clarson.ftc.faker.function.DistanceGetter;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntSupplier;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Creates a ColorRangeGetter with a double generator.  
 */
public final class EasyColorRangeGetter extends UpdateableSupplier<EasyColorRangeGetter.ColorRange> implements ColorRangeGetter {
    /**
     * Stores both a distance and a color reading.
     */
    static final record ColorRange(double distance, int red, int green, int blue) {
        static ColorRange fromColorInt(double distance, @ColorInt int color) {
            return new ColorRange(
                distance,
                ColorGetter.uByteToUShort(ColorGetter.parseRed(color)), 
                ColorGetter.uByteToUShort(ColorGetter.parseGreen(color)), 
                ColorGetter.uByteToUShort(ColorGetter.parseBlue(color)) 
            );
        }

        /** 
         * Red, green, and blue should be in the range of [0, 65535], although they will
         * be clamped.
         */
        ColorRange copyWithComponents(int red, int green, int blue) {
            return new ColorRange(this.distance, red & 65535, green & 65535, blue & 65535);
        }
    }

    public static final DistanceUnit DEFAULT_UNIT = EasyTimedDistanceGetter.DEFAULT_UNIT;

    /**
     * Constructs a ColorRangeGetter whose getDistance() and getColor() methods return
     * values obtained from invoking the provided getters. Construction invokes both 
     * getters, and the first return value from each will be used as the return of 
     * the new ColorRangeGetters's getDistance() and getColor() methods until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * The provided getters are independent of time because they do not have a double 
     * parameter. 
     * 
     * The default distance units of the `get()` method are given by the static 
     * `DEFAULT_UNIT` field. The format of the argb value is `0xAARRGGBB`
     * 
     * @param distance What determines the return value of getDistance(). 
     * @param argb What determines the return value of getColor().
     */
    public EasyColorRangeGetter(DistanceGetter distance, @ColorInt IntSupplier argb) {
        this((unit, deltaSec) -> 
            ColorRange.fromColorInt(distance.getDistance(unit), argb.getAsInt()));
    }
    
    /**
     * Constructs a ColorRangeGetter whose getDistance() and getColor() methods return
     * values obtained from invoking the provided getters. Construction invokes both 
     * getters, and the first return value from each will be used as the return of 
     * the new ColorRangeGetters's getDistance() and getColor() methods until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * The provided getters are independent of time because they do not have a double 
     * parameter. 
     * 
     * The default distance units of the `get()` method are given by the static 
     * `DEFAULT_UNIT` field. The format of the argb value is `0xAARRGGBB`
     * 
     * @param distance What determines the return value of getDistance(). 
     * @param argb What determines the return value of getColor().
     */
    public EasyColorRangeGetter(DistanceGetter distance, ColorGetter argb) {
        this((unit, deltaSec) -> new ColorRange(distance.getDistance(unit), 
                argb.redShort(), argb.greenShort(), argb.blueShort()));
    }
    

    /**
     * Constructs a ColorRangeGetter whose getDistance() and getColor() methods return
     * values obtained from invoking the provided getters. Construction invokes both 
     * getters, and the first return value from each will be used as the return of 
     * the new ColorRangeGetters's getDistance() and getColor() methods until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * The default distance units of the `get()` method are given by the static 
     * `DEFAULT_UNIT` field. The format of the argb value is `0xAARRGGBB`
     * 
     * @param distance What determines the return value of getDistance(). Double parameter 
     * is deltaSec
     * @param argb What determines the return value of getColor(). Double parameter 
     * is deltaSec
     */
    public EasyColorRangeGetter(
        BiFunction<DistanceUnit, Double, Double> distance, 
        Function<Double, Integer> argb
    ) {
        this((unit, deltaSec) -> 
            ColorRange.fromColorInt(distance.apply(unit, deltaSec), argb.apply(deltaSec)));
    }
    
    /**
     * Constructs a ColorRangeGetter whose getDistance() and getColor() methods return
     * values obtained from invoking the provided getters. Construction invokes both 
     * getters, and the first return value from each will be used as the return of 
     * the new ColorRangeGetters's getDistance() and getColor() methods until update() is 
     * called succesfully (i.e. updating is enabled and the provided deltaSec argument was 
     * not 0).
     * 
     * The default distance units of the `get()` method are given by the static 
     * `DEFAULT_UNIT` field. The format of the argb value is `0xAARRGGBB`
     * 
     * @param colorRangeGenerator Generates the distance and color readings all at once.
     */
    private EasyColorRangeGetter(BiFunction<DistanceUnit, Double, ColorRange> colorRangeGenerator) {
        super((deltaSec) -> colorRangeGenerator.apply(DEFAULT_UNIT, deltaSec));
    }

    /**
     * Gets the most recent value from the Double generator. Such value is updated whenever 
     * update() is called succesfully (aka return of update() == 1).
     * 
     * @return The last value obtained from the getter provided at construction. 
     */
    @Override
    public double getDistance(DistanceUnit units) {
        return units.fromUnit(DEFAULT_UNIT, this.get().distance() /* <-- Already in the default unit */);
    }

    @Override
    @ColorInt
    public int getColor() {
        final ColorRange cr = this.get();
        return ColorGetter.colorIntFromShorts(cr.red(), cr.green(), cr.blue());
    }

    @Override
    @ColorInt
    public int redShort() {
        return this.get().red();
    }
    
    @Override
    @ColorInt
    public int greenShort() {
        return this.get().green();
    }
    
    @Override
    @ColorInt
    public int blueShort() {
        return this.get().blue();
    }
}