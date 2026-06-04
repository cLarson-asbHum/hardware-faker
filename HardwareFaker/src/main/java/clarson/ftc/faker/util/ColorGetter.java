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