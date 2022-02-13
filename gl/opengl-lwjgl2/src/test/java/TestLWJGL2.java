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

import com.google.common.base.Strings;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayTransform;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayType;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.FieldsAsArrayAttribute;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarExpand;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;
import net.daporkchop.fp2.gl.command.CommandBuffer;
import net.daporkchop.fp2.gl.command.FramebufferLayer;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.index.IndexBuffer;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.fp2.gl.draw.list.DrawCommandArrays;
import net.daporkchop.fp2.gl.draw.list.DrawCommandIndexed;
import net.daporkchop.fp2.gl.draw.list.selected.JavaSelectedDrawList;
import net.daporkchop.fp2.gl.draw.list.selected.ShaderSelectedDrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.draw.shader.FragmentShader;
import net.daporkchop.fp2.gl.draw.shader.VertexShader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.transform.TransformLayout;
import net.daporkchop.fp2.gl.transform.binding.TransformBinding;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgram;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.ThreadLocalRandom;

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
        //Display.create(new PixelFormat(), new ContextAttribs(3, 0, ContextAttribs.CONTEXT_CORE_PROFILE_BIT_ARB, ContextAttribs.CONTEXT_FORWARD_COMPATIBLE_BIT_ARB));
        Display.create(new PixelFormat(), new ContextAttribs(3, 3, ContextAttribs.CONTEXT_CORE_PROFILE_BIT_ARB, ContextAttribs.CONTEXT_FORWARD_COMPATIBLE_BIT_ARB));
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
        AttributeFormat<UniformAttribs> uniformFormat = gl.createAttributeFormat(UniformAttribs.class).useFor(AttributeUsage.UNIFORM).build();
        AttributeFormat<UniformArrayAttribs> uniformArrayFormat = gl.createAttributeFormat(UniformArrayAttribs.class).useFor(AttributeUsage.UNIFORM_ARRAY).build();
        AttributeFormat<GlobalAttribs> globalFormat = gl.createAttributeFormat(GlobalAttribs.class).useFor(AttributeUsage.DRAW_GLOBAL).build();
        TextureFormat2D<TextureAttribs> textureFormat = gl.createTextureFormat2D(TextureAttribs.class).build();
        AttributeFormat<LocalAttribs> localFormat = gl.createAttributeFormat(LocalAttribs.class).useFor(AttributeUsage.DRAW_LOCAL, AttributeUsage.TRANSFORM_INPUT, AttributeUsage.TRANSFORM_OUTPUT)
                .rename("pos", "posRenamed")
                .build();
        AttributeFormat<UniformSelectionAttribs> selectionUniformFormat = gl.createAttributeFormat(UniformSelectionAttribs.class).useFor(AttributeUsage.UNIFORM).build();

        DrawLayout drawLayout = gl.createDrawLayout()
                .withUniform(uniformFormat)
                .withUniformArray(uniformArrayFormat)
                .withGlobal(globalFormat)
                .withLocal(localFormat)
                .withTexture(textureFormat)
                .build();

        TransformLayout transformLayout = gl.createTransformLayout()
                .withUniform(uniformFormat)
                .withUniformArray(uniformArrayFormat)
                .withInput(localFormat)
                .with(AttributeUsage.TRANSFORM_INPUT, localFormat, "ti_", "_prev")
                .withOutput(localFormat)
                .build();

        IndexFormat indexFormat = gl.createIndexFormat()
                .type(IndexType.UNSIGNED_SHORT)
                .build();

        VertexShader vertexShader = gl.createVertexShader(drawLayout)
                .include(Identifier.from("test.vert"))
                .build();
        FragmentShader fragmentShader = gl.createFragmentShader(drawLayout)
                .include(Identifier.from("test.frag"))
                .build();
        DrawShaderProgram drawShaderProgram = gl.createDrawShaderProgram(drawLayout)
                .addShader(vertexShader)
                .addShader(fragmentShader)
                .build();

        TransformShaderProgram transformShaderProgram = gl.createTransformShaderProgram(transformLayout)
                .addShader(gl.createTransformShader(transformLayout)
                        .include(Identifier.from("test_transform.vert"))
                        .build())
                .build();

        AttributeBuffer<LocalAttribs> localBuffer_1 = localFormat.createBuffer(BufferUsage.STATIC_DRAW);
        localBuffer_1.resize(4);

        AttributeBuffer<LocalAttribs> localBuffer_2 = localFormat.createBuffer(BufferUsage.STREAM_COPY);
        localBuffer_2.resize(localBuffer_1.capacity());

        try (AttributeWriter<LocalAttribs> writer = localFormat.createWriter()) {
            writer.put(new LocalAttribs((byte) 16, (byte) 16));
            writer.put(new LocalAttribs((byte) 16, (byte) 32));
            writer.put(new LocalAttribs((byte) 32, (byte) 32));
            writer.put(new LocalAttribs((byte) 32, (byte) 16));

            localBuffer_1.set(0, writer);
            localBuffer_2.set(0, writer);
        }

        IndexBuffer indexBuffer = indexFormat.createBuffer(BufferUsage.STATIC_DRAW);
        indexBuffer.resize(6);

        try (IndexWriter writer = indexFormat.createWriter()) {
            writer.appendQuadAsTriangles(2, 1, 3, 0);

            indexBuffer.set(0, writer);
        }

        AttributeBuffer<GlobalAttribs> globalBuffer = globalFormat.createBuffer(BufferUsage.STATIC_DRAW);
        globalBuffer.resize(4);

        try (AttributeWriter<GlobalAttribs> writer = globalFormat.createWriter()) {
            for (int i = 0, color = -1, x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++, color = 0xFF << (i << 3), i++) {
                    writer.put(new GlobalAttribs((byte) (x * 32), (byte) (y * 32), 0xFF000000 | color));
                }
            }
            globalBuffer.set(0, writer);
        }

        AttributeBuffer<UniformArrayAttribs> uniformArrayBuffer = uniformArrayFormat.createBuffer(BufferUsage.STATIC_DRAW);
        uniformArrayBuffer.setContents(
                new UniformArrayAttribs(0.5f, 1.0f, 1.0f),
                new UniformArrayAttribs(1.0f, 0.5f, 1.0f),
                new UniformArrayAttribs(1.0f, 1.0f, 0.5f));

        AttributeBuffer<UniformAttribs> uniformBuffer0 = uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);
        uniformBuffer0.setContents(new UniformAttribs((byte) 32, (byte) 32));

        AttributeBuffer<UniformAttribs> uniformBuffer1 = uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);
        uniformBuffer1.setContents(new UniformAttribs((byte) -128, (byte) -128));

        AttributeBuffer<UniformAttribs> uniformBuffer2 = uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);
        uniformBuffer2.setContents(new UniformAttribs((byte) -64, (byte) 32));

        Texture2D<TextureAttribs> texture = textureFormat.createTexture(512, 512, 1);
        try (TextureWriter2D<TextureAttribs> writer = textureFormat.createWriter(512, 512)) {
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    writer.set(x, y, new TextureAttribs(ThreadLocalRandom.current().nextInt() | 0xFF000000));
                }
            }

            texture.set(0, 0, 0, writer);
        }

        DrawBinding binding0 = drawLayout.createBinding()
                .withUniform(uniformBuffer0)
                .withUniformArray(uniformArrayBuffer)
                .withGlobal(globalBuffer)
                .withLocal(localBuffer_1)
                .withTexture(texture)
                .build();

        DrawBindingIndexed binding1 = drawLayout.createBinding()
                .withIndexes(indexBuffer)
                .withUniform(uniformBuffer1)
                .withUniformArray(uniformArrayBuffer)
                .withGlobal(globalBuffer)
                .withLocal(localBuffer_1)
                .withTexture(texture)
                .build();

        TransformBinding binding2_transform = transformLayout.createBinding()
                .withUniform(uniformBuffer1)
                .withUniformArray(uniformArrayBuffer)
                .withInput(localBuffer_1)
                .withInput(localBuffer_1) //TODO: this should be localBuffer_2, but i can't bind the same buffer as an input and output at once (need to make it print an error, too)
                .withOutput(localBuffer_2)
                .build();

        DrawBindingIndexed binding2_draw = drawLayout.createBinding()
                .withIndexes(indexBuffer)
                .withUniform(uniformBuffer2)
                .withUniformArray(uniformArrayBuffer)
                .withGlobal(globalBuffer)
                .withLocal(localBuffer_2)
                .withTexture(texture)
                .build();

        ShaderSelectedDrawList<DrawCommandArrays> listArrays = gl.createDrawListArrays(binding0).buildShaderSelected();
        listArrays.resize(4);
        listArrays.set(0, new DrawCommandArrays(0, 3));
        listArrays.set(1, new DrawCommandArrays(0, 3));
        listArrays.set(2, new DrawCommandArrays(0, 3));
        listArrays.set(3, new DrawCommandArrays(0, 3));

        TransformLayout selectionLayout = listArrays.configureTransformLayoutForSelection(gl.createTransformLayout())
                .withUniform(selectionUniformFormat)
                .build();

        AttributeBuffer<UniformSelectionAttribs> selectionUniformBuffer = selectionUniformFormat.createBuffer(BufferUsage.STREAM_DRAW);

        TransformBinding selectionBinding = listArrays.configureTransformBindingForSelection(selectionLayout.createBinding())
                .withUniform(selectionUniformBuffer)
                .build();

        TransformShaderProgram selectionProgram = listArrays.configureTransformShaderProgramForSelection(gl.createTransformShaderProgram(selectionLayout))
                .addShader(listArrays.configureTransformShaderForSelection(gl.createTransformShader(selectionLayout))
                        .include(Identifier.from("test_selection.vert"))
                        .build())
                .build();

        JavaSelectedDrawList<DrawCommandIndexed> listElements = gl.createDrawListIndexed(binding1).buildJavaSelected();
        listElements.resize(4);
        listElements.set(0, new DrawCommandIndexed(0, 6, 0));
        listElements.set(1, new DrawCommandIndexed(0, 6, 0));
        listElements.set(2, new DrawCommandIndexed(0, 6, 0));
        listElements.set(3, new DrawCommandIndexed(0, 6, 0));

        try (CommandBuffer cmdBuffer = gl.createCommandBuffer()
                .blendDisable()
                .framebufferClear(FramebufferLayer.COLOR)
                .drawSelectedList(drawShaderProgram, DrawMode.TRIANGLES, listElements, i -> ThreadLocalRandom.current().nextBoolean())
                .drawSelectedList(drawShaderProgram, DrawMode.TRIANGLES, listArrays, selectionProgram, selectionBinding)
                .drawArrays(drawShaderProgram, DrawMode.TRIANGLES, binding0, 0, 3)
                .transform(transformShaderProgram, binding2_transform, localBuffer_1.capacity())
                .drawArrays(drawShaderProgram, DrawMode.TRIANGLES, binding2_draw, 0, 3)
                .conditional(() -> false, builder -> builder.framebufferClear(FramebufferLayer.DEPTH))
                .build()) {
            while (!Display.isCloseRequested()) {
                selectionUniformBuffer.setContents(new UniformSelectionAttribs(ThreadLocalRandom.current().nextInt() & 1));

                cmdBuffer.execute();

                Display.update();
                Display.sync(60);
            }
        }
    }

    @Data
    public static class UniformAttribs {
        @FieldsAsArrayAttribute(
                attribute = @Attribute(name = "scale"),
                names = { "scaleX", "scaleY" },
                scalarType = @ScalarType(convert = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true)),
                transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
        public final byte scaleX;
        public final byte scaleY;

        @Attribute
        public final float @ArrayType(length = 3, transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR)) [] floatsAsVector = new float[3];

        @Attribute
        @ScalarType(convert = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = false))
        public final byte @ArrayType(length = 8, transform = @ArrayTransform(value = ArrayTransform.Type.TO_VECTOR_ARRAY, vectorComponents = 2)) [] vec2Array = new byte[8];

        //@Attribute public final float[] array = new float[8];
    }

    @Data
    public static class UniformArrayAttribs {
        @FieldsAsArrayAttribute(
                attribute = @Attribute(name = "colorFactor"),
                names = { "colorFactorR", "colorFactorG", "colorFactorB" },
                transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
        public final float colorFactorR;
        public final float colorFactorG;
        public final float colorFactorB;
    }

    @Data
    public static class GlobalAttribs {
        @FieldsAsArrayAttribute(
                attribute = @Attribute(name = "offset"),
                names = { "offsetX", "offsetY" },
                scalarType = @ScalarType(convert = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = false)),
                transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
        public final byte offsetX;
        public final byte offsetY;

        @Attribute
        @ScalarType(expand = @ScalarExpand(
                value = ScalarExpand.Type.INT_ARGB8_TO_BYTE_VECTOR_RGBA,
                thenConvert = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true)))
        public final int color;
    }

    @Data
    public static class LocalAttribs {
        @FieldsAsArrayAttribute(
                attribute = @Attribute(name = "pos"),
                names = { "posX", "posY" },
                scalarType = @ScalarType(convert = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = false)),
                transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
        public final byte posX;
        public final byte posY;
    }

    @Data
    public static class TextureAttribs {
        @Attribute
        @ScalarType(expand = @ScalarExpand(
                value = ScalarExpand.Type.INT_ARGB8_TO_BYTE_VECTOR_RGBA,
                thenConvert = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true)))
        public final int colorFactor;
    }

    @Data
    public static class UniformSelectionAttribs {
        @Attribute
        public final int selectable;
    }
}
