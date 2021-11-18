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
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalBuffer;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalFormat;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalWriter;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalBuffer;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalFormat;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalWriter;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayFormat;
import net.daporkchop.fp2.gl.attribute.uniform.UniformBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformFormat;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.binding.DrawMode;
import net.daporkchop.fp2.gl.draw.command.DrawCommandArrays;
import net.daporkchop.fp2.gl.draw.command.DrawCommandBuffer;
import net.daporkchop.fp2.gl.draw.command.DrawCommandIndexed;
import net.daporkchop.fp2.gl.draw.index.IndexBuffer;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.draw.shader.FragmentShader;
import net.daporkchop.fp2.gl.draw.shader.VertexShader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
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
        UniformFormat<UniformAttribs> uniformFormat = gl.createUniformFormat(UniformAttribs.class);
        UniformArrayFormat<UniformArrayAttribs> uniformArrayFormat = gl.createUniformArrayFormat(UniformArrayAttribs.class);
        DrawGlobalFormat<GlobalAttribs> globalFormat = gl.createDrawGlobalFormat(GlobalAttribs.class);
        DrawLocalFormat<LocalAttribs> localFormat = gl.createDrawLocalFormat(LocalAttribs.class);

        DrawLayout layout = gl.createDrawLayout()
                .withUniforms(uniformFormat)
                .withUniformArrays(uniformArrayFormat)
                .withGlobals(globalFormat)
                .withLocals(localFormat)
                .build();

        IndexFormat indexFormat = gl.createIndexFormat()
                .type(IndexType.UNSIGNED_SHORT)
                .build();

        VertexShader vertexShader = gl.createVertexShader(layout)
                .include(Identifier.from("test.vert"))
                .build();
        FragmentShader fragmentShader = gl.createFragmentShader(layout)
                .include(Identifier.from("test.frag"))
                .build();
        DrawShaderProgram drawShaderProgram = gl.linkShaderProgram(layout, vertexShader, fragmentShader);

        DrawLocalBuffer<LocalAttribs> localBuffer = localFormat.createBuffer(BufferUsage.STATIC_DRAW);
        localBuffer.resize(4);

        try (DrawLocalWriter<LocalAttribs> writer = localFormat.createWriter()) {
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

        DrawGlobalBuffer<GlobalAttribs> globalBuffer = globalFormat.createBuffer(BufferUsage.STATIC_DRAW);
        globalBuffer.resize(4);

        try (DrawGlobalWriter<GlobalAttribs> writer = globalFormat.createWriter()) {
            for (int i = 0, color = -1, x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++, color = 0xFF << (i << 3), i++) {
                    writer.set(new GlobalAttribs((byte) (x * 32), (byte) (y * 32), 0xFF000000 | color));
                    globalBuffer.set(i, writer);
                }
            }
        }

        UniformArrayBuffer<UniformArrayAttribs> uniformArrayBuffer = uniformArrayFormat.createBuffer(BufferUsage.STATIC_DRAW);
        uniformArrayBuffer.set(new UniformArrayAttribs[]{
                new UniformArrayAttribs(0.5f, 1.0f, 1.0f),
                new UniformArrayAttribs(1.0f, 0.5f, 1.0f),
                new UniformArrayAttribs(1.0f, 1.0f, 0.5f),
        });

        UniformBuffer<UniformAttribs> uniformBuffer = uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);

        DrawBindingIndexed binding = layout.createBinding()
                .withIndexes(indexBuffer)
                .withUniforms(uniformBuffer)
                .withUniformArrays(uniformArrayBuffer)
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

            uniformBuffer.set(new UniformAttribs((byte) 32, (byte) 32));
            commandBufferArrays.execute(DrawMode.TRIANGLES, drawShaderProgram, bitSet);

            uniformBuffer.set(new UniformAttribs((byte) -128, (byte) -128));
            commandBufferElements.execute(DrawMode.TRIANGLES, drawShaderProgram);

            Display.update();
            Display.sync(60);
        }
    }

    @Data
    public static class UniformAttribs {
        @Attribute(vectorAxes = { "X", "Y" }, convert = Attribute.Conversion.TO_NORMALIZED_FLOAT)
        public final byte u_scaleX;
        public final byte u_scaleY;
    }

    @Data
    public static class UniformArrayAttribs {
        @Attribute(vectorAxes = { "R", "G", "B" })
        public final float ua_colorFactorR;
        public final float ua_colorFactorG;
        public final float ua_colorFactorB;
    }

    @Data
    public static class GlobalAttribs {
        @Attribute(vectorAxes = { "X", "Y" }, convert = Attribute.Conversion.TO_FLOAT)
        public final byte a_offsetX;
        public final byte a_offsetY;

        @Attribute(transform = Attribute.Transformation.INT_ARGB8_TO_BYTE_VECTOR_RGBA, convert = Attribute.Conversion.TO_NORMALIZED_FLOAT)
        public final int a_color;
    }

    @Data
    public static class LocalAttribs {
        @Attribute(vectorAxes = { "X", "Y" }, convert = Attribute.Conversion.TO_FLOAT)
        public final byte a_posX;
        public final byte a_posY;
    }
}
