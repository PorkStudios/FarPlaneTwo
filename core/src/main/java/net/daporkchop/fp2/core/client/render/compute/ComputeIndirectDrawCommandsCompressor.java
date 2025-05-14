/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.core.client.render.compute;

import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.client.shader.ShaderRegistration;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.draw.VertexMode;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.state.MultiBindHelper;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.math.PMath;

import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class ComputeIndirectDrawCommandsCompressor extends AbstractComputeShaderContainer {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = AbstractComputeShaderContainer.REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_draw_indirect) //the extension isn't actually needed, but this would be useless if indirect draw isn't supported
            .add(GLExtension.GL_ARB_shader_image_load_store) //glMemoryBarrier()
            .add(GLExtension.GL_ARB_shader_atomic_counters);

    private static final int SHADER_WORK_GROUP_SIZE = 64; //synced with resources/assets/fp2/shaders/comp/compress_indirect_draw_commands.comp

    private static final String SELECTED_FLAGS_SSBO_NAME = "B_SelectedFlags"; //synced with resources/assets/fp2/shaders/comp/compress_indirect_draw_commands.comp
    private static final int SELECTED_FLAGS_SSBO_BINDING = 0;

    private static final String SRC_COMMANDS_SSBO_NAME = "B_SrcCommands"; //synced with resources/assets/fp2/shaders/comp/compress_indirect_draw_commands.comp
    private static final int SRC_COMMANDS_SSBO_BINDING = SELECTED_FLAGS_SSBO_BINDING + 1;

    private static final String DST_COMMANDS_SSBO_NAME = "B_DstCommands"; //synced with resources/assets/fp2/shaders/comp/compress_indirect_draw_commands.comp
    private static final int DST_COMMANDS_SSBO_BINDING = SRC_COMMANDS_SSBO_BINDING + 1;

    private static final int COUNT_SELECTED_COUNTERS_BINDING = 0;

    private static final int MIN_PASS_COUNT = 1;
    private static final int MAX_PASS_COUNT = 8; //this is equal to the minimum guaranteed number of atomic counter bindings in a compute shader

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    private static final class CompressIndirectDrawCommandsVariant {
        final @NonNull VertexMode vertexMode;
        final int passCount;

        public ImmutableMap<String, Object> defines() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("FP2_COMPRESS_" + this.vertexMode.name(), true);
            builder.put("FP2_COMPRESS_PASS_COUNT", this.passCount);
            builder.put("COUNT_SELECTED_COUNTER_BINDING", COUNT_SELECTED_COUNTERS_BINDING);
            return builder.build();
        }

        public static List<CompressIndirectDrawCommandsVariant> allVariants() {
            VertexMode[] vertexModes = VertexMode.values();

            List<CompressIndirectDrawCommandsVariant> result = new ArrayList<>(vertexModes.length * (MAX_PASS_COUNT - MIN_PASS_COUNT + 1));
            for (val vertexMode : vertexModes) {
                for (int passCount = MIN_PASS_COUNT; passCount <= MAX_PASS_COUNT; passCount++) {
                    result.add(new CompressIndirectDrawCommandsVariant(vertexMode, passCount));
                }
            }
            return result;
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static final class RegisterShaders extends ShaderRegistration {
        public RegisterShaders() {
            super(REQUIRED_EXTENSIONS);
        }

        @Override
        public void registerShaders(@NonNull GlobalRenderer globalRenderer, @NonNull ReloadableShaderRegistry.Builder shaderRegistryBuilder, @NonNull ShaderMacros shaderMacros, @NonNull FP2Client client, @NonNull OpenGL gl) {
            for (val variant : CompressIndirectDrawCommandsVariant.allVariants()) {
                shaderRegistryBuilder.registerCompute(variant, shaderMacros.withDefined(variant.defines()), null)
                        .addShader(ShaderType.COMPUTE, Identifier.from(MODID, "shaders/comp/compress_indirect_draw_commands.comp"))
                        .addSSBO(SELECTED_FLAGS_SSBO_BINDING, SELECTED_FLAGS_SSBO_NAME)
                        .addSSBO(SRC_COMMANDS_SSBO_BINDING, SRC_COMMANDS_SSBO_NAME)
                        .addSSBO(DST_COMMANDS_SSBO_BINDING, DST_COMMANDS_SSBO_NAME);
            }
        }
    }

    private final @NonNull VertexMode vertexMode;

    private final @NonNull MultiBindHelper bindHelper_counts_flags_src_dst;

    public ComputeIndirectDrawCommandsCompressor(@NonNull OpenGL gl, @NonNull GlobalRenderer globalRenderer, @NonNull VertexMode vertexMode) {
        super(gl.checkSupported(REQUIRED_EXTENSIONS), globalRenderer);

        try {
            this.vertexMode = vertexMode;

            this.bindHelper_counts_flags_src_dst = MultiBindHelper.builder(gl)
                    .bindBufferRange(IndexedBufferTarget.ATOMIC_COUNTER_BUFFER, COUNT_SELECTED_COUNTERS_BINDING)
                    .bindBufferBase(IndexedBufferTarget.SHADER_STORAGE_BUFFER, SELECTED_FLAGS_SSBO_BINDING)
                    .bindBufferBase(IndexedBufferTarget.SHADER_STORAGE_BUFFER, SRC_COMMANDS_SSBO_BINDING)
                    .bindBufferBase(IndexedBufferTarget.SHADER_STORAGE_BUFFER, DST_COMMANDS_SSBO_BINDING)
                    .build();
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored = this.bindHelper_counts_flags_src_dst) {
            super.close();
        }
    }

    /**
     * Takes as input an array of indirect draw commands and a corresponding array of uints which act as a mask. All draw commands
     * whose mask value is non-zero and whose vertex count is non-zero will be written tightly packed into the destination array,
     * leaving out all commands which do not fulfil both conditions. The total number of draw commands written is accumulated as
     * an atomic counter buffer, which must be manually cleared to zero before this function is called.
     * <p>
     * Draw commands from multiple render passes can be compressed simultaneously, sharing the same mask element. When compressing
     * N commands in P render passes, the first render pass's commands start at index 0*N, then 1*N, etc. relative to the src/dst
     * buffer starts. This also uses P atomic counter variables.
     * <p>
     * This method does <strong>not</strong> invalidate the destination buffer's storage, nor does it clear the selected counts buffer to zero.
     * <p>
     * The generated draw commands and selected command counts are written to their corresponding output buffers incoherently. In order
     * to read the generated values, you must either wait until all commands have completed (using a
     * {@link net.daporkchop.fp2.gl.sync.GLFenceSync fence sync object}), or call {@link OpenGL#glMemoryBarrier(int) glMemoryBarrier}
     * before the GL call which would read from the buffers.
     *
     * @param srcCommandCount            the number of draw commands per render pass
     * @param passCount                  the number of render passes
     * @param selectedFlagsBuffer        the buffer containing the mask array, {@code srcCommandCount} uints
     * @param srcCommandsBuffer          the buffer containing the source indirect draw commands, {@code srcCommandCount * passCount} commands
     * @param dstCommandsBuffer          the buffer to write the compressed indirect draw commands to, {@code srcCommandCount * passCount} commands
     * @param selectedCountsBuffer       the buffer containing the number of draw commands written to the destination buffer for each render pass, {@code passCount} uints
     * @param selectedCountsBufferOffset the offset from the beginning of {@code selectedCountsBuffer} to bind
     */
    public void compressCommands(
            @NotNegative int srcCommandCount,
            @Positive int passCount,
            @NonNull GLBuffer selectedFlagsBuffer,
            @NonNull GLBuffer srcCommandsBuffer,
            @NonNull GLBuffer dstCommandsBuffer,
            @NonNull GLBuffer selectedCountsBuffer, @NotNegative long selectedCountsBufferOffset) {
        checkIndex(MIN_PASS_COUNT, MAX_PASS_COUNT + 1, passCount);
        notNegative(srcCommandCount, "srcCommandCount");

        long selectedFlagsBufferSize = (long) srcCommandCount * Integer.BYTES;
        long srcCommandsBufferSize = (long) srcCommandCount * passCount * this.vertexMode.indirectCommandSize();
        long dstCommandsBufferSize = (long) srcCommandCount * passCount * this.vertexMode.indirectCommandSize();
        long selectedCountsBufferSize = (long) passCount * Integer.BYTES;

        checkArg(selectedFlagsBuffer.capacity() == selectedFlagsBufferSize, "selectedFlagsBuffer has incorrect size!");
        checkArg(srcCommandsBuffer.capacity() == srcCommandsBufferSize, "srcCommandsBuffer has incorrect size!");
        checkArg(dstCommandsBuffer.capacity() == dstCommandsBufferSize, "dstCommandsBuffer has incorrect size!");
        checkRangeLen(selectedCountsBuffer.capacity(), selectedCountsBufferOffset, selectedCountsBufferSize);

        if (srcCommandCount <= 0) {
            return;
        }

        val shader = this.shaderRegistry.<ComputeShaderProgram>get(new CompressIndirectDrawCommandsVariant(this.vertexMode, passCount)).get();
        val uniformSetter = shader.bindUnsafe();

        uniformSetter.set1ui(shader.uniformLocation("u_srcCommandCount"), srcCommandCount);

        //bind all the buffers
        this.bindHelper_counts_flags_src_dst.dispatcher().invokeExact(
                this.gl,
                selectedCountsBuffer.id(), selectedCountsBufferOffset, selectedCountsBufferSize,
                selectedFlagsBuffer.id(),
                srcCommandsBuffer.id(),
                dstCommandsBuffer.id());

        this.gl.glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

        //dispatch the compute shader!
        this.gl.glDispatchCompute(PMath.ceilDiv(srcCommandCount, SHADER_WORK_GROUP_SIZE), 1, 1);
    }

    @Override
    public void configureModifiedState(@NonNull StatePreserver.Builder builder) {
        builder.activeProgram()
                .indexedBuffer(IndexedBufferTarget.ATOMIC_COUNTER_BUFFER, COUNT_SELECTED_COUNTERS_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, SELECTED_FLAGS_SSBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, SELECTED_FLAGS_SSBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, SELECTED_FLAGS_SSBO_BINDING);
    }
}
