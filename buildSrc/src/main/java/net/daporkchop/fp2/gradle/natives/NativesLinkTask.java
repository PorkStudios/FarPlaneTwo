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
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class NativesLinkTask extends DefaultTask {
    private final ConfigurableFileCollection inputs = this.getProject().getObjects().fileCollection();

    @Accessors(fluent = false)
    @Getter(value = AccessLevel.PROTECTED, onMethod_ = {
            @PathSensitive(PathSensitivity.RELATIVE),
            @InputFiles
    })
    private final FileTree stableSources = this.getProject()
            .files((Callable<FileTree>) () -> this.inputs.getAsFileTree().matching(patternFilterable -> patternFilterable.include("**/*.o")))
            .getAsFileTree();

    @Input
    public abstract Property<NativeSpec> getSpec();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @TaskAction
    @SneakyThrows
    public void execute() {
        NativeSpec spec = this.getSpec().get();

        this.getOutput().getAsFile().get().getParentFile().mkdirs();
        System.out.println("linking " + this.getOutput().getAsFile().get() + " for " + spec.descriptionText());

        List<String> command = new ArrayList<>();
        command.add(Natives.CLANG_PATH.get().toString());
        command.add("-target");
        command.add(spec.platformString());
        command.addAll(spec.linkerFlags());
        command.add("-o");
        command.add(this.getOutput().getAsFile().get().getAbsolutePath());
        this.stableSources.forEach(file -> command.add(file.getAbsolutePath()));

        Natives.launchProcessAndWait(command);
    }
}
