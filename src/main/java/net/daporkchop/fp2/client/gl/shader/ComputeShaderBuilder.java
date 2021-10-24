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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;
import net.daporkchop.fp2.gl.compute.ComputeLocalSize;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@With
@EqualsAndHashCode(callSuper = false)
public final class ComputeShaderBuilder extends ShaderManager.AbstractShaderBuilder<ComputeShaderBuilder, ComputeShaderProgram> {
    @NonNull
    protected final String programName;

    protected final ResourceLocation computeShader;

    protected final ComputeLocalSize workGroupSize;

    @NonNull
    protected final Set<EnumFacing.Axis> localEnableAxes;

    @NonNull
    protected final Set<EnumFacing.Axis> globalEnableAxes;

    @With(AccessLevel.PROTECTED)
    @NonNull
    protected final Map<String, Object> defines;

    /**
     * @see ShaderManager#computeShaderBuilder(String)
     */
    protected ComputeShaderBuilder(@NonNull String programName) {
        this.programName = programName;
        this.computeShader = null;
        this.workGroupSize = null;
        this.localEnableAxes = this.globalEnableAxes = EnumSet.allOf(EnumFacing.Axis.class);
        this.defines = Collections.emptyMap();
    }

    @Override
    public ComputeShaderBuilder define(@NonNull String name, @NonNull Object value) {
        if (!value.equals(this.defines.get(name))) {
            Map<String, Object> defines = new Object2ObjectOpenHashMap<>(this.defines);
            defines.put(name, value);
            return this.withDefines(ImmutableMap.copyOf(defines));
        }
        return this;
    }

    @Override
    public ComputeShaderBuilder undefine(@NonNull String name) {
        if (this.defines.containsKey(name)) {
            Map<String, Object> defines = new Object2ObjectOpenHashMap<>(this.defines);
            defines.remove(name);
            return this.withDefines(ImmutableMap.copyOf(defines));
        }
        return this;
    }

    protected Map<String, Object> macros() {
        return ImmutableMap.<String, Object>builder()
                .putAll(ShaderManager.GLOBAL_DEFINES)
                .putAll(this.defines)
                .put("COMPUTE_SHADER_LOCAL_SIZE_X", this.workGroupSize.x())
                .put("COMPUTE_SHADER_LOCAL_SIZE_Y", this.workGroupSize.y())
                .put("COMPUTE_SHADER_LOCAL_SIZE_Z", this.workGroupSize.z())
                .put("COMPUTE_SHADER_LOCAL_SIZE_TOTAL", this.workGroupSize.count())
                .put("COMPUTE_SHADER_LOCAL_ENABLE_X", this.localEnableAxes.contains(EnumFacing.Axis.X))
                .put("COMPUTE_SHADER_LOCAL_ENABLE_Y", this.localEnableAxes.contains(EnumFacing.Axis.Y))
                .put("COMPUTE_SHADER_LOCAL_ENABLE_Z", this.localEnableAxes.contains(EnumFacing.Axis.Z))
                .put("COMPUTE_SHADER_GLOBAL_ENABLE_X", this.globalEnableAxes.contains(EnumFacing.Axis.X))
                .put("COMPUTE_SHADER_GLOBAL_ENABLE_Y", this.globalEnableAxes.contains(EnumFacing.Axis.Y))
                .put("COMPUTE_SHADER_GLOBAL_ENABLE_Z", this.globalEnableAxes.contains(EnumFacing.Axis.Z))
                .build();
    }

    @Override
    public ComputeShaderProgram link() {
        checkState(this.computeShader != null, "computeShader must be set!");
        checkState(this.workGroupSize != null, "workGroupSize must be set!");
        checkState(!this.localEnableAxes.isEmpty(), "program \"%s\" has no local axes enabled!", this.programName);
        checkState(!this.globalEnableAxes.isEmpty(), "program \"%s\" has no global axes enabled!", this.programName);
        return super.link();
    }

    @Override
    protected ComputeShaderProgram supply() {
        return new ComputeShaderProgram(
                this.programName,
                null, null, null,
                ShaderManager.get(this.computeShader, this.macros(), ShaderType.COMPUTE),
                null,
                this.workGroupSize, this.globalEnableAxes);
    }

    @Override
    protected void reload(@NonNull ComputeShaderProgram program) {
        program.reload(
                null, null, null,
                ShaderManager.get(this.computeShader, this.macros(), ShaderType.COMPUTE),
                null,
                this.globalEnableAxes);
    }
}
