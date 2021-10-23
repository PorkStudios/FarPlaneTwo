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
import net.minecraft.util.ResourceLocation;

import java.util.Map;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = false)
public final class RenderShaderBuilder extends ShaderManager.AbstractShaderBuilder<RenderShaderBuilder, RenderShaderProgram> {
    @NonNull
    @With
    protected final String programName;

    @With
    protected final ResourceLocation vertexShader;
    @With
    protected final ResourceLocation geometryShader;
    @With
    protected final ResourceLocation fragmentShader;

    @With(AccessLevel.PROTECTED)
    protected final Map<String, Object> defines;

    @Override
    public RenderShaderBuilder define(@NonNull String name, @NonNull Object value) {
        if (!value.equals(this.defines.get(name))) {
            Map<String, Object> defines = new Object2ObjectOpenHashMap<>(this.defines);
            defines.put(name, value);
            return this.withDefines(ImmutableMap.copyOf(defines));
        }
        return this;
    }

    @Override
    public RenderShaderBuilder undefine(@NonNull String name) {
        if (this.defines.containsKey(name)) {
            Map<String, Object> defines = new Object2ObjectOpenHashMap<>(this.defines);
            defines.remove(name);
            return this.withDefines(ImmutableMap.copyOf(defines));
        }
        return this;
    }

    @Override
    public RenderShaderProgram link() {
        checkState(this.vertexShader != null, "vertexShader must be set for %s", this.programName);
        /*if (meta.xfb_varying != null) {
            checkState(meta.frag == null, "Program \"%s\" has both fragment shaders and transform feedback outputs!", this.programName);
        } else {*/
        checkState(this.fragmentShader != null, "fragmentShader must be set for %s", this.programName);
        //}

        return super.link();
    }

    @Override
    protected RenderShaderProgram supply() {
        return new RenderShaderProgram(
                this.programName,
                this.vertexShader != null ? ShaderManager.get(this.vertexShader, ShaderManager.GLOBAL_DEFINES, ShaderType.VERTEX) : null,
                this.geometryShader != null ? ShaderManager.get(this.geometryShader, ShaderManager.GLOBAL_DEFINES, ShaderType.GEOMETRY) : null,
                this.fragmentShader != null ? ShaderManager.get(this.fragmentShader, ShaderManager.GLOBAL_DEFINES, ShaderType.FRAGMENT) : null,
                null,
                null);
    }

    @Override
    protected void reload(@NonNull RenderShaderProgram program) {
        program.reload(
                this.vertexShader != null ? ShaderManager.get(this.vertexShader, ShaderManager.GLOBAL_DEFINES, ShaderType.VERTEX) : null,
                this.geometryShader != null ? ShaderManager.get(this.geometryShader, ShaderManager.GLOBAL_DEFINES, ShaderType.GEOMETRY) : null,
                this.fragmentShader != null ? ShaderManager.get(this.fragmentShader, ShaderManager.GLOBAL_DEFINES, ShaderType.FRAGMENT) : null,
                null,
                null);
    }
}
