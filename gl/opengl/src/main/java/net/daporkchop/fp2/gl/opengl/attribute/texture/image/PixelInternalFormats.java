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
 */

package net.daporkchop.fp2.gl.opengl.attribute.texture.image;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelRange;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel.*;
import static net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelRange.*;
import static net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class PixelInternalFormats {
    protected final OpenGL gl;

    @Getter(AccessLevel.NONE)
    protected final Map<Set<PixelFormatChannel>,
            Map<PixelFormatChannelType,
                    Map<PixelFormatChannelRange,
                            List<PixelInternalFormat>>>> formatsByRangeByTypeByChannels;

    public PixelInternalFormats(@NonNull OpenGL gl) {
        this.gl = gl;

        ImmutableSet<PixelFormatChannel> setR = ImmutableSet.of(RED);
        ImmutableSet<PixelFormatChannel> setRG = ImmutableSet.of(RED, GREEN);
        ImmutableSet<PixelFormatChannel> setRGB = ImmutableSet.of(RED, GREEN, BLUE);
        ImmutableSet<PixelFormatChannel> setRGBA = ImmutableSet.of(RED, GREEN, BLUE, ALPHA);

        List<PixelInternalFormat> allFormats = new ArrayList<>();
        
        allFormats.add(new SimpleInternalFormat(GL_R8, FLOATING_POINT, ZERO_TO_ONE, setR, 8));
        allFormats.add(new SimpleInternalFormat(GL_R8_SNORM, FLOATING_POINT, NEGATIVE_ONE_TO_ONE, setR, 8));
        allFormats.add(new SimpleInternalFormat(GL_R16, FLOATING_POINT, ZERO_TO_ONE, setR, 16));
        allFormats.add(new SimpleInternalFormat(GL_R16_SNORM, FLOATING_POINT, NEGATIVE_ONE_TO_ONE, setR, 16));

        allFormats.add(new SimpleInternalFormat(GL_RG8, FLOATING_POINT, ZERO_TO_ONE, setRG, 8));
        allFormats.add(new SimpleInternalFormat(GL_RG8_SNORM, FLOATING_POINT, NEGATIVE_ONE_TO_ONE, setRG, 8));
        allFormats.add(new SimpleInternalFormat(GL_RG16, FLOATING_POINT, ZERO_TO_ONE, setRG, 16));
        allFormats.add(new SimpleInternalFormat(GL_RG16_SNORM, FLOATING_POINT, NEGATIVE_ONE_TO_ONE, setRG, 16));

        allFormats.add(new SimpleInternalFormat(GL_RGB4, FLOATING_POINT, ZERO_TO_ONE, setRGB, 4));
        allFormats.add(new SimpleInternalFormat(GL_RGB5, FLOATING_POINT, ZERO_TO_ONE, setRGB, 5));
        allFormats.add(new SimpleInternalFormat(GL_RGB8, FLOATING_POINT, ZERO_TO_ONE, setRGB, 8));
        allFormats.add(new SimpleInternalFormat(GL_RGB8_SNORM, FLOATING_POINT, NEGATIVE_ONE_TO_ONE, setRGB, 8));
        allFormats.add(new SimpleInternalFormat(GL_RGB10, FLOATING_POINT, ZERO_TO_ONE, setRGB, 10));
        allFormats.add(new SimpleInternalFormat(GL_RGB12, FLOATING_POINT, ZERO_TO_ONE, setRGB, 12));
        allFormats.add(new SimpleInternalFormat(GL_RGB16, FLOATING_POINT, ZERO_TO_ONE, setRGB, 16)); //TODO: this isn't present in the table at https://registry.khronos.org/OpenGL-Refpages/gl4/html/glTexImage2D.xhtml, figure out why
        allFormats.add(new SimpleInternalFormat(GL_RGB16_SNORM, FLOATING_POINT, NEGATIVE_ONE_TO_ONE, setRGB, 16));

        allFormats.add(new SimpleInternalFormat(GL_RGBA2, FLOATING_POINT, ZERO_TO_ONE, setRGBA, 2));
        allFormats.add(new SimpleInternalFormat(GL_RGBA4, FLOATING_POINT, ZERO_TO_ONE, setRGBA, 4));
        allFormats.add(new SimpleInternalFormat(GL_RGBA8, FLOATING_POINT, ZERO_TO_ONE, setRGBA, 8));
        allFormats.add(new SimpleInternalFormat(GL_RGBA8_SNORM, FLOATING_POINT, NEGATIVE_ONE_TO_ONE, setRGBA, 8));
        allFormats.add(new SimpleInternalFormat(GL_RGBA12, FLOATING_POINT, ZERO_TO_ONE, setRGBA, 12));
        allFormats.add(new SimpleInternalFormat(GL_RGBA16, FLOATING_POINT, ZERO_TO_ONE, setRGBA, 16));
        allFormats.add(new SimpleInternalFormat(GL_RGBA16_SNORM, FLOATING_POINT, NEGATIVE_ONE_TO_ONE, setRGBA, 16)); //TODO: this isn't present in the table at https://registry.khronos.org/OpenGL-Refpages/gl4/html/glTexImage2D.xhtml, figure out why

        allFormats.add(new SimpleInternalFormat(GL_R16F, FLOATING_POINT, INFINITY, setR, 16));
        allFormats.add(new SimpleInternalFormat(GL_R32F, FLOATING_POINT, INFINITY, setR, 32));
        allFormats.add(new SimpleInternalFormat(GL_RG16F, FLOATING_POINT, INFINITY, setRG, 16));
        allFormats.add(new SimpleInternalFormat(GL_RG32F, FLOATING_POINT, INFINITY, setRG, 32));
        allFormats.add(new SimpleInternalFormat(GL_RGB16F, FLOATING_POINT, INFINITY, setRGB, 16));
        allFormats.add(new SimpleInternalFormat(GL_RGB32F, FLOATING_POINT, INFINITY, setRGB, 32));
        allFormats.add(new SimpleInternalFormat(GL_RGBA16F, FLOATING_POINT, INFINITY, setRGBA, 16));
        allFormats.add(new SimpleInternalFormat(GL_RGBA32F, FLOATING_POINT, INFINITY, setRGBA, 32));

        allFormats.add(new SimpleInternalFormat(GL_R8I, INTEGER, INFINITY, setR, 8));
        allFormats.add(new SimpleInternalFormat(GL_R16I, INTEGER, INFINITY, setR, 16));
        allFormats.add(new SimpleInternalFormat(GL_R32I, INTEGER, INFINITY, setR, 32));
        allFormats.add(new SimpleInternalFormat(GL_RG8I, INTEGER, INFINITY, setRG, 8));
        allFormats.add(new SimpleInternalFormat(GL_RG16I, INTEGER, INFINITY, setRG, 16));
        allFormats.add(new SimpleInternalFormat(GL_RG32I, INTEGER, INFINITY, setRG, 32));
        allFormats.add(new SimpleInternalFormat(GL_RGB8I, INTEGER, INFINITY, setRGB, 8));
        allFormats.add(new SimpleInternalFormat(GL_RGB16I, INTEGER, INFINITY, setRGB, 16));
        allFormats.add(new SimpleInternalFormat(GL_RGB32I, INTEGER, INFINITY, setRGB, 32));
        allFormats.add(new SimpleInternalFormat(GL_RGBA8I, INTEGER, INFINITY, setRGBA, 8));
        allFormats.add(new SimpleInternalFormat(GL_RGBA16I, INTEGER, INFINITY, setRGBA, 16));
        allFormats.add(new SimpleInternalFormat(GL_RGBA32I, INTEGER, INFINITY, setRGBA, 32));

        allFormats.add(new SimpleInternalFormat(GL_R8UI, UNSIGNED_INTEGER, INFINITY, setR, 8));
        allFormats.add(new SimpleInternalFormat(GL_R16UI, UNSIGNED_INTEGER, INFINITY, setR, 16));
        allFormats.add(new SimpleInternalFormat(GL_R32UI, UNSIGNED_INTEGER, INFINITY, setR, 32));
        allFormats.add(new SimpleInternalFormat(GL_RG8UI, UNSIGNED_INTEGER, INFINITY, setRG, 8));
        allFormats.add(new SimpleInternalFormat(GL_RG16UI, UNSIGNED_INTEGER, INFINITY, setRG, 16));
        allFormats.add(new SimpleInternalFormat(GL_RG32UI, UNSIGNED_INTEGER, INFINITY, setRG, 32));
        allFormats.add(new SimpleInternalFormat(GL_RGB8UI, UNSIGNED_INTEGER, INFINITY, setRGB, 8));
        allFormats.add(new SimpleInternalFormat(GL_RGB16UI, UNSIGNED_INTEGER, INFINITY, setRGB, 16));
        allFormats.add(new SimpleInternalFormat(GL_RGB32UI, UNSIGNED_INTEGER, INFINITY, setRGB, 32));
        allFormats.add(new SimpleInternalFormat(GL_RGBA8UI, UNSIGNED_INTEGER, INFINITY, setRGBA, 8));
        allFormats.add(new SimpleInternalFormat(GL_RGBA16UI, UNSIGNED_INTEGER, INFINITY, setRGBA, 16));
        allFormats.add(new SimpleInternalFormat(GL_RGBA32UI, UNSIGNED_INTEGER, INFINITY, setRGBA, 32));

        //we've enumerated all the available internal formats, sort them and then build the index
        allFormats.sort(Comparator.naturalOrder());
        this.formatsByRangeByTypeByChannels = ImmutableMap.copyOf(allFormats.stream().collect(Collectors.groupingBy(
                PixelInternalFormat::channels,
                Collectors.collectingAndThen(Collectors.groupingBy(
                        PixelInternalFormat::channelType,
                        () -> new EnumMap<>(PixelFormatChannelType.class),
                        Collectors.collectingAndThen(Collectors.groupingBy(
                                PixelInternalFormat::channelRange,
                                () -> new EnumMap<>(PixelFormatChannelRange.class),
                                Collectors.toList()
                        ), ImmutableMap::copyOf)
                ), ImmutableMap::copyOf))));
    }

    public PixelInternalFormat getOptimalInternalFormatFor(@NonNull PixelFormatBuilderImpl builder) {
        return this.formatsByRangeByTypeByChannels
                .getOrDefault(builder.channelsToMinimumBitDepths().keySet(), Collections.emptyMap())
                .getOrDefault(builder.type(), Collections.emptyMap())
                .getOrDefault(builder.range(), Collections.emptyList())
                .stream().filter(format -> { //iterate through in ascending order by texel size
                    //make sure all the channels have a sufficient bit depth

                    Map<PixelFormatChannel, Integer> formatBitDepths = format.bitDepthPerChannel();
                    Map<PixelFormatChannel, Integer> builderBitDepths = builder.channelsToMinimumBitDepths();

                    for (PixelFormatChannel channel : formatBitDepths.keySet()) {
                        Integer formatBitDepth = formatBitDepths.get(channel);
                        Integer builderBitDepth = builderBitDepths.get(channel);

                        if (builderBitDepth == null) { //it doesn't matter what the format's bit depth is, because the user said they don't care
                            continue;
                        }

                        if (formatBitDepth == null) { //we don't know the bit depth - it's chosen by the implementation
                            //the user has set a precision requirement which we don't know if this pixel format meets
                            return false;
                        } else { //we know what the implementation bit depth for this format is going to be
                            if (builderBitDepth > formatBitDepth) { //the user has requested a bit depth greater than what the current format provides
                                return false;
                            } else { //the format's bit depth is sufficiently large to meet the user's requirements
                                //noinspection UnnecessaryContinue
                                continue;
                            }
                        }
                    }

                    return true;
                })
                .findFirst().orElseThrow(() -> new IllegalArgumentException("unable to determine internal format for " + builder));
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static abstract class BaseInternalFormat implements PixelInternalFormat {
        private final int glInternalFormat;
        @NonNull
        private final PixelFormatChannelType channelType;
        @NonNull
        private final PixelFormatChannelRange channelRange;

        private String toString;

        @Getter(AccessLevel.NONE)
        protected int sizeBytes = -1;

        @Override
        public int sizeBytes() {
            return this.sizeBytes >= 0 ? this.sizeBytes : this.sizeBytes0();
        }

        @Override
        public String toString() {
            if (this.toString == null) { //compute
                this.toString = OpenGL.DEBUG
                        ? OpenGLConstants.getNameIfPossible(this.glInternalFormat).orElseGet(() -> String.valueOf(this.glInternalFormat).intern())
                        : String.valueOf(this.glInternalFormat).intern();
            }
            return this.toString;
        }

        protected int sizeBytes0() {
            int sizeBits = this.bitDepthPerChannel().values().stream()
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            return this.sizeBytes = (sizeBits >> 3) + ((sizeBits & 7) == 0 ? 0 : 1);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Getter
    private static class SimpleInternalFormat extends BaseInternalFormat {
        private final ImmutableSet<PixelFormatChannel> channels;
        private final Integer bitDepth;

        public SimpleInternalFormat(int glInternalFormat, @NonNull PixelFormatChannelType channelType, @NonNull PixelFormatChannelRange channelRange, @NonNull ImmutableSet<PixelFormatChannel> channels, Integer bitDepth) {
            super(glInternalFormat, channelType, channelRange);

            this.channels = channels;
            this.bitDepth = bitDepth;
        }

        @Override
        protected int sizeBytes0() {
            int sizeBits = (this.bitDepth == null ? 0 : this.bitDepth) * this.channels.size();

            return this.sizeBytes = (sizeBits >> 3) + ((sizeBits & 7) == 0 ? 0 : 1);
        }

        @Override
        public ImmutableMap<PixelFormatChannel, Integer> bitDepthPerChannel() {
            //noinspection UnstableApiUsage
            return this.channels.stream().collect(Maps.toImmutableEnumMap(Function.identity(), channel -> this.bitDepth));
        }
    }
}
