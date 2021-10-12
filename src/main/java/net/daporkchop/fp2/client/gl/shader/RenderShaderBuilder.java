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
import lombok.SneakyThrows;
import lombok.With;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.util.Constants.*;
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

    @SneakyThrows(IOException.class)
    protected ProgramMeta meta() {
        ProgramMeta meta;
        try (InputStream in = MC.resourceManager.getResource(new ResourceLocation(MODID, ShaderManager.BASE_PATH + "/prog/" + this.programName + ".json")).getInputStream()) {
            meta = GSON.fromJson(new UTF8FileReader(in), ProgramMeta.class);
        }

        checkState(meta.vert != null, "Program \"%s\" has no vertex shaders!", this.programName);
        if (meta.xfb_varying != null) {
            checkState(meta.frag == null, "Program \"%s\" has both fragment shaders and transform feedback outputs!", this.programName);
        } else {
            checkState(meta.frag != null, "Program \"%s\" has no fragment shaders!", this.programName);
        }

        return meta;
    }

    @Override
    protected RenderShaderProgram supply() {
        ProgramMeta meta = this.meta();

        return new RenderShaderProgram(
                this.programName,
                meta.vert != null ? ShaderManager.get(meta.vert, ShaderManager.headers(this.defines), ShaderType.VERTEX) : null,
                meta.geom != null ? ShaderManager.get(meta.geom, ShaderManager.headers(this.defines), ShaderType.GEOMETRY) : null,
                meta.frag != null ? ShaderManager.get(meta.frag, ShaderManager.headers(this.defines), ShaderType.FRAGMENT) : null,
                null,
                meta.xfb_varying);
    }

    @Override
    protected void reload(@NonNull RenderShaderProgram program) {
        ProgramMeta meta = this.meta();

        program.reload(
                meta.vert != null ? ShaderManager.get(meta.vert, ShaderManager.headers(this.defines), ShaderType.VERTEX) : null,
                meta.geom != null ? ShaderManager.get(meta.geom, ShaderManager.headers(this.defines), ShaderType.GEOMETRY) : null,
                meta.frag != null ? ShaderManager.get(meta.frag, ShaderManager.headers(this.defines), ShaderType.FRAGMENT) : null,
                null,
                meta.xfb_varying);
    }

    /**
     * Metadata for a shader program.
     *
     * @author DaPorkchop_
     */
    private static final class ProgramMeta {
        public String[] vert;
        public String[] geom;
        public String[] frag;

        public String[] xfb_varying;
    }
}
