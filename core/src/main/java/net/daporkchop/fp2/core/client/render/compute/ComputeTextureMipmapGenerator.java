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
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.texture.GLTexture2D;
import net.daporkchop.fp2.gl.texture.PixelComponentType;
import net.daporkchop.fp2.gl.texture.PixelKind;
import net.daporkchop.fp2.gl.texture.TextureInternalFormat;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.math.PMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Helper class for generating mipmaps for a texture using a given reduction function.
 *
 * @author DaPorkchop_
 */
public final class ComputeTextureMipmapGenerator extends AbstractComputeShaderContainer {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = AbstractComputeShaderContainer.REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_shader_image_load_store);

    private static final int SHADER_WORK_GROUP_TILE_SIZE = 16; //synced with resources/assets/fp2/shaders/comp/generate_mipmap.comp
    private static final int MAX_LEVELS_PER_DISPATCH = 1; //synced with resources/assets/fp2/shaders/comp/generate_mipmap.comp

    private static final int SRC_SAMPLER_BINDING = 0;
    private static final int SRC_IMAGE_BINDING = 0;
    private static final int DST_IMAGE_BINDING = SRC_IMAGE_BINDING + 1;

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    private static final class MipmapGeneratorShaderVariant {
        final @NonNull MipmapMode mode;
        final @NonNull TextureInternalFormat imageFormat;
        final boolean srcSampler;

        public ImmutableMap<String, Object> defines() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("FP2_MIPMAP_MODE", this.mode.ordinal());
            builder.put("FP2_MIPMAP_SRC_SAMPLER", this.srcSampler);

            builder.put("FP2_MIPMAP_FORMAT_IMAGE_LAYOUT", this.imageFormat.name().toLowerCase(Locale.ROOT));
            builder.put("FP2_MIPMAP_FORMAT_IMAGE_TYPE", this.imageFormat.sampledType().glslPrefix() + "image2D");
            builder.put("FP2_MIPMAP_FORMAT_SAMPLER_TYPE", this.imageFormat.sampledType().glslPrefix() + "sampler2D");
            builder.put("FP2_MIPMAP_FORMAT_RAW_TEXEL_TYPE", this.imageFormat.sampledType().glslPrefix() + "vec4");
            return builder.build();
        }

        public static List<MipmapGeneratorShaderVariant> allVariants() {
            TextureInternalFormat[] imageFormats = TextureInternalFormat.colorFormatsFloat();
            MipmapMode[] mipmapModes = MipmapMode.values();
            val srcSamplers = new boolean[]{false, true};

            List<MipmapGeneratorShaderVariant> result = new ArrayList<>(imageFormats.length * mipmapModes.length * srcSamplers.length);
            for (val imageFormat : imageFormats) {
                if (imageFormat.defaultFormat().components() == 3) { //3-component formats aren't supported by image load/store
                    continue;
                }

                for (val mipmapMode : mipmapModes) {
                    for (val srcSampler : srcSamplers) {
                        result.add(new MipmapGeneratorShaderVariant(mipmapMode, imageFormat, srcSampler));
                    }
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
        public void registerShaders(@NonNull GlobalRenderer globalRenderer, @NonNull ReloadableShaderRegistry shaderRegistry, @NonNull ShaderMacros shaderMacros, @NonNull FP2Client client, @NonNull OpenGL gl) {
            for (val variant : MipmapGeneratorShaderVariant.allVariants()) {
                shaderRegistry.createCompute(variant, shaderMacros.withDefined(variant.defines()), builder -> builder
                                .addSampler(SRC_SAMPLER_BINDING, "u_srcTexture")
                                .addImage(SRC_IMAGE_BINDING, "u_srcImage")
                                .addImage(DST_IMAGE_BINDING, "u_dstImage"))
                        .addShader(ShaderType.COMPUTE, Identifier.from(MODID, "shaders/comp/generate_mipmap.comp"))
                        .build();
            }
        }
    }

    public ComputeTextureMipmapGenerator(@NonNull OpenGL gl, @NonNull GlobalRenderer globalRenderer) {
        super(gl.checkSupported(REQUIRED_EXTENSIONS), globalRenderer);
    }

    /**
     * Generates the given number of mipmap levels from the given level of the given source texture into the
     * given destination texture starting at the given level. Texels are resampled according to the given
     * {@link MipmapMode mode}.
     * <p>
     * This method does <strong>not</strong> invalidate the destination texture's storage.
     * <p>
     * The generated mipmaps are written to the destination texture incoherently. In order to read the generated mipmaps,
     * you must either wait until all commands have completed (using a {@link net.daporkchop.fp2.gl.sync.GLFenceSync fence sync object}),
     * or call {@link OpenGL#glMemoryBarrier(int) glMemoryBarrier} before the GL call which would read the images.
     *
     * @param srcTexture the source texture
     * @param srcLevel   the level of the source texture to generate mipmaps from
     * @param dstTexture the destination texture
     * @param dstLevel   the first level of the destination texture to write the generated mipmaps to
     * @param levels     the number of mipmap levels to be generated
     * @param mode       the texel resampling mode
     */
    public void generateMipmaps(@NonNull GLTexture2D srcTexture, @NotNegative int srcLevel,
                                @NonNull GLTexture2D dstTexture, @NotNegative int dstLevel,
                                @NotNegative int levels,
                                @NonNull MipmapMode mode) {
        checkIndex(srcTexture.levels(), srcLevel);
        checkRangeLen(dstTexture.levels(), dstLevel, levels);

        checkArg(Math.max(srcTexture.width() >> (srcLevel + 1), 1) == Math.max(dstTexture.width() >> dstLevel, 1)
                        && Math.max(srcTexture.height() >> (srcLevel + 1), 1) == Math.max(dstTexture.height() >> dstLevel, 1),
                "src and dst texture resolutions don't match!");

        TextureInternalFormat srcFormat = srcTexture.internalFormat();
        TextureInternalFormat dstFormat = dstTexture.internalFormat();
        PixelComponentType sampledType = srcFormat.sampledType();
        checkArg(sampledType == dstFormat.sampledType(), "src: %s, dst: %s", sampledType, dstFormat.sampledType());
        checkArg(dstFormat.defaultFormat().kind() == PixelKind.COLOR, "cannot write mipmaps to a %s texture", dstFormat.defaultFormat().kind());

        //if the source is a depth texture, the first pass needs to read from the source texture using a sampler
        boolean srcSampler = srcFormat.defaultFormat().kind() != PixelKind.COLOR;

        //if the source texture needs to be read by a sampler and the mipmap generation will need more than one shader dispatch,
        //  we'll separate the first dispatch from subsequent ones so that all subsequent shader invocations read from their parent
        //  level using image load/store instead of a sampler
        if (srcSampler && levels > MAX_LEVELS_PER_DISPATCH) {
            this.generateMipmaps(
                    srcTexture, srcLevel,
                    dstTexture, dstLevel,
                    MAX_LEVELS_PER_DISPATCH,
                    mode);

            this.generateMipmaps(
                    dstTexture, dstLevel + MAX_LEVELS_PER_DISPATCH,
                    dstTexture, dstLevel + MAX_LEVELS_PER_DISPATCH,
                    levels - MAX_LEVELS_PER_DISPATCH,
                    mode);
            return;
        }

        val shader = this.shaderRegistry.<ComputeShaderProgram>get(new MipmapGeneratorShaderVariant(mode, dstFormat, srcSampler)).get();
        val uniformSetter = shader.bindUnsafe();

        for (int level = 0; level < levels; level += MAX_LEVELS_PER_DISPATCH) {
            int levelsThisDispatch = Math.min(levels - level, MAX_LEVELS_PER_DISPATCH);

            int dstWidthThisDispatch = Math.max(dstTexture.width() >> (dstLevel + level), 1);
            int dstHeightThisDispatch = Math.max(dstTexture.height() >> (dstLevel + level), 1);

            int numGroupsX = PMath.roundUp(dstWidthThisDispatch, SHADER_WORK_GROUP_TILE_SIZE) / SHADER_WORK_GROUP_TILE_SIZE;
            int numGroupsY = PMath.roundUp(dstHeightThisDispatch, SHADER_WORK_GROUP_TILE_SIZE) / SHADER_WORK_GROUP_TILE_SIZE;

            if (srcSampler) {
                uniformSetter.set1i(shader.uniformLocation("u_srcTextureLod"), srcLevel + level);
                srcTexture.bindToUnitUnsafe(SRC_SAMPLER_BINDING);
            } else {
                //TODO: on subsequent dispatches, bind the destination texture instead of the source
                this.gl.glBindImageTexture(SRC_IMAGE_BINDING, srcTexture.id(), srcLevel, false, 0, GL_READ_ONLY, srcTexture.internalFormat().id());
            }

            for (int dispatchLevel = 0; dispatchLevel < levelsThisDispatch; dispatchLevel++) {
                this.gl.glBindImageTexture(DST_IMAGE_BINDING + dispatchLevel, dstTexture.id(), dstLevel + level + dispatchLevel, false, 0, GL_WRITE_ONLY, dstTexture.internalFormat().id());
            }

            this.gl.glMemoryBarrier(GL_TEXTURE_FETCH_BARRIER_BIT | GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            this.gl.glDispatchCompute(numGroupsX, numGroupsY, 1);

            level += levelsThisDispatch;
        }

        //this.gl.glBindImageTextures(DST_IMAGE_BINDING, Math.min(levels, MAX_LEVELS_PER_DISPATCH));
    }

    @Override
    public void configureModifiedState(@NonNull StatePreserver.Builder builder) {
        builder.activeProgram();
        throw new AbstractMethodError("unimplemented"); //TODO
    }

    /**
     * @author DaPorkchop_
     */
    public enum MipmapMode {
        MIN,
        MAX,
        ;
    }
}
