/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.client.render.state;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.daporkchop.lib.common.misc.Cloneable;

/**
 * Encapsulates non-camera draw state during rendering.
 *
 * @author DaPorkchop_
 */
@EqualsAndHashCode
@ToString
public final class DrawState implements Cloneable<DrawState> {
    //fog
    public float fogColorR;
    public float fogColorG;
    public float fogColorB;
    public float fogColorA;
    public int fogMode;
    public float fogDensity;
    public float fogStart;
    public float fogEnd;

    //misc. GL state
    public float alphaRefCutout;

    @Override
    public DrawState clone() {
        return (DrawState) super.clone();
    }

    /**
     * Stores this draw state in the given {@link DrawStateUniforms} instance.
     *
     * @param uniforms the {@link DrawStateUniforms} instance to store the draw state values in
     */
    public void configureUniforms(DrawStateUniforms uniforms) {
        //fog
        uniforms.fogColor(this.fogColorR, this.fogColorG, this.fogColorB, this.fogColorA);
        uniforms.fogMode(this.fogMode);
        uniforms.fogDensity(this.fogDensity);
        uniforms.fogStart(this.fogStart);
        uniforms.fogEnd(this.fogEnd);
        uniforms.fogScale(1.0f / (this.fogEnd - this.fogStart));

        //misc. GL state
        uniforms.alphaRefCutout(this.alphaRefCutout);
    }
}
