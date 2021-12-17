/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.gradle.natives;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileType;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.execution.history.changes.DefaultFileChange;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class NativesCompileTask extends DefaultTask {
    private final ConfigurableFileCollection inputs = this.getProject().getObjects().fileCollection();
    private final ConfigurableFileCollection includes = this.getProject().getObjects().fileCollection();

    @Accessors(fluent = false)
    @Getter(value = AccessLevel.PROTECTED, onMethod_ = {
            @Incremental,
            @PathSensitive(PathSensitivity.RELATIVE),
            @InputFiles
    })
    private final FileTree stableSources = this.getProject()
            .files((Callable<FileTree>) () -> this.inputs.getAsFileTree().matching(patternFilterable -> patternFilterable.include("**/*.cpp")))
            .getAsFileTree();

    @Accessors(fluent = false)
    @Getter(value = AccessLevel.PROTECTED, onMethod_ = {
            @Incremental,
            @PathSensitive(PathSensitivity.RELATIVE),
            @InputFiles
    })
    private final FileTree stableHeaders = this.getProject()
            .files((Callable<FileTree>) () -> this.includes.getAsFileTree().matching(patternFilterable -> patternFilterable.include("**/*.h", "**/*.hpp")))
            .getAsFileTree();

    @Inject
    public abstract WorkerExecutor getExecutor();

    @Input
    public abstract Property<NativeSpec> getSpec();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void execute(InputChanges changes) {
        WorkQueue workQueue = this.getExecutor().noIsolation();
        Iterator<? extends FileChange> fileChanges = changes.getFileChanges(this.stableSources).iterator();

        if (!changes.isIncremental()) {
            System.out.println("not an incremental build, nuking output directory...");
            this.getProject().delete(this.getOutputDirectory().getAsFileTree());
        } else if (changes.getFileChanges(this.stableHeaders).iterator().hasNext()) {
            System.out.println("headers have changed, nuking output directory...");
            this.getProject().delete(this.getOutputDirectory().getAsFileTree());

            //forcibly iterate over EVERYTHING, since changes which take effect after the task begins aren't taken into account
            fileChanges = this.stableSources.getFiles().stream().map(file -> DefaultFileChange.added(file.getAbsolutePath(), "", org.gradle.internal.file.FileType.RegularFile, file.getAbsolutePath())).iterator();
        }

        fileChanges.forEachRemaining(fileChange -> {
            if (fileChange.getFileType() == FileType.DIRECTORY) { //we don't care about folders
                return;
            }

            Path notAbsoluteSourcePath = fileChange.getFile().toPath().subpath(0, fileChange.getFile().toPath().getNameCount());
            Provider<RegularFile> objectFile = this.getOutputDirectory().file(notAbsoluteSourcePath + ".o");
            workQueue.submit(CompileAction.class, parameters -> {
                parameters.getSpec().set(this.getSpec().get());
                parameters.getChangeType().set(fileChange.getChangeType());
                parameters.getSourceFile().set(fileChange.getFile());
                parameters.getObjectFile().set(objectFile);
            });
        });
    }

    /**
     * @author DaPorkchop_
     */
    public interface CompileParameters extends WorkParameters {
        Property<NativeSpec> getSpec();

        Property<ChangeType> getChangeType();

        RegularFileProperty getSourceFile();

        RegularFileProperty getObjectFile();
    }

    /**
     * @author DaPorkchop_
     */
    public abstract static class CompileAction implements WorkAction<CompileParameters> {
        @Override
        @SneakyThrows(IOException.class)
        public void execute() {
            CompileParameters parameters = this.getParameters();

            switch (parameters.getChangeType().get()) {
                case ADDED:
                case MODIFIED:
                    System.out.println("compiling " + parameters.getObjectFile().getAsFile().get() + " for " + parameters.getSpec().get().descriptionText());
                    this.compile(parameters);
                    return;
                case REMOVED:
                    System.out.println("deleting " + parameters.getObjectFile().getAsFile().get());
                    Files.deleteIfExists(parameters.getObjectFile().getAsFile().get().toPath());
                    return;
                default:
                    throw new IllegalArgumentException(parameters.getChangeType().get().name());
            }
        }

        @SneakyThrows
        private void compile(CompileParameters parameters) {
            File sourceFile = parameters.getSourceFile().getAsFile().get();
            File objectFile = parameters.getObjectFile().getAsFile().get();

            objectFile.getParentFile().mkdirs();

            NativeSpec spec = parameters.getSpec().get();

            List<String> command = new ArrayList<>();
            command.add(Natives.CLANG_PATH.get().toString());
            command.add("-target");
            command.add(spec.platformString());
            command.addAll(spec.cxxFlags());
            spec.includeDirectories().forEach(d -> command.add("-I" + d));
            spec.defines().forEach((k, v) -> command.add("-D" + k + '=' + v));
            command.add("-c");
            command.add(sourceFile.getAbsolutePath());
            command.add("-o");
            command.add(objectFile.getAbsolutePath());

            Natives.launchProcessAndWait(command);
        }
    }
}
