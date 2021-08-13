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

package net.daporkchop.fp2.client.gl.shader;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import net.daporkchop.fp2.client.gl.WorkGroupSize;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.util.EnumFacing;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class ComputeShaderBuilder extends ShaderManager.AbstractShaderBuilder<ComputeShaderBuilder, ComputeShaderProgram> {
    @NonNull
    @With
    protected final String programName;
    protected final Map<String, Object> defines;

    @With
    protected final WorkGroupSize workGroupSize;

    protected String headers(@NonNull ProgramMeta meta) {
        return ShaderManager.headers(ImmutableMap.<String, Object>builder()
                        .putAll(this.defines)
                        .put("COMPUTE_SHADER_LOCAL_SIZE_X", this.workGroupSize.x())
                        .put("COMPUTE_SHADER_LOCAL_SIZE_Y", this.workGroupSize.y())
                        .put("COMPUTE_SHADER_LOCAL_SIZE_Z", this.workGroupSize.z())
                        .put("COMPUTE_SHADER_LOCAL_SIZE_TOTAL", this.workGroupSize.totalSize())
                        .put("COMPUTE_SHADER_LOCAL_ENABLE_X", meta.localEnableAxes.contains(EnumFacing.Axis.X))
                        .put("COMPUTE_SHADER_LOCAL_ENABLE_Y", meta.localEnableAxes.contains(EnumFacing.Axis.Y))
                        .put("COMPUTE_SHADER_LOCAL_ENABLE_Z", meta.localEnableAxes.contains(EnumFacing.Axis.Z))
                        .put("COMPUTE_SHADER_GLOBAL_ENABLE_X", meta.globalEnableAxes.contains(EnumFacing.Axis.X))
                        .put("COMPUTE_SHADER_GLOBAL_ENABLE_Y", meta.globalEnableAxes.contains(EnumFacing.Axis.Y))
                        .put("COMPUTE_SHADER_GLOBAL_ENABLE_Z", meta.globalEnableAxes.contains(EnumFacing.Axis.Z))
                        .build(),
                PStrings.fastFormat("layout(local_size_x = %d, local_size_y = %d, local_size_z = %d) in;",
                        this.workGroupSize.x(), this.workGroupSize.y(), this.workGroupSize.z()));
    }

    @Override
    public ComputeShaderProgram link() {
        checkState(this.workGroupSize != null, "workGroupSize must be set!");
        return super.link();
    }

    @SneakyThrows(IOException.class)
    protected ProgramMeta meta() {
        ProgramMeta meta;
        try (InputStream in = ShaderManager.class.getResourceAsStream(PStrings.fastFormat("%s/prog/%s.json", ShaderManager.BASE_PATH, this.programName))) {
            checkArg(in != null, "Unable to find shader meta file: \"%s/prog/%s.json\"!", ShaderManager.BASE_PATH, this.programName);
            meta = GSON.fromJson(new UTF8FileReader(in), ProgramMeta.class);
        }

        checkState(meta.comp != null, "Program \"%s\" has no shaders!", this.programName);
        checkState(!meta.localEnableAxes.isEmpty(), "Program \"%s\" has no local axes enabled!", this.programName);
        checkState(!meta.globalEnableAxes.isEmpty(), "Program \"%s\" has no global axes enabled!", this.programName);

        return meta;
    }

    @Override
    protected ComputeShaderProgram supply() {
        ProgramMeta meta = this.meta();

        return new ComputeShaderProgram(
                this.programName,
                null, null, null,
                ShaderManager.get(meta.comp, this.headers(meta), ShaderType.COMPUTE),
                null,
                this.workGroupSize, meta.globalEnableAxes);
    }

    @Override
    protected void reload(@NonNull ComputeShaderProgram program) {
        ProgramMeta meta = this.meta();

        program.reload(
                null, null, null,
                ShaderManager.get(meta.comp, this.headers(meta), ShaderType.COMPUTE),
                null,
                meta.globalEnableAxes);
    }

    /**
     * Metadata for a compute shader program.
     *
     * @author DaPorkchop_
     */
    private static final class ProgramMeta {
        public Set<EnumFacing.Axis> localEnableAxes = EnumSet.allOf(EnumFacing.Axis.class);
        public Set<EnumFacing.Axis> globalEnableAxes = EnumSet.allOf(EnumFacing.Axis.class);
        public String[] comp;
    }
}
