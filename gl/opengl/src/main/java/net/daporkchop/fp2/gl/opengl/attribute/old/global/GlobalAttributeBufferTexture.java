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
 *
 */

package net.daporkchop.fp2.gl.opengl.attribute.old.global;

/**
 * @author DaPorkchop_
 */
/*@Getter
public class GlobalAttributeBufferTexture extends BaseAttributeBufferImpl implements GlobalAttributeBuffer {
    protected final TextureImpl[] textures;
    protected final GLBufferImpl[] buffers;
    protected final int[] strides;

    protected int capacity;

    public GlobalAttributeBufferTexture(@NonNull AttributeFormatImpl format, @NonNull BufferUsage usage) {
        super(format);

        int attribCount = format.attribsArray().length;
        this.textures = new TextureImpl[attribCount];
        this.buffers = new GLBufferImpl[attribCount];
        this.strides = format.stridesTexture();

        GLAPI api = this.gl.api();

        int oldTexUnit = api.glGetInteger(GL_ACTIVE_TEXTURE);
        api.glActiveTexture(GL_TEXTURE0);
        int oldBufferTexture = api.glGetInteger(GL_TEXTURE_BINDING_BUFFER);
        try {
            for (int i = 0; i < attribCount; i++) {
                this.textures[i] = this.gl.createTexture();
                this.buffers[i] = this.gl.createBuffer(usage);

                api.glBindTexture(GL_TEXTURE_BUFFER, this.textures[i].id());
                api.glTexBuffer(GL_TEXTURE_BUFFER, format.attribsArray()[i].unpackedTextureFormat(), this.buffers[i].id());
            }
        } finally {
            api.glActiveTexture(oldTexUnit);
            api.glBindTexture(GL_TEXTURE_BUFFER, oldBufferTexture);
        }
    }

    @Override
    public void close() {
        Stream.of(this.textures).forEach(GLResource::close);
        Stream.of(this.buffers).forEach(GLResource::close);
    }

    @Override
    public void resize(int capacity) {
        this.capacity = notNegative(capacity, "capacity");

        for (int i = 0; i < this.strides.length; i++) {
            this.buffers[i].resize(capacity * (long) this.strides[i]);
        }
    }

    @Override
    public void set(int index, @NonNull GlobalAttributeWriter _writer) {
        GlobalAttributeWriterPacked writer = (GlobalAttributeWriterPacked) _writer;
        checkArg(writer.format() == this.format, "mismatched attribute formats!");
        checkIndex(this.capacity, index);

        float[] floatBuf = new float[4];
        long buf = PUnsafe.allocateMemory(IntStream.of(this.strides).max().orElse(0));
        try {
            for (int i = 0; i < this.strides.length; i++) {
                this.format.attribsArray()[i].unpack(null, writer.addr + writer.offsets[i], null, buf);
                this.format.attribsArray()[i].unpack(null, writer.addr + writer.offsets[i], floatBuf, PUnsafe.ARRAY_FLOAT_BASE_OFFSET);
                this.buffers[i].uploadRange(index * (long) this.strides[i], buf, this.strides[i]);
            }
        } finally {
            PUnsafe.freeMemory(buf);
        }
    }
}*/
