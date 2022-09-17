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

package net.daporkchop.fp2.gl.attribute.texture.image;

import lombok.NonNull;
import net.daporkchop.lib.common.annotation.param.Positive;

import java.util.Map;

/**
 * Builder for {@link PixelFormat pixel formats}.
 *
 * @author DaPorkchop_
 */
public interface PixelFormatBuilder {
    /**
     * Configures the minimum bit depth for all channels.
     */
    PixelFormatBuilder minBitDepth(@Positive int minimumBitDepth);

    /**
     * Configures the minimum bit depth for the given channels.
     */
    PixelFormatBuilder minBitDepth(@NonNull Map<@NonNull PixelFormatChannel, @Positive Integer> minimumBitDepths);

    /**
     * @return the constructed {@link PixelFormat}
     */
    PixelFormat build();

    /**
     * @author DaPorkchop_
     */
    interface ChannelSelectionStage {
        /**
         * Configures the {@link PixelFormat} to use the given {@link PixelFormatChannel channels}.
         */
        TypeSelectionStage channels(@NonNull PixelFormatChannel @NonNull ... channels);

        /**
         * Configures the {@link PixelFormat} to use the {@link PixelFormatChannel#RED} channel.
         *
         * @see #channels(PixelFormatChannel...)
         */
        default TypeSelectionStage r() {
            return this.channels(PixelFormatChannel.RED);
        }

        /**
         * Configures the {@link PixelFormat} to use the {@link PixelFormatChannel#RED} and {@link PixelFormatChannel#GREEN} channels.
         *
         * @see #channels(PixelFormatChannel...)
         */
        default TypeSelectionStage rg() {
            return this.channels(PixelFormatChannel.RED, PixelFormatChannel.GREEN);
        }

        /**
         * Configures the {@link PixelFormat} to use the {@link PixelFormatChannel#RED}, {@link PixelFormatChannel#GREEN} and {@link PixelFormatChannel#BLUE} channels.
         *
         * @see #channels(PixelFormatChannel...)
         */
        default TypeSelectionStage rgb() {
            return this.channels(PixelFormatChannel.RED, PixelFormatChannel.GREEN, PixelFormatChannel.BLUE);
        }

        /**
         * Configures the {@link PixelFormat} to use the {@link PixelFormatChannel#RED}, {@link PixelFormatChannel#GREEN}, {@link PixelFormatChannel#BLUE} and
         * {@link PixelFormatChannel#ALPHA} channels.
         *
         * @see #channels(PixelFormatChannel...)
         */
        default TypeSelectionStage rgba() {
            return this.channels(PixelFormatChannel.RED, PixelFormatChannel.GREEN, PixelFormatChannel.BLUE, PixelFormatChannel.ALPHA);
        }
    }

    /**
     * @author DaPorkchop_
     */
    interface TypeSelectionStage {
        /**
         * Configures the {@link PixelFormat} to use the given {@link PixelFormatChannelType channel type}.
         */
        RangeSelectionStage type(@NonNull PixelFormatChannelType type);
    }

    /**
     * @author DaPorkchop_
     */
    interface RangeSelectionStage {
        /**
         * Configures the {@link PixelFormat} to use the given {@link PixelFormatChannelRange channel range}.
         */
        PixelFormatBuilder range(@NonNull PixelFormatChannelRange range);
    }
}
