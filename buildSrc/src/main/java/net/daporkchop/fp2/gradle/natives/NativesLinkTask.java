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
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
public abstract class NativesLinkTask extends DefaultTask {
    @Input
    public abstract Property<NativeSpec> getSpec();

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputDirectory
    public abstract DirectoryProperty getInput();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @TaskAction
    @SneakyThrows
    public void execute() {
        NativeSpec spec = this.getSpec().get();

        this.getOutput().getAsFile().get().getParentFile().mkdirs();
        System.out.println("linking " + this.getInput().get() + " for " + spec.descriptionText());

        List<String> command = new ArrayList<>();
        command.add("/usr/bin/clang++");
        command.add("-target");
        command.add(spec.platformString());
        command.addAll(spec.linkerFlags());
        command.add("-o");
        command.add(this.getOutput().getAsFile().get().getAbsolutePath());
        command.addAll(this.getInput().getAsFileTree().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toList()));

        //System.out.println(String.join(" ", command));
        Process process = new ProcessBuilder().redirectErrorStream(true).command(command).start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream in = new BufferedInputStream(process.getInputStream())) {
            for (int b; (b = in.read()) >= 0; ) {
                baos.write(b);
            }
        }
        process.waitFor();

        if (process.exitValue() != 0) {
            throw new IllegalStateException(new String(baos.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
