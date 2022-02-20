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

import com.google.gson.JsonObject;
import lombok.NonNull;
import net.daporkchop.fp2.resources.FResources;
import net.daporkchop.lib.common.function.io.IOFunction;
import net.daporkchop.lib.common.function.io.IOSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public class MinecraftPackTranslatingFResources3To4 extends AbstractMinecraftPackTranslatingFResources {
    public MinecraftPackTranslatingFResources3To4(@NonNull FResources next) {
        super(next, 4);

        //.lang -> .json
        this.addTranslator(new Translator() {
            @Override
            public Optional<IOSupplier<Stream<Path>>> expandSrcNames(@NonNull Path srcPath, @NonNull FResources next) {
                return srcPath.getFileName().toString().matches(".._..\\.lang")
                        ? Optional.of(() -> Stream.of(srcPath.resolveSibling(srcPath.getFileName().toString().replace("lang", "json"))))
                        : Optional.empty();
            }

            @Override
            public Optional<IOSupplier<Stream<Path>>> globalSrcNames(@NonNull FResources next) {
                return Optional.empty();
            }

            @Override
            public Optional<IOSupplier<InputStream>> getFromDstName(@NonNull Path dstPath, @NonNull FResources next) {
                if (!dstPath.getFileName().toString().matches(".._..\\.json")) {
                    return Optional.empty();
                }

                Path jsonPath = dstPath.resolveSibling(dstPath.getFileName().toString().replace("json", "lang"));
                return next.getResource(jsonPath).map((IOFunction<IOSupplier<InputStream>, IOSupplier<InputStream>>) langSupplier -> () -> {
                    Properties properties = new Properties();
                    try (InputStream in = langSupplier.getThrowing()) {
                        properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    }

                    JsonObject object = new JsonObject();
                    properties.forEach((key, value) -> object.addProperty((String) key, (String) value));

                    return new ByteArrayInputStream(GSON.toJson(object).getBytes(StandardCharsets.UTF_8));
                });
            }
        });
    }
}
