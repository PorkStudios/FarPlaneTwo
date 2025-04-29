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
public final class ComputeTextureCopier extends AbstractComputeShaderContainer {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = AbstractComputeShaderContainer.REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_shader_image_load_store);

    private static final int SHADER_WORK_GROUP_TILE_SIZE = 16; //synced with resources/assets/fp2/shaders/comp/texture_copy.comp

    private static final int SRC_SAMPLER_BINDING = 7; //TODO: improve this
    private static final int SRC_IMAGE_BINDING = 0;
    private static final int DST_IMAGE_BINDING = SRC_IMAGE_BINDING + 1;

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    private static final class TextureCopyShaderVariant {
        final @NonNull TextureInternalFormat imageFormat;
        final boolean srcSampler;

        public ImmutableMap<String, Object> defines() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("FP2_COPY_SRC_SAMPLER", this.srcSampler);

            builder.put("FP2_COPY_FORMAT_IMAGE_LAYOUT", this.imageFormat.name().toLowerCase(Locale.ROOT));
            builder.put("FP2_COPY_FORMAT_IMAGE_TYPE", this.imageFormat.sampledType().glslPrefix() + "image2D");
            builder.put("FP2_COPY_FORMAT_SAMPLER_TYPE", this.imageFormat.sampledType().glslPrefix() + "sampler2D");
            return builder.build();
        }

        public static List<TextureCopyShaderVariant> allVariants() {
            TextureInternalFormat[] imageFormats = TextureInternalFormat.colorFormatsFloat();
            val srcSamplers = new boolean[]{false, true};

            List<TextureCopyShaderVariant> result = new ArrayList<>(imageFormats.length * srcSamplers.length);
            for (val imageFormat : imageFormats) {
                if (imageFormat.defaultFormat().components() == 3) { //3-component formats aren't supported by image load/store
                    continue;
                }

                for (val srcSampler : srcSamplers) {
                    result.add(new TextureCopyShaderVariant(imageFormat, srcSampler));
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
            for (val variant : TextureCopyShaderVariant.allVariants()) {
                shaderRegistryBuilder.registerCompute(variant, shaderMacros.withDefined(variant.defines()), builder -> builder
                                .addSampler(SRC_SAMPLER_BINDING, "u_srcTexture")
                                .addImage(SRC_IMAGE_BINDING, "u_srcImage")
                                .addImage(DST_IMAGE_BINDING, "u_dstImage"))
                        .addShader(ShaderType.COMPUTE, Identifier.from(MODID, "shaders/comp/texture_copy.comp"));
            }
        }
    }

    public ComputeTextureCopier(@NonNull OpenGL gl, @NonNull GlobalRenderer globalRenderer) {
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
     */
    public void copyTextureLevel(@NonNull GLTexture2D srcTexture, @NotNegative int srcLevel,
                                 @NonNull GLTexture2D dstTexture, @NotNegative int dstLevel) {
        checkIndex(srcTexture.levels(), srcLevel);
        checkIndex(dstTexture.levels(), dstLevel);

        int srcWidth = Math.max(srcTexture.width() >> srcLevel, 1);
        int srcHeight = Math.max(srcTexture.height() >> srcLevel, 1);
        int dstWidth = Math.max(dstTexture.width() >> dstLevel, 1);
        int dstHeight = Math.max(dstTexture.height() >> dstLevel, 1);

        checkArg(srcWidth == dstWidth && srcHeight == dstHeight, "src and dst texture resolutions don't match!");

        TextureInternalFormat srcFormat = srcTexture.internalFormat();
        TextureInternalFormat dstFormat = dstTexture.internalFormat();
        PixelComponentType sampledType = srcFormat.sampledType();
        checkArg(sampledType == dstFormat.sampledType(), "src: %s, dst: %s", sampledType, dstFormat.sampledType());
        checkArg(dstFormat.defaultFormat().kind() == PixelKind.COLOR, "cannot copy to a %s texture", dstFormat.defaultFormat().kind());

        //if the source is a depth texture, the first pass needs to read from the source texture using a sampler
        boolean srcSampler = srcFormat.defaultFormat().kind() != PixelKind.COLOR;
        srcSampler = true; //TODO

        val shader = this.shaderRegistry.<ComputeShaderProgram>get(new TextureCopyShaderVariant(dstFormat, srcSampler)).get();
        val uniformSetter = shader.bindUnsafe();

        int numGroupsX = PMath.roundUp(dstWidth, SHADER_WORK_GROUP_TILE_SIZE) / SHADER_WORK_GROUP_TILE_SIZE;
        int numGroupsY = PMath.roundUp(dstHeight, SHADER_WORK_GROUP_TILE_SIZE) / SHADER_WORK_GROUP_TILE_SIZE;

        if (srcSampler) {
            uniformSetter.set1i(shader.uniformLocation("u_srcTextureLod"), srcLevel);
            srcTexture.bindToUnitUnsafe(SRC_SAMPLER_BINDING);
        } else {
            //TODO: on subsequent dispatches, bind the destination texture instead of the source
            this.gl.glBindImageTexture(SRC_IMAGE_BINDING, srcTexture.id(), srcLevel, false, 0, GL_READ_ONLY, srcTexture.internalFormat().id());
        }

        this.gl.glBindImageTexture(DST_IMAGE_BINDING, dstTexture.id(), dstLevel, false, 0, GL_WRITE_ONLY, dstTexture.internalFormat().id());

        this.gl.glMemoryBarrier(GL_TEXTURE_FETCH_BARRIER_BIT | GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        this.gl.glDispatchCompute(numGroupsX, numGroupsY, 1);
    }

    @Override
    public void configureModifiedState(@NonNull StatePreserver.Builder builder) {
        builder.activeProgram();
        builder.texture(TextureTarget.TEXTURE_2D, SRC_SAMPLER_BINDING);
        //TODO: add image bindings to StatePreserver
    }
}
