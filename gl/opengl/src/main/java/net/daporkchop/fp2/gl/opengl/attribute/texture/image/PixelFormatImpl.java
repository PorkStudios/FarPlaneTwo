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

package net.daporkchop.fp2.gl.opengl.attribute.texture.image;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormat;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelRange;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.opengl.OpenGL;

/**
 * @author DaPorkchop_
 */
@Data
public class PixelFormatImpl implements PixelFormat {
    @NonNull
    protected final OpenGL gl;

    @NonNull
    protected final PixelInternalFormat internalFormat;

    @NonNull
    protected final PixelStorageFormat storageFormat;
    @NonNull
    protected final PixelStorageType storageType;

    @Override
    public PixelFormatChannelType channelType() {
        return this.internalFormat.channelType();
    }

    @Override
    public PixelFormatChannelRange channelRange() {
        return this.internalFormat.channelRange();
    }

    @Override
    public ImmutableSet<PixelFormatChannel> channels() {
        return this.internalFormat.channels();
    }

    @Override
    public ImmutableMap<PixelFormatChannel, Integer> bitDepthPerChannel() {
        return this.internalFormat.bitDepthPerChannel();
    }
}
