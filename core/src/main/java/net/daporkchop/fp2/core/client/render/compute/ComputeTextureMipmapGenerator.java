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
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.state.MultiBindHelper;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.texture.GLTexture2D;
import net.daporkchop.fp2.gl.texture.PixelComponentType;
import net.daporkchop.fp2.gl.texture.PixelKind;
import net.daporkchop.fp2.gl.texture.TextureInternalFormat;
import net.daporkchop.fp2.gl.texture.TextureTarget;
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
    private static final int MAX_LEVELS_PER_DISPATCH = 4; //synced with resources/assets/fp2/shaders/comp/generate_mipmap.comp

    private static final int SRC_SAMPLER_BINDING = 7; //TODO: improve this
    private static final int SRC_IMAGE_BINDING = 0;
    private static final int DST_IMAGE_BINDING_BASE = SRC_IMAGE_BINDING + 1;

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
        final boolean fastReduction;

        public ImmutableMap<String, Object> defines() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("FP2_MIPMAP_MODE", this.mode.ordinal());
            builder.put("FP2_MIPMAP_SRC_SAMPLER", this.srcSampler);
            builder.put("FP2_MIPMAP_FAST_REDUCTION", this.fastReduction);

            builder.put("FP2_MIPMAP_FORMAT_IMAGE_LAYOUT", this.imageFormat.name().toLowerCase(Locale.ROOT));
            builder.put("FP2_MIPMAP_FORMAT_IMAGE_TYPE", this.imageFormat.sampledType().glslPrefix() + "image2D");
            builder.put("FP2_MIPMAP_FORMAT_SAMPLER_TYPE", this.imageFormat.sampledType().glslPrefix() + "sampler2D");
            builder.put("FP2_MIPMAP_FORMAT_RAW_TEXEL_TYPE", this.imageFormat.sampledType().glslPrefix() + "vec4");
            return builder.build();
        }

        public static List<MipmapGeneratorShaderVariant> allVariants() {
            TextureInternalFormat[] imageFormats = TextureInternalFormat.colorFormatsFloat();
            MipmapMode[] mipmapModes = MipmapMode.values();
            boolean[] srcSamplers = { false, true };
            boolean[] fastReductions = { false, true };

            List<MipmapGeneratorShaderVariant> result = new ArrayList<>(imageFormats.length * mipmapModes.length * srcSamplers.length * fastReductions.length);
            for (val imageFormat : imageFormats) {
                if (imageFormat.defaultFormat().components() == 3) { //3-component formats aren't supported by image load/store
                    continue;
                }

                for (val mipmapMode : mipmapModes) {
                    for (val srcSampler : srcSamplers) {
                        for (val fastReduction : fastReductions) {
                            result.add(new MipmapGeneratorShaderVariant(mipmapMode, imageFormat, srcSampler, fastReduction));
                        }
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
        public void registerShaders(@NonNull GlobalRenderer globalRenderer, @NonNull ReloadableShaderRegistry.Builder shaderRegistryBuilder, @NonNull ShaderMacros shaderMacros, @NonNull FP2Client client, @NonNull OpenGL gl) {
            for (val variant : MipmapGeneratorShaderVariant.allVariants()) {
                shaderRegistryBuilder.registerCompute(variant, shaderMacros.withDefined(variant.defines()), builder -> builder
                                .addSampler(SRC_SAMPLER_BINDING, "u_srcTexture")
                                .addImage(SRC_IMAGE_BINDING, "u_srcImage"))
                        .addShader(ShaderType.COMPUTE, Identifier.from(MODID, "shaders/comp/generate_mipmap.comp"))
                        .addImages(DST_IMAGE_BINDING_BASE, MAX_LEVELS_PER_DISPATCH, "u_dstImages");
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

        checkArg(srcTexture.widthAtLevel(srcLevel + 1) == dstTexture.widthAtLevel(dstLevel)
                        && srcTexture.heightAtLevel(srcLevel + 1) == dstTexture.heightAtLevel(dstLevel),
                "src and dst texture resolutions don't match!");

        TextureInternalFormat srcFormat = srcTexture.internalFormat();
        TextureInternalFormat dstFormat = dstTexture.internalFormat();
        PixelComponentType sampledType = srcFormat.sampledType();
        checkArg(sampledType == dstFormat.sampledType(), "src: %s, dst: %s", sampledType, dstFormat.sampledType());
        checkArg(dstFormat.defaultFormat().kind() == PixelKind.COLOR, "cannot write mipmaps to a %s texture", dstFormat.defaultFormat().kind());

        ComputeShaderProgram shader = null;
        ShaderProgram.UniformSetter uniformSetter = null;
        int u_levelsThisDispatch = -1;

        int currentlyBoundDstImageCount = 0;

        GLTexture2D prevTexture = srcTexture;
        int prevLevel = srcLevel;

        for (int processedLevels = 0; processedLevels < levels; ) {
            int remainingLevels = levels - processedLevels;

            int remainingEvenSizedLevels = Math.min(
                    Integer.numberOfTrailingZeros(prevTexture.widthAtLevel(prevLevel) | prevTexture.heightAtLevel(prevLevel)),
                    remainingLevels);

            boolean useFastReduction = remainingEvenSizedLevels >= 2;

            int levelsThisDispatch;
            if (useFastReduction) {
                levelsThisDispatch = Math.min(remainingEvenSizedLevels, MAX_LEVELS_PER_DISPATCH);
            } else {
                //TODO: we could theoretically process up to two mipmaps at a time here, like https://github.com/nvpro-samples/vk_compute_mipmaps
                levelsThisDispatch = 1;
            }

            //if the source is a depth texture, the first pass needs to read from the source texture using a sampler
            boolean useSamplerSrc = prevTexture.internalFormat().defaultFormat().kind() != PixelKind.COLOR;

            //switch to the necessary shader for the current configuration
            ComputeShaderProgram nextShader = this.shaderRegistry.<ComputeShaderProgram>get(new MipmapGeneratorShaderVariant(mode, dstFormat, useSamplerSrc, useFastReduction)).get();
            if (nextShader != shader) {
                shader = nextShader;
                uniformSetter = nextShader.bindUnsafe();

                u_levelsThisDispatch = shader.uniformLocation("u_levelsThisDispatch");
            }

            int currLevelWidth = dstTexture.widthAtLevel(dstLevel + processedLevels);
            int currLevelHeight = dstTexture.heightAtLevel(dstLevel + processedLevels);

            int numGroupsX = PMath.roundUp(currLevelWidth, SHADER_WORK_GROUP_TILE_SIZE) / SHADER_WORK_GROUP_TILE_SIZE;
            int numGroupsY = PMath.roundUp(currLevelHeight, SHADER_WORK_GROUP_TILE_SIZE) / SHADER_WORK_GROUP_TILE_SIZE;

            uniformSetter.set1ui(u_levelsThisDispatch, levelsThisDispatch);

            if (useSamplerSrc) {
                uniformSetter.set1i(shader.uniformLocation("u_srcTextureLod"), prevLevel);
                prevTexture.bindToUnitUnsafe(SRC_SAMPLER_BINDING);
            } else {
                //for subsequent levels, reduce based on the previous mipmap level written to the destination
                this.gl.glBindImageTexture(SRC_IMAGE_BINDING, prevTexture.id(), prevLevel, false, 0, GL_READ_ONLY, prevTexture.internalFormat().id());
            }

            for (int dispatchLevel = 0; dispatchLevel < levelsThisDispatch; dispatchLevel++) {
                this.gl.glBindImageTexture(DST_IMAGE_BINDING_BASE + dispatchLevel, dstTexture.id(), dstLevel + processedLevels + dispatchLevel, false, 0, GL_WRITE_ONLY, dstTexture.internalFormat().id());
            }

            //if there are more images bound than there were in the previous pass, unbind them
            if (levelsThisDispatch < currentlyBoundDstImageCount) {
                MultiBindHelper.unbindImageTextures(this.gl, DST_IMAGE_BINDING_BASE + levelsThisDispatch, currentlyBoundDstImageCount - levelsThisDispatch);
            }
            currentlyBoundDstImageCount = levelsThisDispatch;

            this.gl.glMemoryBarrier(GL_TEXTURE_FETCH_BARRIER_BIT | GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            this.gl.glDispatchCompute(numGroupsX, numGroupsY, 1);

            prevTexture = dstTexture;
            prevLevel = dstLevel + processedLevels + levelsThisDispatch - 1;

            processedLevels += levelsThisDispatch;
        }

        //unbind all the images again
        //this is necessary to work around a bug in mesa which seems to cause things to break if we leave the images bound
        MultiBindHelper.unbindImageTextures(this.gl, SRC_IMAGE_BINDING, 1 + currentlyBoundDstImageCount);
    }

    @Override
    public void configureModifiedState(@NonNull StatePreserver.Builder builder) {
        builder.activeProgram();
        builder.texture(TextureTarget.TEXTURE_2D, SRC_SAMPLER_BINDING);
        builder.image(SRC_IMAGE_BINDING);
        builder.images(DST_IMAGE_BINDING_BASE, MAX_LEVELS_PER_DISPATCH);
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
