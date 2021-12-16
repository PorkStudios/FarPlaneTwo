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

import lombok.SneakyThrows;
import net.daporkchop.fp2.gradle.natives.struct.NativeArchitecture;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author DaPorkchop_
 */
public abstract class NativesCompileTask extends DefaultTask {
    @Inject
    public abstract WorkerExecutor getExecutor();

    @Input
    public abstract Property<NativeArchitecture> getArchitecture();

    @Incremental
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputDirectory
    public abstract DirectoryProperty getSourceDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();

    @TaskAction
    public void execute(InputChanges changes) {
        WorkQueue workQueue = this.getExecutor().noIsolation();
        changes.getFileChanges(this.getSourceDirectory()).forEach(fileChange -> {
            if (fileChange.getFileType() == FileType.DIRECTORY) { //we don't care about folders
                return;
            }

            Provider<RegularFile> objectFile = this.getDestinationDirectory().file(fileChange.getFile().getName() + ".o");
            workQueue.submit(CompileAction.class, parameters -> {
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
                    System.out.println("compiling " + parameters.getObjectFile().getAsFile().get());
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

        private void compile(CompileParameters parameters) {
        }
    }
}
