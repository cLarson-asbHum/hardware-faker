/*
 * ColorGetter.java
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

package clarson.ftc.faker.function;

import java.util.function.IntSupplier;

import androidx.annotation.ColorInt;


@FunctionalInterface
public interface ColorGetter extends IntSupplier {
    /**
     * Gets an RGBA color whose hex matches the form `0x00RRGGBB` (written here 
     * in big endian). While alpha can be returned (bits 24-31), it will be 
     * ignored when used in color sensors.
     * 
     * @return An Android color int, which is an RGBA color in form `0xAARRGGBB`
     * (`AA` is ignored)
     */
    @ColorInt
    public int getColor();

    /**
     * Alias for getColor()
     */
    @Override
    default int getAsInt() {
        return this.getColor();
    }

    /** 
     * Gets the red component of a color as an unsigned short. When max-ed out, this
     * should return 65535 (full-saturation red), and should read 0 when bottomed out
     * (such as when reading black).
     * 
     * By default, this simply parses the red component from `argb()` and scales to 
     * the correct range; however, implementors may find it more useful to override
     * the redShort(), greenShort(), and/or blueShort() methods directly, as these 
     * provide greater precision (8 more bits per component than getColor() provides).
     * 
     * @return The red value of the sensor, as a whole number in range [0, 65535].
     */
    default int redShort() {
        return uByteToUShort(parseRed(this.getColor()));
    }

    /** 
     * Gets the green component of a color as an unsigned short. When max-ed out, this
     * should return 65535 (full-saturation green), and should read 0 when bottomed out
     * (such as when reading black).
     * 
     * By default, this simply parses the green component from `argb()` and scales to 
     * the correct range; however, implementors may find it more useful to override
     * the redShort(), greenShort(), and/or blueShort() methods directly, as these 
     * provide greater precision (8 more bits per component than getColor() provides).
     * 
     * @return The green value of the sensor, as a whole number in range [0, 65535].
     */
    default int greenShort() {
        return uByteToUShort(parseGreen(this.getColor()));
    }

    /** 
     * Gets the blue component of a color as an unsigned short. When max-ed out, this
     * should return 65535 (full-saturation blue), and should read 0 when bottomed out
     * (such as when reading black).
     * 
     * By default, this simply parses the blue component from `argb()` and scales to 
     * the correct range; however, implementors may find it more useful to override
     * the redShort(), greenShort(), and/or blueShort() methods directly, as these 
     * provide greater precision (8 more bits per component than getColor() provides).
     * 
     * @return The blue value of the sensor, as a whole number in range [0, 65535].
     */
    default int blueShort() {
        return uByteToUShort(parseBlue(this.getColor()));
    }

    /** 
     * Gets the red component of a color as an unsigned short. When max-ed out, this
     * should return 255 (full-saturation red), and should read 0 when bottomed out
     * (such as when reading black).
     * 
     * By default, this simply scales the return value of redShort() from the range
     * [0, 65535] to the range [0, 255].
     * 
     * @return The red value of the sensor, as a whole number in range [0, 255].
     */
    default int redByte() {
        return uShortToUByte(this.redShort());
    }

    /** 
     * Gets the green component of a color as an unsigned short. When max-ed out, this
     * should return 255 (full-saturation green), and should read 0 when bottomed out
     * (such as when reading black).
     * 
     * By default, this simply scales the return value of greenShort() from the range
     * [0, 65535] to the range [0, 255].
     * 
     * @return The green value of the sensor, as a whole number in range [0, 255].
     */
    default int greenByte() {
        return uShortToUByte(this.greenShort());
    }

    /** 
     * Gets the blue component of a color as an unsigned short. When max-ed out, this
     * should return 255 (full-saturation blue), and should read 0 when bottomed out
     * (such as when reading black).
     * 
     * By default, this simply scales the return value of blueShort() from the range
     * [0, 65535] to the range [0, 255].
     * 
     * @return The blue value of the sensor, as a whole number in range [0, 255].
     */
    default int blueByte() {
        return uShortToUByte(this.blueShort());
    }


    /**
     * Gets the alpha component of a color int as an unsigned byte.
     * 
     * @param argb Integer with a hex color in the form `0xAARRGGBB`
     * @return The alpha, in range [0, 255].
     */
    public static int parseAlpha(@ColorInt int argb) {
        return ((argb & 0xff_00_00_00) >>> 24) & 255;
    }

    /**
     * Gets the red component of a color int as an unsigned byte.
     * 
     * @param argb Integer with a hex color in the form `0xAARRGGBB`
     * @return The red, in range [0, 255].
     */
    public static int parseRed(@ColorInt int argb) {
        return ((argb & 0x00_ff_00_00) >>> 16) & 255;
    }

    /**
     * Gets the green component of a color int as an unsigned byte.
     * 
     * @param argb Integer with a hex color in the form `0xAARRGGBB`
     * @return The green, in range [0, 255].
     */
    public static int parseGreen(@ColorInt int argb) {
        return ((argb & 0x00_00_ff_00) >>> 8) & 255;
    }

    /**
     * Gets the blue component of a color int as an unsigned byte.
     * 
     * @param argb Integer with a hex color in the form `0xAARRGGBB`
     * @return The blue, in range [0, 255].
     */
    public static int parseBlue(@ColorInt int argb) {
        return ((argb & 0x00_00_00_ff) >>> 0) & 255;
    }

    /**
     * Constructs an Android color int from the given red, green, and blue.
     * The provided R, G, B arguments should be in the range of [0, 65535];
     * they will be masked with 65535 (not clamped) if they are outside.
     * 
     * The upper byte of each component will be used as its component in the 
     * color int. For instance, if the red is unsigned `0x1234`, blue is 
     * `0x5678`, and green `0x9abc`, then the returned int will be `0x0012569a`
     * because only the upper bytes (`0x12`, `0x56`, `0x9a`) were used.
     * 
     * The alpha component is always set to 0.
     * 
     * @param redShort The red value, in range [0, 65535]
     * @param greenShort The green value, in range [0, 65535]
     * @param blueShort The blue value, in range [0, 65535]
     * @return An integer with the structure `0x00RRGGBB`
     */
    @ColorInt
    public static int colorIntFromShorts(int redShort, int greenShort, int blueShort) {
        final int rb = uShortToUByte(redShort   & 65535) & 255; // `& 255` is just paranoia
        final int gb = uShortToUByte(greenShort & 65535) & 255; // `& 255` is just paranoia
        final int bb = uShortToUByte(blueShort  & 65535) & 255; // `& 255` is just paranoia
        return colorIntFromBytes(rb, gb, bb);
    }

    /**
     * Constructs an Android color int from the given red, green, and blue.
     * The provided R, G, B arguments should be in the range of [0, 255];
     * they will be masked with 65535 (not 255) if they are outside.
     * 
     * The bytes are simply concatenated. For instance, if red is `0x12`,
     * green is `0x34`, and blue is `0x56`, then the result will be 
     * `0x00123456`.
     * 
     * The alpha component is always set to 0.
     * 
     * @param redByte The red value, in range [0, 255]
     * @param greenByte The green value, in range [0, 255]
     * @param blueByte The blue value, in range [0, 255]
     * @return An integer with the structure `0x00RRGGBB`
     */
    @ColorInt
    public static int colorIntFromBytes(int redByte, int greenByte, int blueByte) {
        // My cat thought it would be important to tell you: 4
        return ((redByte & 255) << 16) | ((greenByte & 255) << 8) | ((blueByte & 255) << 0);
    }
    

    /**
     * Linearly scales a unsigned byte to the range of a unsigned short. For 
     * instance, if we have an unsigned byte value of 204, which is 80% of the 
     * way to 255 from 0. This value will be scale to an unsigned short that is
     * 80% of the way to 65535 from 0, which happens to be 52428.
     * 
     * Bitwise, this can be thought of as copying the unsigned byte into the high
     * and low bytes of the returned short; if our byte is `0xab`, then our short 
     * is `0xabab`.
     * 
     * If the provided value is outside the range of [0, 255] (it isn't an 
     * unsigned byte), then the lowest byte will be used. This is equivalent to 
     * doing modulo 255, with a positive sign.
     * 
     * @param unsignedByte Value in range [0, 255].
     * @return Value in range [0, 65535], linearly scaled from the unsigned byte
     */
    public static int uByteToUShort(int unsignedByte) {
        return 257 * unsignedByte;
    }

    /**
     * Linearly scales a unsigned byte to the range of a unsigned short. For 
     * instance, if we have an unsigned short value of 65535, which is 80% of the 
     * way to 65355 from 0. This value will be scale to an unsigned byte that is
     * 80% of the way to 255 from 0, which happens to be 204.
     * 
     * Bitwise, this can be thought of as right-shifting the short by 8 bits. If
     * our short is `0x1234`, we shift it 8 bits to the right to get `0x0012`, 
     * which is equal to `0x12`. 
     * 
     * If the provided value is outside the range of [0, 65535] (it isn't an 
     * unsigned short), then the lowest two bytes will be of it's value will be used. 
     * This is equivalent to doing modulo 65535, with a positive sign.
     * 
     * @param unsignedShort Value in range [0, 65535].
     * @return Value in range [0, 255], linearly scaled from the unsigned byte
     */
    public static int uShortToUByte(int unsignedShort) {
        return ((unsignedShort & 65535) >>> 8) & 255;
    }
}