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

import lombok.SneakyThrows;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.gradle.internal.Cast.*;

/**
 * @author DaPorkchop_
 */
public abstract class TranslateResourcesTask extends DefaultTask {
    @Inject
    public abstract WorkerExecutor getExecutor();

    @Incremental
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputDirectory
    public abstract DirectoryProperty getInputDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void execute(InputChanges changes) {
        Iterator<? extends FileChange> fileChanges = changes.getFileChanges(this.getInputDirectory()).iterator();

        if (!changes.isIncremental()) {
            System.out.println("not an incremental build, nuking output directory...");
            this.getProject().delete(this.getOutputDirectory().getAsFileTree());
        }

        List<Transformation<WorkParameters>> transformations = uncheckedNonnullCast(this.transformations());

        Path inputDir = this.getInputDirectory().getAsFile().get().toPath();
        Path outputDir = this.getOutputDirectory().getAsFile().get().toPath();

        WorkQueue workQueue = this.getExecutor().noIsolation();

        fileChanges.forEachRemaining(fileChange -> {
            if (fileChange.getFileType() == FileType.DIRECTORY) { //we don't care about folders
                return;
            }

            Path absoluteInput = fileChange.getFile().toPath();
            Path relativeInput = inputDir.relativize(absoluteInput);

            for (Transformation<WorkParameters> transformation : transformations) {
                Optional<Stream<Action<WorkParameters>>> optionalConfigurators = transformation.tryTransform(fileChange.getChangeType(), absoluteInput, relativeInput, outputDir);
                if (optionalConfigurators.isPresent()) {
                    optionalConfigurators.get().forEach(configurator -> workQueue.submit(transformation.actionClass(), configurator));
                    return;
                }
            }

            throw new IllegalStateException("no transformation will handle " + absoluteInput);
        });

        for (Transformation<WorkParameters> transformation : transformations) {
            transformation.additionalTransforms(outputDir).forEach(configurator -> workQueue.submit(transformation.actionClass(), configurator));
        }
    }

    protected abstract List<Transformation<?>> transformations();

    /**
     * @author DaPorkchop_
     */
    interface Transformation<P extends WorkParameters> {
        Optional<Stream<Action<P>>> tryTransform(ChangeType changeType, Path absoluteInput, Path relativeInput, Path outputDir);

        default Stream<Action<P>> additionalTransforms(Path outputDir) {
            return Stream.empty();
        }

        Class<? extends WorkAction<P>> actionClass();
    }

    /**
     * @author DaPorkchop_
     */
    public interface TransformParameters extends WorkParameters {
        Property<ChangeType> getChangeType();

        RegularFileProperty getInputFile();

        RegularFileProperty getOutputFile();
    }

    /**
     * @author DaPorkchop_
     */
    public static abstract class AbstractTransformAction<T extends TransformParameters> implements WorkAction<T> {
        @Override
        @SneakyThrows(IOException.class)
        public void execute() {
            T parameters = this.getParameters();

            switch (parameters.getChangeType().get()) {
                case ADDED:
                case MODIFIED:
                    System.out.println("transforming " + parameters.getInputFile().getAsFile().getOrNull() + " -> " + parameters.getOutputFile().getAsFile().get());
                    Files.createDirectories(parameters.getOutputFile().getAsFile().get().toPath().getParent());
                    this.transform(parameters);
                    return;
                case REMOVED:
                    System.out.println("deleting " + parameters.getOutputFile().getAsFile().get());
                    Files.deleteIfExists(parameters.getOutputFile().getAsFile().get().toPath());
                    return;
                default:
                    throw new IllegalArgumentException(parameters.getChangeType().get().name());
            }
        }

        protected abstract void transform(T parameters) throws IOException;
    }

    /**
     * @author DaPorkchop_
     */
    public static final class CopyTransformation implements Transformation<TransformParameters> {
        @Override
        public Optional<Stream<Action<TransformParameters>>> tryTransform(ChangeType changeType, Path absoluteInput, Path relativeInput, Path outputDir) {
            return Optional.of(Stream.of(parameters -> {
                parameters.getChangeType().set(changeType);
                parameters.getInputFile().set(absoluteInput.toFile());
                parameters.getOutputFile().set(outputDir.resolve(relativeInput).toFile());
            }));
        }

        @Override
        public Class<? extends WorkAction<TransformParameters>> actionClass() {
            return CopyAction.class;
        }

        /**
         * @author DaPorkchop_
         */
        public static abstract class CopyAction extends AbstractTransformAction<TransformParameters> {
            @Override
            protected void transform(TransformParameters parameters) throws IOException {
                Path input = parameters.getInputFile().getAsFile().get().toPath();
                Path output = parameters.getOutputFile().getAsFile().get().toPath();

                Files.createDirectories(output.getParent());
                Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
