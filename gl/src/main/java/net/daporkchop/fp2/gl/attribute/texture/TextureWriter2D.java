/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
 */

package net.daporkchop.fp2.gl.attribute.texture;

/**
 * @author DaPorkchop_
 */
public interface TextureWriter2D extends BaseTextureWriter {
    @Override
    TextureFormat2D format();

    //
    // R
    //

    /**
     * Sets the texel's R component at the given coordinates to the given integer value.
     * <p>
     * If this texture's format has a floating-point R component, the given integer value will be converted to a float and clamped to the valid range. If this texture's
     * format has an integer R component, the given integer value will be clamped to the valid range. If this texture's format does not have an R component, the value
     * will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setRawR(int x, int y, int r);

    /**
     * Sets the texel's R component at the given coordinates to the given unsigned integer value.
     * <p>
     * If this texture's format has a floating-point R component, the given integer value will be converted to a float and clamped to the valid range. If this texture's
     * format has an integer R component, the given integer value will be clamped to the valid range. If this texture's format does not have an R component, the value
     * will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setRawUnsignedR(int x, int y, int r);

    /**
     * Sets the texel's R component at the given coordinates to the given floating-point value.
     * <p>
     * If this texture's format has a floating-point R component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer R component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an R component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setRawR(int x, int y, float r);

    /**
     * Sets the texel's R component at the given coordinates to the floating-point value computed by interpreting the given integer as an 8-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point R component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer R component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an R component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setNormalized8bR(int x, int y, int r);

    /**
     * Sets the texel's R component at the given coordinates to the floating-point value computed by interpreting the given integer as an 8-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point R component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer R component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an R component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setUnsignedNormalized8bR(int x, int y, int r);

    /**
     * Sets the texel's R component at the given coordinates to the floating-point value computed by interpreting the given integer as a 16-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point R component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer R component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an R component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setNormalized16bR(int x, int y, int r);

    /**
     * Sets the texel's R component at the given coordinates to the floating-point value computed by interpreting the given integer as a 16-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point R component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer R component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an R component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setUnsignedNormalized16bR(int x, int y, int r);

    /**
     * Sets the texel's R component at the given coordinates to the floating-point value computed by interpreting the given integer as a 32-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point R component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer R component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an R component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setNormalized32bR(int x, int y, int r);

    /**
     * Sets the texel's R component at the given coordinates to the floating-point value computed by interpreting the given integer as a 32-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point R component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer R component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an R component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     */
    void setUnsignedNormalized32bR(int x, int y, int r);

    //
    // G
    //

    /**
     * Sets the texel's G component at the given coordinates to the given integer value.
     * <p>
     * If this texture's format has a floating-point G component, the given integer value will be converted to a float and clamped to the valid range. If this texture's
     * format has an integer G component, the given integer value will be clamped to the valid range. If this texture's format does not have an G component, the value
     * will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setRawG(int x, int y, int g);

    /**
     * Sets the texel's G component at the given coordinates to the given unsigned integer value.
     * <p>
     * If this texture's format has a floating-point G component, the given integer value will be converted to a float and clamped to the valid range. If this texture's
     * format has an integer G component, the given integer value will be clamped to the valid range. If this texture's format does not have an G component, the value
     * will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setRawUnsignedG(int x, int y, int g);

    /**
     * Sets the texel's G component at the given coordinates to the given floating-point value.
     * <p>
     * If this texture's format has a floating-point G component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer G component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an G component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setRawG(int x, int y, float g);

    /**
     * Sets the texel's G component at the given coordinates to the floating-point value computed by interpreting the given integer as an 8-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point G component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer G component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an G component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setNormalized8bG(int x, int y, int g);

    /**
     * Sets the texel's G component at the given coordinates to the floating-point value computed by interpreting the given integer as an 8-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point G component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer G component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an G component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setUnsignedNormalized8bG(int x, int y, int g);

    /**
     * Sets the texel's G component at the given coordinates to the floating-point value computed by interpreting the given integer as a 16-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point G component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer G component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an G component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setNormalized16bG(int x, int y, int g);

    /**
     * Sets the texel's G component at the given coordinates to the floating-point value computed by interpreting the given integer as a 16-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point G component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer G component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an G component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setUnsignedNormalized16bG(int x, int y, int g);

    /**
     * Sets the texel's G component at the given coordinates to the floating-point value computed by interpreting the given integer as a 32-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point G component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer G component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an G component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setNormalized32bG(int x, int y, int g);

    /**
     * Sets the texel's G component at the given coordinates to the floating-point value computed by interpreting the given integer as a 32-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point G component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer G component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an G component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param g the new G component value
     */
    void setUnsignedNormalized32bG(int x, int y, int g);

    //
    // B
    //

    /**
     * Sets the texel's B component at the given coordinates to the given integer value.
     * <p>
     * If this texture's format has a floating-point B component, the given integer value will be converted to a float and clamped to the valid range. If this texture's
     * format has an integer B component, the given integer value will be clamped to the valid range. If this texture's format does not have an B component, the value
     * will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setRawB(int x, int y, int b);

    /**
     * Sets the texel's B component at the given coordinates to the given unsigned integer value.
     * <p>
     * If this texture's format has a floating-point B component, the given integer value will be converted to a float and clamped to the valid range. If this texture's
     * format has an integer B component, the given integer value will be clamped to the valid range. If this texture's format does not have an B component, the value
     * will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setRawUnsignedB(int x, int y, int b);

    /**
     * Sets the texel's B component at the given coordinates to the given floating-point value.
     * <p>
     * If this texture's format has a floating-point B component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer B component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an B component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setRawB(int x, int y, float b);

    /**
     * Sets the texel's B component at the given coordinates to the floating-point value computed by interpreting the given integer as an 8-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point B component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer B component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an B component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setNormalized8bB(int x, int y, int b);

    /**
     * Sets the texel's B component at the given coordinates to the floating-point value computed by interpreting the given integer as an 8-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point B component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer B component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an B component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setUnsignedNormalized8bB(int x, int y, int b);

    /**
     * Sets the texel's B component at the given coordinates to the floating-point value computed by interpreting the given integer as a 16-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point B component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer B component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an B component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setNormalized16bB(int x, int y, int b);

    /**
     * Sets the texel's B component at the given coordinates to the floating-point value computed by interpreting the given integer as a 16-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point B component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer B component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an B component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setUnsignedNormalized16bB(int x, int y, int b);

    /**
     * Sets the texel's B component at the given coordinates to the floating-point value computed by interpreting the given integer as a 32-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point B component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer B component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an B component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setNormalized32bB(int x, int y, int b);

    /**
     * Sets the texel's B component at the given coordinates to the floating-point value computed by interpreting the given integer as a 32-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point B component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer B component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an B component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param b the new B component value
     */
    void setUnsignedNormalized32bB(int x, int y, int b);

    //
    // A
    //

    /**
     * Sets the texel's A component at the given coordinates to the given integer value.
     * <p>
     * If this texture's format has a floating-point A component, the given integer value will be converted to a float and clamped to the valid range. If this texture's
     * format has an integer A component, the given integer value will be clamped to the valid range. If this texture's format does not have an A component, the value
     * will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setRawA(int x, int y, int a);

    /**
     * Sets the texel's A component at the given coordinates to the given unsigned integer value.
     * <p>
     * If this texture's format has a floating-point A component, the given integer value will be converted to a float and clamped to the valid range. If this texture's
     * format has an integer A component, the given integer value will be clamped to the valid range. If this texture's format does not have an A component, the value
     * will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setRawUnsignedA(int x, int y, int a);

    /**
     * Sets the texel's A component at the given coordinates to the given floating-point value.
     * <p>
     * If this texture's format has a floating-point A component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer A component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an A component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setRawA(int x, int y, float a);

    /**
     * Sets the texel's A component at the given coordinates to the floating-point value computed by interpreting the given integer as an 8-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point A component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer A component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an A component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setNormalized8bA(int x, int y, int a);

    /**
     * Sets the texel's A component at the given coordinates to the floating-point value computed by interpreting the given integer as an 8-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point A component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer A component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an A component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setUnsignedNormalized8bA(int x, int y, int a);

    /**
     * Sets the texel's A component at the given coordinates to the floating-point value computed by interpreting the given integer as a 16-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point A component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer A component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an A component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setNormalized16bA(int x, int y, int a);

    /**
     * Sets the texel's A component at the given coordinates to the floating-point value computed by interpreting the given integer as a 16-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point A component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer A component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an A component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setUnsignedNormalized16bA(int x, int y, int a);

    /**
     * Sets the texel's A component at the given coordinates to the floating-point value computed by interpreting the given integer as a 32-bit signed integer, and
     * converting it to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * If this texture's format has a floating-point A component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer A component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an A component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setNormalized32bA(int x, int y, int a);

    /**
     * Sets the texel's A component at the given coordinates to the floating-point value computed by interpreting the given integer as a 32-bit unsigned integer, and
     * converting it to a float normalized on the range {@code [0, 1]}.
     * <p>
     * If this texture's format has a floating-point A component, the given floating-point value will be clamped to the valid range. If this texture's format has an
     * integer A component, the given floating-point value will be converted to an integer by rounding towards negative infinity. If this texture's format does not have
     * an A component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param a the new A component value
     */
    void setUnsignedNormalized32bA(int x, int y, int a);

    //
    // R
    //

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the given integer values.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding integer value will be converted to a float and clamped to the valid range.
     * If this texture's format has a corresponding integer component, the corresponding integer value will be clamped to the valid range. If this texture's format does
     * not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setRawRGBA(int x, int y, int r, int g, int b, int a);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the given unsigned integer values.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding integer value will be converted to a float and clamped to the valid range.
     * If this texture's format has a corresponding integer component, the corresponding integer value will be clamped to the valid range. If this texture's format does
     * not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setRawUnsignedRGBA(int x, int y, int r, int g, int b, int a);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the given floating-point values.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this texture's
     * format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative infinity. If this
     * texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setRawRGBA(int x, int y, float r, float g, float b, float a);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the floating-point values computed by interpreting the given integers as 8-bit signed
     * integers, and converting them to floats normalized on the range {@code [-1, 1]}.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this texture's
     * format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative infinity. If this
     * texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setNormalized8bRGBA(int x, int y, int r, int g, int b, int a);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the floating-point values computed by interpreting the given integers as 8-bit unsigned
     * integers, and converting them to floats normalized on the range {@code [0, 1]}.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this texture's
     * format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative infinity. If this
     * texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setUnsignedNormalized8bRGBA(int x, int y, int r, int g, int b, int a);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the floating-point values computed by interpreting the given integers as 16-bit signed
     * integers, and converting them to floats normalized on the range {@code [-1, 1]}.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this texture's
     * format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative infinity. If this
     * texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setNormalized16bRGBA(int x, int y, int r, int g, int b, int a);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the floating-point values computed by interpreting the given integers as 16-bit unsigned
     * integers, and converting them to floats normalized on the range {@code [0, 1]}.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this texture's
     * format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative infinity. If this
     * texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setUnsignedNormalized16bRGBA(int x, int y, int r, int g, int b, int a);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the floating-point values computed by interpreting the given integers as 32-bit signed
     * integers, and converting them to floats normalized on the range {@code [-1, 1]}.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this texture's
     * format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative infinity. If this
     * texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setNormalized32bRGBA(int x, int y, int r, int g, int b, int a);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the floating-point values computed by interpreting the given integers as 32-bit unsigned
     * integers, and converting them to floats normalized on the range {@code [0, 1]}.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this texture's
     * format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative infinity. If this
     * texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x the texel's X coordinate
     * @param y the texel's Y coordinate
     * @param r the new R component value
     * @param g the new G component value
     * @param b the new B component value
     * @param a the new A component value
     */
    void setUnsignedNormalized32bRGBA(int x, int y, int r, int g, int b, int a);

    //
    // ARGB8888
    //

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the integer values computed by interpreting each of the ARGB8888-packed color channels as an
     * 8-bit signed integer.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding integer value will be converted to a float and clamped to the valid range.
     * If this texture's format has a corresponding integer component, the corresponding integer value will be clamped to the valid range. If this texture's format does
     * not have a corresponding component, the value will be silently discarded.
     *
     * @param x    the texel's X coordinate
     * @param y    the texel's Y coordinate
     * @param argb the new R, G, B and A component values, ARGB8888-packed into a single {@code int}
     */
    void setRawARGB8(int x, int y, int argb);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the integer values computed by interpreting each of the ARGB8888-packed color channels as an
     * 8-bit unsigned integer.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding integer value will be converted to a float and clamped to the valid range.
     * If this texture's format has a corresponding integer component, the corresponding integer value will be clamped to the valid range. If this texture's format does
     * not have a corresponding component, the value will be silently discarded.
     *
     * @param x    the texel's X coordinate
     * @param y    the texel's Y coordinate
     * @param argb the new R, G, B and A component values, ARGB8888-packed into a single {@code int}
     */
    void setRawUnsignedARGB8(int x, int y, int argb);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the floating-point values computed by interpreting each of the ARGB8888-packed color channels
     * as an 8-bit signed integer, and converting them to a float normalized on the range {@code [-1, 1]}.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this
     * texture's format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative
     * infinity. If this texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x    the texel's X coordinate
     * @param y    the texel's Y coordinate
     * @param argb the new R, G, B and A component values, ARGB8888-packed into a single {@code int}
     */
    void setNormalizedARGB8(int x, int y, int argb);

    /**
     * Sets the texel's R, G, B and A components at the given coordinates to the floating-point values computed by interpreting each of the ARGB8888-packed color channels
     * as an 8-bit unsigned integer, and converting them to a float normalized on the range {@code [0, 1]}.
     * <p>
     * For each of R, G, B and A:
     * If this texture's format has a corresponding floating-point component, the corresponding floating-point value will be clamped to the valid range. If this
     * texture's format has a corresponding integer component, the corresponding floating-point value will be converted to an integer by rounding towards negative
     * infinity. If this texture's format does not have a corresponding component, the value will be silently discarded.
     *
     * @param x    the texel's X coordinate
     * @param y    the texel's Y coordinate
     * @param argb the new R, G, B and A component values, ARGB8888-packed into a single {@code int}
     */
    void setUnsignedNormalizedARGB8(int x, int y, int argb);
}
