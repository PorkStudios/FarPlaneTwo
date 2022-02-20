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
 *
 */

package net.daporkchop.fp2.resources.translate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import net.daporkchop.fp2.resources.FResources;
import net.daporkchop.lib.common.function.io.IOSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractMinecraftPackTranslatingFResources extends SimpleTranslatingFResources {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

    private static final Path PACK_MCMETA_PATH = Paths.get("pack.mcmeta");
    private static final String PACK_MCMETA_DEFAULT = "{\"pack\":{\"description\":\"unknown resources\"}}";

    public AbstractMinecraftPackTranslatingFResources(@NonNull FResources next, int format) {
        super(next, new Translator() {
            @Override
            public Optional<IOSupplier<Stream<Path>>> expandSrcNames(@NonNull Path srcPath, @NonNull FResources next) {
                return Optional.empty();
            }

            @Override
            public Optional<IOSupplier<Stream<Path>>> globalSrcNames(@NonNull FResources next) {
                return Optional.of(() -> next.getResource(PACK_MCMETA_PATH).isPresent()
                        ? Stream.empty()
                        : Stream.of(PACK_MCMETA_PATH));
            }

            @Override
            public Optional<IOSupplier<InputStream>> getFromDstName(@NonNull Path dstPath, @NonNull FResources next) {
                return PACK_MCMETA_PATH.equals(dstPath)
                        ? Optional.of(
                        () -> {
                            try (InputStream in = next.getResource(PACK_MCMETA_PATH)
                                    .orElseGet(() -> () -> new ByteArrayInputStream(PACK_MCMETA_DEFAULT.getBytes(StandardCharsets.UTF_8)))
                                    .get()) {
                                JsonObject obj = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                                obj.getAsJsonObject("pack").addProperty("pack_format", format);
                                return new ByteArrayInputStream(GSON.toJson(obj).getBytes(StandardCharsets.UTF_8));
                            }
                        })
                        : Optional.empty();
            }
        });
    }
}
