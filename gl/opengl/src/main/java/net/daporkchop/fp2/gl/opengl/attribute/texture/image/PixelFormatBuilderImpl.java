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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormat;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatBuilder;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelRange;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.lib.common.annotation.param.Positive;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@ToString
public class PixelFormatBuilderImpl implements PixelFormatBuilder.ChannelSelectionStage, PixelFormatBuilder.TypeSelectionStage, PixelFormatBuilder.RangeSelectionStage, PixelFormatBuilder {
    @NonNull
    protected final OpenGL gl;

    protected Map<PixelFormatChannel, Integer> channelsToMinimumBitDepths;

    protected PixelFormatChannelType type;
    protected PixelFormatChannelRange range;

    //
    // ChannelSelectionStage
    //

    @Override
    public TypeSelectionStage channels(@NonNull PixelFormatChannel @NonNull ... channels) {
        checkArg(channels.length != 0, "at least one channel must be given");

        //ensure pixel formats are distinct
        Set<PixelFormatChannel> uniqueChannels = EnumSet.noneOf(PixelFormatChannel.class);
        for (PixelFormatChannel channel : channels) {
            checkArg(uniqueChannels.add(channel), "duplicate color channel: %s", channel);
        }

        //ensure pixel formats have their required dependencies
        for (PixelFormatChannel channel : channels) {
            checkArg(uniqueChannels.containsAll(channel.depends()), "channel %s depends on channels %s, but they are not present! %s",
                    channel, channel.depends(), uniqueChannels);
        }

        //store all the pixel formats in the bit depth map
        this.channelsToMinimumBitDepths = new EnumMap<>(PixelFormatChannel.class);
        for (PixelFormatChannel channel : channels) {
            this.channelsToMinimumBitDepths.put(channel, null);
        }

        return this;
    }

    //
    // TypeSelectionStage
    //

    @Override
    public RangeSelectionStage type(@NonNull PixelFormatChannelType type) {
        this.type = type;
        return this;
    }

    //
    // RangeSelectionStage
    //

    @Override
    public PixelFormatBuilder range(@NonNull PixelFormatChannelRange range) {
        this.range = range;
        return this;
    }

    //
    // PixelFormatBuilder
    //

    @Override
    public PixelFormatBuilder minBitDepth(@Positive int minimumBitDepth) {
        checkIndex(1, 33, minimumBitDepth);

        //replace all minimum bit depths
        this.channelsToMinimumBitDepths.replaceAll((channel, oldMinimumBitDepth) -> minimumBitDepth);

        return this;
    }

    @Override
    public PixelFormatBuilder minBitDepth(@NonNull Map<@NonNull PixelFormatChannel, @Positive Integer> minimumBitDepths) {
        //validate arguments
        minimumBitDepths.forEach((channel, minimumBitDepth) -> {
            checkArg(this.channelsToMinimumBitDepths.containsKey(channel), "channel %s isn't defined by this pixel format!", channel);

            if (minimumBitDepth != null) {
                checkIndex(1, 33, minimumBitDepth);
            }
        });

        //store all minimum bit depths
        this.channelsToMinimumBitDepths.putAll(minimumBitDepths);

        return null;
    }

    @Override
    public PixelFormat build() {
        return this.gl.pixelFormatFactory().build(this);
    }
}
