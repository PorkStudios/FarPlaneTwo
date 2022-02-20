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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.resources.FResources;
import net.daporkchop.fp2.resources.simple.DirectoryFResources;
import net.daporkchop.lib.common.function.io.IOConsumer;
import net.daporkchop.lib.common.function.io.IOSupplier;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public abstract class TranslateResourcesTask extends DefaultTask {
    @Input
    public abstract Property<String> getTargetFormat();

    @Incremental
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputDirectory
    public abstract DirectoryProperty getInputDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    @SneakyThrows(IOException.class)
    public void execute(InputChanges changes) {
        if (!changes.isIncremental()) {
            System.out.println("not an incremental build, nuking output directory...");
            this.getProject().delete(this.getOutputDirectory().getAsFileTree());
        }

        Path inputDir = this.getInputDirectory().getAsFile().get().toPath();
        Path outputDir = this.getOutputDirectory().getAsFile().get().toPath();

        Set<Path> removedPaths = new HashSet<>();
        Set<Path> changedPaths = new HashSet<>();
        changes.getFileChanges(this.getInputDirectory()).iterator().forEachRemaining(fileChange -> {
            if (fileChange.getFileType() == FileType.DIRECTORY) { //we don't care about folders
                return;
            }

            Path absoluteInput = fileChange.getFile().toPath();
            Path relativeInput = inputDir.relativize(absoluteInput);

            switch (fileChange.getChangeType()) {
                case MODIFIED:
                case ADDED:
                    changedPaths.add(relativeInput);
                    break;
                case REMOVED:
                    removedPaths.add(relativeInput);
                    break;
            }
        });

        //translate removed sources to target format and delete corresponding files in output
        try (Stream<Path> stream = FResources.translateResources(new SetEnumeratingResources(removedPaths), this.getTargetFormat().get()).listResources().getThrowing()) {
            stream.map(outputDir::resolve).forEach((IOConsumer<Path>) Files::deleteIfExists);
        }

        //translate changed sources to target format and modify corresponding files in output
        try (Stream<Path> stream = FResources.translateResources(new SetEnumeratingResources(changedPaths), this.getTargetFormat().get()).listResources().getThrowing()) {
            FResources changedResources = FResources.translateResources(new DirectoryFResources(inputDir), this.getTargetFormat().get());

            stream.forEach((IOConsumer<Path>) relativeOutput -> {
                Path absoluteOutput = outputDir.resolve(relativeOutput);

                Files.createDirectories(absoluteOutput.getParent());
                try (InputStream in = changedResources.getResource(relativeOutput).get().getThrowing()) {
                    Files.copy(in, absoluteOutput, StandardCopyOption.REPLACE_EXISTING);
                }
            });
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class SetEnumeratingResources implements FResources {
        @NonNull
        protected final Set<Path> paths;

        @Override
        public Optional<IOSupplier<InputStream>> getResource(@NonNull Path path) {
            return !this.paths.contains(path) ? Optional.empty() : Optional.of(() -> {
                throw new UnsupportedOperationException();
            });
        }

        @Override
        public IOSupplier<Stream<Path>> listResources() {
            return this.paths::stream;
        }
    }
}
