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

package net.daporkchop.fp2.gradle.translateresources;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.gradle.api.Action;
import org.gradle.work.ChangeType;
import org.gradle.workers.WorkAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public abstract class TranslateResourcesTask_MinecraftPackFormat6 extends TranslateResourcesTask {
    @Override
    protected List<Transformation<?>> transformations() {
        return Arrays.asList(
                new LangToJsonTransformation(),
                new CopyTransformation());
    }

    /**
     * @author DaPorkchop_
     */
    public static final class LangToJsonTransformation implements Transformation<TransformParameters> {
        @Override
        public Optional<Stream<Action<TransformParameters>>> tryTransform(ChangeType changeType, Path absoluteInput, Path relativeInput, Path outputDir) {
            return relativeInput.startsWith("assets/fp2/lang")
                    ? Optional.of(Stream.of(parameters -> { //intellij decided to indent this weird, idk
                parameters.getChangeType().set(changeType);
                parameters.getInputFile().set(absoluteInput.toFile());
                parameters.getOutputFile().set(outputDir.resolve(relativeInput.resolveSibling(relativeInput.getFileName().toString().replace(".lang", ".json"))).toFile());
            }))
                    : Optional.empty();
        }

        @Override
        public Class<? extends WorkAction<TransformParameters>> actionClass() {
            return LangToJsonAction.class;
        }

        /**
         * @author DaPorkchop_
         */
        public static abstract class LangToJsonAction extends AbstractTransformAction<TransformParameters> {
            @Override
            protected void transform(TransformParameters parameters) throws IOException {
                Path input = parameters.getInputFile().getAsFile().get().toPath();
                Path output = parameters.getOutputFile().getAsFile().get().toPath();

                Properties properties = new Properties();
                try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }

                JsonObject object = new JsonObject();
                properties.forEach((key, value) -> object.addProperty((String) key, (String) value));

                Files.write(output, new GsonBuilder().setPrettyPrinting().create().toJson(object).getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
