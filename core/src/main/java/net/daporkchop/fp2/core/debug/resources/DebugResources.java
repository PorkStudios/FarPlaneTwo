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

package net.daporkchop.fp2.core.debug.resources;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.function.io.IOFunction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class DebugResources {
    static {
        checkState(FP2_DEBUG, "debug mode not enabled!");
    }

    private final Path RESOURCES_DIR = findResourcesDir();

    private Path findResourcesDir() {
        Path path = Paths.get(".").toAbsolutePath();

        //find git repo root directory
        while (path != null && !Files.isDirectory(path.resolve(".git")) && !Files.isRegularFile(path.resolve("LICENSE"))) {
            path = path.getParent();
        }

        return path != null ? path.resolve("core").resolve("resources") : null;
    }

    /**
     * Finds a debug resource with the given path.
     *
     * @param path the path to the debug resource, relative to the universal resource root
     * @return the absolute path to the debug resource file, if present
     */
    public Optional<Path> findPath(@NonNull Path path) {
        if (RESOURCES_DIR == null) {
            return Optional.empty();
        }

        return Optional.of(RESOURCES_DIR.resolve(path)).filter(Files::isRegularFile);
    }

    /**
     * Finds a debug resource with the given path.
     *
     * @param path the path to the debug resource, relative to the universal resource root
     * @return the absolute path to the debug resource file, if present
     */
    public Optional<InputStream> findStream(@NonNull Path path) throws IOException {
        if (RESOURCES_DIR == null) {
            return Optional.empty();
        }

        return Optional.of(RESOURCES_DIR.resolve(path)).filter(Files::isRegularFile).map((IOFunction<Path, InputStream>) Files::newInputStream);
    }

    /**
     * @return the relative paths of all the available debug resources
     */
    public List<Path> list(@NonNull Path path, int maxDepth) throws IOException {
        if (RESOURCES_DIR == null || !Files.exists(RESOURCES_DIR.resolve(path))) {
            return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.walk(RESOURCES_DIR.resolve(path), maxDepth)) {
            return stream.filter(Files::isRegularFile)
                    .map(p -> p.relativize(RESOURCES_DIR))
                    .collect(Collectors.toList());
        }
    }
}
