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

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayIndex;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayLength;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayType;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarExpand;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarTransform;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;
import net.daporkchop.fp2.gl.attribute.annotation.VectorType;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelRange;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
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

import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class TestOpenGL {
    public static final int WINDOW_SIZE_W = 512;
    public static final int WINDOW_SIZE_H = WINDOW_SIZE_W;

    public static void run(@NonNull BooleanSupplier closeRequested, @NonNull Runnable swapAndSync) {
        System.setProperty("fp2.gl.opengl.debug", "true");

        try (GL gl = GL.builder()
                .withResourceProvider(id -> {
                    InputStream in = TestOpenGL.class.getResourceAsStream(id.path());
                    if (in != null) {
                        return in;
                    }
                    throw new ResourceNotFoundException(id);
                })
                .wrapCurrent()) {
            run(gl, closeRequested, swapAndSync);
        }
    }

    @SneakyThrows({ ShaderCompilationException.class, ShaderLinkageException.class })
    private static void run(@NonNull GL gl, @NonNull BooleanSupplier closeRequested, @NonNull Runnable swapAndSync) {
        AttributeFormat<UniformAttribs> uniformFormat = gl.createAttributeFormat(UniformAttribs.class).useFor(AttributeUsage.UNIFORM).build();
        AttributeFormat<UniformArrayAttribs> uniformArrayFormat = gl.createAttributeFormat(UniformArrayAttribs.class).useFor(AttributeUsage.UNIFORM_ARRAY).build();
        AttributeFormat<GlobalAttribs> globalFormat = gl.createAttributeFormat(GlobalAttribs.class).useFor(AttributeUsage.DRAW_GLOBAL).build();
        TextureFormat2D textureFormat = gl.createTextureFormat2D(gl.createPixelFormat()
                .rgba()
                .type(PixelFormatChannelType.FLOATING_POINT)
                .range(PixelFormatChannelRange.ZERO_TO_ONE)
                .minBitDepth(8)
                .build(), "colorFactor").build();
        AttributeFormat<LocalAttribs> localFormat = gl.createAttributeFormat(LocalAttribs.class)
                .useFor(AttributeUsage.DRAW_LOCAL, AttributeUsage.TRANSFORM_INPUT, AttributeUsage.TRANSFORM_OUTPUT)
                .build();
        AttributeFormat<UniformSelectionAttribs> selectionUniformFormat = gl.createAttributeFormat(UniformSelectionAttribs.class).useFor(AttributeUsage.UNIFORM).build();

        if (false) { //debug stuff: allocate texture formats so i can read the method bytecode
            gl.createTextureFormat2D(gl.createPixelFormat()
                    .rgb()
                    .type(PixelFormatChannelType.FLOATING_POINT)
                    .range(PixelFormatChannelRange.NEGATIVE_ONE_TO_ONE)
                    .minBitDepth(16)
                    .build(), "colorFactor").build().createWriter(1, 1);

            gl.createTextureFormat2D(gl.createPixelFormat()
                    .rgb()
                    .type(PixelFormatChannelType.INTEGER)
                    .range(PixelFormatChannelRange.INFINITY)
                    .minBitDepth(16)
                    .build(), "colorFactor").build().createWriter(1, 1);

            gl.createTextureFormat2D(gl.createPixelFormat()
                    .rgb()
                    .type(PixelFormatChannelType.FLOATING_POINT)
                    .range(PixelFormatChannelRange.INFINITY)
                    .minBitDepth(30)
                    .build(), "colorFactor").build().createWriter(1, 1);
        }

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

        AttributeBuffer<LocalAttribs> localBuffer_2_in = localFormat.createBuffer(BufferUsage.STREAM_COPY);
        AttributeBuffer<LocalAttribs> localBuffer_2_out = localFormat.createBuffer(BufferUsage.STREAM_COPY);

        try (AttributeWriter<LocalAttribs> writer = localFormat.createWriter()) {
            writer.append().pos(16, 16).close();
            writer.append().pos(16, 32).close();
            writer.append().pos(32, 32).close();
            writer.append().pos(32, 16).close();

            localBuffer_1.set(0, writer);
            localBuffer_2_in.setContentsFrom(localBuffer_1);
            localBuffer_2_out.setContentsFrom(localBuffer_1);
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
                    writer.append()
                            .offset(x * 32, y * 32)
                            .color(0xFF000000 | color);
                }
            }
            globalBuffer.set(writer);
        }

        AttributeBuffer<UniformArrayAttribs> uniformArrayBuffer = uniformArrayFormat.createBuffer(BufferUsage.STATIC_DRAW);
        try (AttributeWriter<UniformArrayAttribs> writer = uniformArrayFormat.createWriter()) {
            writer.append().colorFactor(0.5f, 1.0f, 1.0f).close();
            writer.append().colorFactor(1.0f, 0.5f, 1.0f).close();
            writer.append().colorFactor(1.0f, 1.0f, 0.5f).close();

            uniformArrayBuffer.set(writer);
        }

        AttributeBuffer<UniformAttribs> uniformBuffer0 = uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);
        try (AttributeWriter<UniformAttribs> writer = uniformFormat.createWriter()) {
            writer.append().scale((byte) 32, (byte) 32).close();

            uniformBuffer0.set(writer);
        }

        AttributeBuffer<UniformAttribs> uniformBuffer1 = uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);
        try (UniformAttribs writer = uniformBuffer1.setToSingle()) {
            writer.scale((byte) -128, (byte) -128);
        }

        AttributeBuffer<UniformAttribs> uniformBuffer2 = uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);
        try (AttributeWriter<UniformAttribs> writer = uniformFormat.createWriter()) {
            writer.append().scale((byte) -64, (byte) 32).close();

            uniformBuffer2.set(writer);
        }

        Texture2D texture = textureFormat.createTexture(512, 512, 1);
        try (TextureWriter2D writer = textureFormat.createWriter(512, 512)) {
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    writer.setNormalizedUnsignedARGB8(x, y, ThreadLocalRandom.current().nextInt() | 0xFF000000);
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
                .withInput(localBuffer_2_in)
                .withOutput(localBuffer_2_out)
                .build();

        DrawBindingIndexed binding2_draw = drawLayout.createBinding()
                .withIndexes(indexBuffer)
                .withUniform(uniformBuffer2)
                .withUniformArray(uniformArrayBuffer)
                .withGlobal(globalBuffer)
                .withLocal(localBuffer_2_in)
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
                .drawArrays(drawShaderProgram, DrawMode.TRIANGLES, binding2_draw, 0, 3)
                .conditional(
                        () -> (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) & 1) == 0L, //only move every other second
                        builder -> builder
                                .transform(transformShaderProgram, binding2_transform, localBuffer_1.capacity())
                                .copy(localBuffer_2_out, localBuffer_2_in))
                .build()) {
            while (!closeRequested.getAsBoolean()) {
                try (AttributeWriter<UniformSelectionAttribs> writer = selectionUniformFormat.createWriter()) {
                    writer.append().selectable(ThreadLocalRandom.current().nextInt() & 1).close();

                    selectionUniformBuffer.set(writer);
                }

                cmdBuffer.execute();

                swapAndSync.run();
            }
        }
    }

    @Attribute(name = "scale",
            typeVector = @VectorType(components = 2,
                    componentType = @ScalarType(value = byte.class,
                            interpret = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true))))
    @Attribute(name = "floatsAsVector",
            typeVector = @VectorType(components = 3, componentType = @ScalarType(float.class)))
    @Attribute(name = "vec2Array",
            typeArray = @ArrayType(length = 4,
                    componentTypeVector = @VectorType(components = 2,
                            componentType = @ScalarType(value = byte.class,
                                    interpret = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = false)))))
    public interface UniformAttribs extends AttributeStruct {
        @AttributeSetter("scale")
        UniformAttribs scale(byte scaleX, byte scaleY);

        @AttributeSetter
        UniformAttribs floatsAsVector(float f0, float f1, float f2);

        @AttributeSetter
        UniformAttribs floatsAsVector(float @ArrayLength(3) [] f);

        @AttributeSetter
        UniformAttribs vec2Array(byte @ArrayLength(8) [] bytes);

        @AttributeSetter
        UniformAttribs vec2Array(byte @ArrayLength(4) [] @ArrayLength(2) [] bytes);

        @AttributeSetter
        UniformAttribs vec2Array(@ArrayIndex int index, byte @ArrayLength(2) [] bytes);
    }

    @Attribute(name = "colorFactor",
            typeVector = @VectorType(components = 3, componentType = @ScalarType(float.class)))
    public interface UniformArrayAttribs extends AttributeStruct {
        @AttributeSetter
        UniformArrayAttribs colorFactor(float colorFactorR, float colorFactorG, float colorFactorB);
    }

    @Attribute(name = "offset",
            typeVector = @VectorType(components = 2,
                    componentType = @ScalarType(value = byte.class,
                            interpret = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = false))))
    @Attribute(name = "color",
            typeVector = @VectorType(components = 4,
                    componentType = @ScalarType(value = byte.class,
                            interpret = {
                                    @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED),
                                    @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true)
                            })))
    public interface GlobalAttribs extends AttributeStruct {
        @AttributeSetter
        GlobalAttribs offset(int offsetX, int offsetY);

        @AttributeSetter
        GlobalAttribs color(@ScalarTransform(expand = @ScalarExpand(ScalarExpand.Type.INT_ARGB8_TO_BYTE_VECTOR_RGBA)) int color);
    }

    @Attribute(name = "pos",
            typeVector = @VectorType(components = 2,
                    componentType = @ScalarType(value = byte.class,
                            interpret = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = false))))
    public interface LocalAttribs extends AttributeStruct {
        @AttributeSetter
        LocalAttribs pos(int posX, int posY);
    }

    @Attribute(name = "selectable",
            typeScalar = @ScalarType(int.class))
    public interface UniformSelectionAttribs extends AttributeStruct {
        @AttributeSetter
        UniformSelectionAttribs selectable(int selectable);
    }
}
