/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.strategy.heightmap;

/**
 * Allows access to heightmap data at given coordinates.
 *
 * @author DaPorkchop_
 */
public interface HeightmapAccess {
    /**
     * Gets the height value at the given coordinates.
     *
     * @param x the X coordinate of the height value to get
     * @param z the Z coordinate of the height value to get
     * @return the height value at the given coordinates
     */
    int height(int x, int z);

    /**
     * Gets the color value at the given coordinates.
     *
     * @param x the X coordinate of the color value to get
     * @param z the Z coordinate of the color value to get
     * @return the color value at the given coordinates
     */
    int color(int x, int z);

    /**
     * Sets the height value at the given coordinates.
     *
     * @param x      the X coordinate of the height value to set
     * @param z      the Z coordinate of the height value to set
     * @param height the new height value
     * @return this
     */
    HeightmapAccess height(int x, int z, int height);

    /**
     * Sets the color value at the given coordinates.
     *
     * @param x     the X coordinate of the color value to set
     * @param z     the Z coordinate of the color value to set
     * @param color the new color value
     * @return this
     */
    HeightmapAccess color(int x, int z, int color);
}
