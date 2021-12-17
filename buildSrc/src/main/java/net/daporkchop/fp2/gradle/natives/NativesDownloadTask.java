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
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author DaPorkchop_
 */
public abstract class NativesDownloadTask extends DefaultTask {
    @Inject
    public abstract WorkerExecutor getExecutor();

    @Input
    public abstract Property<NativeSpec> getSpec();

    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();

    @TaskAction
    public void execute() {
        this.getProject().delete(this.getDestinationDirectory().getAsFileTree());

        WorkQueue workQueue = this.getExecutor().noIsolation();
        NativeSpec spec = this.getSpec().get();

        spec.libraries().forEach(library -> {
            workQueue.submit(DownloadAction.class, parameters -> {
                parameters.getLibraryUrl().set(library);
                parameters.getDestination().set(this.getDestinationDirectory());
            });
        });
    }

    /**
     * @author DaPorkchop_
     */
    public interface DownloadParameters extends WorkParameters {
        Property<String> getLibraryUrl();

        DirectoryProperty getDestination();
    }

    /**
     * @author DaPorkchop_
     */
    public abstract static class DownloadAction implements WorkAction<DownloadParameters> {
        @Override
        @SneakyThrows
        public void execute() {
            DownloadParameters parameters = this.getParameters();
            System.out.println("downloading " + parameters.getLibraryUrl().get() + ".tar.gz");

            List<String> command = new ArrayList<>();
            command.add(Natives.TAR_PATH.get().toString());
            command.add("zxf");
            command.add("-");

            Process process = new ProcessBuilder().directory(parameters.getDestination().getAsFile().get()).redirectErrorStream(true).command(command).start();
            try (InputStream in = new URL(parameters.getLibraryUrl().get() + ".tar.gz").openStream();
                 OutputStream out = process.getOutputStream()) {
                byte[] buf = new byte[4096];
                for (int i; (i = in.read(buf)) >= 0; ) {
                    out.write(buf, 0, i);
                }
            }
            process.waitFor();

            if (process.exitValue() != 0) {
                throw new IllegalStateException(String.valueOf(process.exitValue()));
            }
        }
    }
}
