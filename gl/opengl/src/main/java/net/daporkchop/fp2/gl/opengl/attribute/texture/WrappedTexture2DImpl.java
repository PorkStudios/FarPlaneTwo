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

package net.daporkchop.fp2.gl.opengl.attribute.texture;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;

import java.util.function.IntConsumer;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public class WrappedTexture2DImpl extends BaseTextureImpl<TextureFormat2DImpl> implements Texture2D {
    public WrappedTexture2DImpl(@NonNull TextureFormat2DImpl format, int id) {
        super(format, id);
    }

    @Override
    public int width() {
        throw new UnsupportedOperationException("wrapped texture cannot know its size");
    }

    @Override
    public int height() {
        throw new UnsupportedOperationException("wrapped texture cannot know its size");
    }

    @Override
    public int levels() {
        class State implements IntConsumer {
            int levels;

            @Override
            public void accept(int target) {
                this.levels = WrappedTexture2DImpl.this.gl.api().glGetTexParameterInteger(target, GL_TEXTURE_MAX_LEVEL);
            }
        }

        State state = new State();
        this.bindAnyUnit(TextureTarget.TEXTURE_2D, state);
        return state.levels;
    }

    @Override
    public void set(int level, int xOffset, int yOffset, @NonNull TextureWriter2D writer) {
        throw new UnsupportedOperationException();
    }
}
