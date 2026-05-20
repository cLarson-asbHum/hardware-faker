package clarson.ftc.faker.util;

import androidx.annotation.ColorInt;

public interface ColorGetter {
    /**
     * Gets an RGBA color whose hex matches the form `0xAARRGGBB` (written here 
     * in big endian). 
     * 
     * @return An Android color int, which is an RGBA color in form `0x`
     */
    @ColorInt
    public int getColor();
}