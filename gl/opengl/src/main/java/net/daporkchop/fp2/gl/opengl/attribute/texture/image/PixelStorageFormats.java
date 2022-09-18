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
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.opengl.OpenGL;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public class PixelStorageFormats {
    protected final OpenGL gl;

    protected final List<PixelStorageFormat> all;

    protected final Map<Set<PixelFormatChannel>, List<PixelStorageFormat>> byChannels;

    public PixelStorageFormats(@NonNull OpenGL gl) {
        this.gl = gl;

        ImmutableList.Builder<PixelStorageFormat> builder = ImmutableList.builder();

        ImmutableList<PixelFormatChannel> listR = ImmutableList.of(RED);
        ImmutableList<PixelFormatChannel> listRG = ImmutableList.of(RED, GREEN);
        ImmutableList<PixelFormatChannel> listRGB = ImmutableList.of(RED, GREEN, BLUE);
        ImmutableList<PixelFormatChannel> listBGR = ImmutableList.of(BLUE, GREEN, RED);
        ImmutableList<PixelFormatChannel> listRGBA = ImmutableList.of(RED, GREEN, BLUE, ALPHA);
        ImmutableList<PixelFormatChannel> listBGRA = ImmutableList.of(BLUE, GREEN, RED, ALPHA);

        builder.add(new SimpleStorageFormat(GL_RED, PixelFormatChannelType.FLOATING_POINT, listR));
        builder.add(new SimpleStorageFormat(GL_RED_INTEGER, PixelFormatChannelType.INTEGER, listR));
        builder.add(new SimpleStorageFormat(GL_RED_INTEGER, PixelFormatChannelType.UNSIGNED_INTEGER, listR));
        builder.add(new SimpleStorageFormat(GL_RG, PixelFormatChannelType.FLOATING_POINT, listRG));
        builder.add(new SimpleStorageFormat(GL_RG_INTEGER, PixelFormatChannelType.INTEGER, listRG));
        builder.add(new SimpleStorageFormat(GL_RG_INTEGER, PixelFormatChannelType.UNSIGNED_INTEGER, listRG));
        builder.add(new SimpleStorageFormat(GL_RGB, PixelFormatChannelType.FLOATING_POINT, listRGB));
        builder.add(new SimpleStorageFormat(GL_RGB_INTEGER, PixelFormatChannelType.INTEGER, listRGB));
        builder.add(new SimpleStorageFormat(GL_RGB_INTEGER, PixelFormatChannelType.UNSIGNED_INTEGER, listRGB));
        builder.add(new SimpleStorageFormat(GL_BGR, PixelFormatChannelType.FLOATING_POINT, listBGR));
        builder.add(new SimpleStorageFormat(GL_BGR_INTEGER, PixelFormatChannelType.INTEGER, listBGR));
        builder.add(new SimpleStorageFormat(GL_BGR_INTEGER, PixelFormatChannelType.UNSIGNED_INTEGER, listBGR));
        builder.add(new SimpleStorageFormat(GL_RGBA, PixelFormatChannelType.FLOATING_POINT, listRGBA));
        builder.add(new SimpleStorageFormat(GL_RGBA_INTEGER, PixelFormatChannelType.INTEGER, listRGBA));
        builder.add(new SimpleStorageFormat(GL_RGBA_INTEGER, PixelFormatChannelType.UNSIGNED_INTEGER, listRGBA));
        builder.add(new SimpleStorageFormat(GL_BGRA, PixelFormatChannelType.FLOATING_POINT, listBGRA));
        builder.add(new SimpleStorageFormat(GL_BGRA_INTEGER, PixelFormatChannelType.INTEGER, listBGRA));
        builder.add(new SimpleStorageFormat(GL_BGRA_INTEGER, PixelFormatChannelType.UNSIGNED_INTEGER, listBGRA));

        this.all = builder.build();

        //noinspection UnstableApiUsage
        this.byChannels = ImmutableMap.copyOf(this.all.stream().collect(Collectors.groupingBy(
                format -> Sets.immutableEnumSet(format.channels()),
                ImmutableList.toImmutableList())));
    }

    public Stream<PixelStorageFormat> all() {
        return this.all.stream();
    }

    public PixelStorageFormat getOptimalStorageFormatFor(@NonNull PixelFormatBuilderImpl builder) {
        return this.byChannels.getOrDefault(builder.channelsToMinimumBitDepths().keySet(), Collections.emptyList())
                .stream()
                .filter(format -> format.type() == builder.type())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("unable to determine storage format for " + builder));
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static class SimpleStorageFormat implements PixelStorageFormat {
        protected final int glFormat;
        @NonNull
        protected final PixelFormatChannelType type;
        @NonNull
        protected final ImmutableList<PixelFormatChannel> channels;
    }
}
