/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

import com.google.common.base.Strings;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.Attrib;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeFormatBuilder;
import net.daporkchop.fp2.gl.attribute.AttributeInterpretation;
import net.daporkchop.fp2.gl.attribute.AttributeType;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeWriter;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeFormat;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeWriter;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeBuffer;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.command.DrawCommandArrays;
import net.daporkchop.fp2.gl.command.DrawCommandBuffer;
import net.daporkchop.fp2.gl.command.DrawCommandIndexed;
import net.daporkchop.fp2.gl.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.binding.DrawMode;
import net.daporkchop.fp2.gl.index.IndexBuffer;
import net.daporkchop.fp2.gl.index.IndexFormat;
import net.daporkchop.fp2.gl.index.IndexType;
import net.daporkchop.fp2.gl.index.IndexWriter;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.VertexShader;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
public class TestLWJGL2 {
    private static void hackNatives() {
        String paths = System.getProperty("java.library.path");
        String nativesDir = "/media/daporkchop/PortableIDE/.gradle/caches/minecraft/net/minecraft/natives/1.12.2";

        if (Strings.isNullOrEmpty(paths)) {
            paths = nativesDir;
        } else {
            paths += File.pathSeparator + nativesDir;
        }

        System.setProperty("java.library.path", paths);

        // hack the classloader now.
        try {
            final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (Throwable t) {
        }
    }

    public static void main(String... args) throws LWJGLException {
        hackNatives();

        Display.setDisplayMode(new DisplayMode(512, 512));
        Display.setTitle("title");
        Display.create(new PixelFormat(), new ContextAttribs(3, 0, ContextAttribs.CONTEXT_CORE_PROFILE_BIT_ARB, ContextAttribs.CONTEXT_FORWARD_COMPATIBLE_BIT_ARB));
        //Display.create(new PixelFormat(), new ContextAttribs(3, 3, ContextAttribs.CONTEXT_CORE_PROFILE_BIT_ARB, ContextAttribs.CONTEXT_FORWARD_COMPATIBLE_BIT_ARB));
        //Display.create();

        try (GL gl = GL.builder()
                .withResourceProvider(id -> {
                    InputStream in = TestLWJGL2.class.getResourceAsStream(id.path());
                    if (in != null) {
                        return in;
                    }
                    throw new ResourceNotFoundException(id);
                })
                .wrapCurrent()) {
            run(gl);
        } finally {
            Display.destroy();
        }
    }

    @SneakyThrows({ ShaderCompilationException.class, ShaderLinkageException.class })
    private static void run(@NonNull GL gl) {
        Attribute.Int4 outputAttrColor;
        AttributeFormat outputFormat;

        {
            AttributeFormatBuilder builder = gl.createAttributeFormat()
                    .name("OUTPUT_COLOR");

            outputAttrColor = builder.attrib().name("f_color")
                    .int4(AttributeType.Integer.UNSIGNED_BYTE)
                    .interpretation(AttributeInterpretation.NORMALIZED_FLOAT)
                    .build();

            outputFormat = builder.build();
        }

        LocalAttributeFormat<LocalAttribs> localFormat = gl.createLocalFormat(LocalAttribs.class);

        Attribute.Int2 attrOffset;
        Attribute.Int4 attrColor;
        AttributeFormat globalFormat;

        {
            AttributeFormatBuilder builder = gl.createAttributeFormat()
                    .name("GLOBAL_0");

            attrOffset = builder.attrib().name("a_offset")
                    .int2(AttributeType.Integer.BYTE)
                    .interpretation(AttributeInterpretation.FLOAT)
                    .build();

            attrColor = builder.attrib().name("a_color")
                    .int4(AttributeType.Integer.UNSIGNED_BYTE)
                    .interpretation(AttributeInterpretation.NORMALIZED_FLOAT)
                    .build();

            globalFormat = builder.build();
        }

        Attribute.Int2 attrScale;
        AttributeFormat uniformFormat;

        {
            AttributeFormatBuilder builder = gl.createAttributeFormat()
                    .name("UNIFORM_0");

            attrScale = builder.attrib().name("u_scale")
                    .int2(AttributeType.Integer.BYTE)
                    .interpretation(AttributeInterpretation.NORMALIZED_FLOAT)
                    .build();

            uniformFormat = builder.build();
        }

        DrawLayout layout = gl.createDrawLayout()
                .withUniforms(uniformFormat)
                .withGlobals(globalFormat)
                .withLocals(localFormat)
                .withOutputs(outputFormat)
                .build();

        IndexFormat indexFormat = gl.createIndexFormat()
                .type(IndexType.UNSIGNED_SHORT)
                .build();

        VertexShader vertexShader = gl.createVertexShader()
                .forLayout(layout)
                .include(Identifier.from("test.vert")).endSource()
                .endDefines()
                .build();
        FragmentShader fragmentShader = gl.createFragmentShader()
                .forLayout(layout)
                .include(Identifier.from("test.frag")).endSource()
                .endDefines()
                .build();
        DrawShaderProgram drawShaderProgram = gl.linkShaderProgram(layout, vertexShader, fragmentShader);

        LocalAttributeBuffer<LocalAttribs> localBuffer = localFormat.createBuffer(BufferUsage.STATIC_DRAW);
        localBuffer.resize(4);

        try (LocalAttributeWriter<LocalAttribs> writer = localFormat.createWriter()) {
            writer.put(new LocalAttribs((byte) 16, (byte) 16));
            writer.put(new LocalAttribs((byte) 16, (byte) 32));
            writer.put(new LocalAttribs((byte) 32, (byte) 32));
            writer.put(new LocalAttribs((byte) 32, (byte) 16));

            localBuffer.set(0, writer);
        }

        IndexBuffer indexBuffer = indexFormat.createBuffer(BufferUsage.STATIC_DRAW);
        indexBuffer.resize(6);

        try (IndexWriter writer = indexFormat.createWriter()) {
            writer.appendQuadAsTriangles(2, 1, 3, 0);

            indexBuffer.set(0, writer);
        }

        GlobalAttributeBuffer globalBuffer = globalFormat.createGlobalBuffer(BufferUsage.STATIC_DRAW);
        globalBuffer.resize(4);

        try (GlobalAttributeWriter writer = globalFormat.createGlobalWriter()) {
            for (int i = 0, color = -1, x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++, color = 0xFF << (i << 3), i++) {
                    writer.set(attrOffset, x * 32, y * 32).setARGB(attrColor, 0xFF000000 | color);
                    globalBuffer.set(i, writer);
                }
            }
        }

        UniformAttributeBuffer uniformBuffer = uniformFormat.createUniformBuffer(BufferUsage.STATIC_DRAW);

        DrawBindingIndexed binding = layout.createBinding()
                .withIndexes(indexBuffer)
                .withUniforms(uniformBuffer)
                .withGlobals(globalBuffer)
                .withLocals(localBuffer)
                .build();

        DrawCommandBuffer<DrawCommandArrays> commandBufferArrays = gl.createCommandBuffer()
                .forArrays(binding)
                .build();
        commandBufferArrays.resize(4);
        commandBufferArrays.set(0, new DrawCommandArrays(0, 3));
        commandBufferArrays.set(1, new DrawCommandArrays(0, 3));
        commandBufferArrays.set(2, new DrawCommandArrays(0, 3));
        commandBufferArrays.set(3, new DrawCommandArrays(0, 3));

        DrawCommandBuffer<DrawCommandIndexed> commandBufferElements = gl.createCommandBuffer()
                .forIndexed(binding)
                .build();
        commandBufferElements.resize(4);
        commandBufferElements.set(0, new DrawCommandIndexed(0, 6, 0));
        commandBufferElements.set(1, new DrawCommandIndexed(0, 6, 0));
        commandBufferElements.set(2, new DrawCommandIndexed(0, 6, 0));
        commandBufferElements.set(3, new DrawCommandIndexed(0, 6, 0));

        GLBitSet bitSet = gl.createBitSet()
                .optimizeFor(commandBufferElements)
                .build();
        bitSet.resize(4);

        while (!Display.isCloseRequested()) {
            glClear(GL_COLOR_BUFFER_BIT);

            bitSet.set(i -> ThreadLocalRandom.current().nextBoolean());

            uniformBuffer.set(attrScale, 64, 64);
            commandBufferArrays.execute(DrawMode.TRIANGLES, drawShaderProgram, bitSet);

            uniformBuffer.set(attrScale, -128, -128);
            commandBufferElements.execute(DrawMode.TRIANGLES, drawShaderProgram);

            Display.update();
            Display.sync(60);
        }
    }

    @Data
    public static class LocalAttribs {
        @Attrib(vectorAxes = {"X", "Y"}, convert = Attrib.Conversion.TO_FLOAT)
        public final byte a_posX;
        public final byte a_posY;
    }
}
