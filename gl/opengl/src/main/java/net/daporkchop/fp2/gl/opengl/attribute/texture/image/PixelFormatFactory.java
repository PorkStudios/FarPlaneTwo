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

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelRange;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.opengl.OpenGL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
public class PixelFormatFactory {
    protected final OpenGL gl;

    protected final Map<Set<PixelFormatChannel>,
            Map<PixelFormatChannelType,
                    Map<PixelFormatChannelRange,
                            List<PixelInternalFormat>>>> formatsByRangeByTypeByChannels;

    public PixelFormatFactory(@NonNull OpenGL gl) {
        this.gl = gl;

        List<PixelInternalFormat> allFormats = new ArrayList<>();

        //we've enumerated all the available pixel formats, sort them and then build the index
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

    public PixelInternalFormat getOptimalPixelFormatFor(@NonNull PixelFormatBuilderImpl builder) {
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
                .findFirst().orElseThrow(() -> new IllegalArgumentException("unable to determine pixel format for " + builder));
    }
}
