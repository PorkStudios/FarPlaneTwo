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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gradle.natives.struct.NativeModule;
import net.daporkchop.fp2.gradle.natives.struct.NativesExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class Natives {
    private static final String ROOT_TASK_NAME = "natives";

    @NonNull
    protected final Project project;

    public void register() {
        if (!Files.exists(Paths.get("/usr/bin/clang++"))) { //do nothing
            return;
        }

        NativesExtension extension = this.project.getExtensions().create("natives", NativesExtension.class);

        this.project.afterEvaluate(_project -> {
            TaskProvider<Task> rootTask = this.project.getTasks().register(ROOT_TASK_NAME);

            extension.getModules().all(module -> {
                extension.getOperatingSystems().forEach(operatingSystem -> {
                    operatingSystem.getSupportedArchitectures().get().forEach(architecture -> {
                        if (module.getSimd().getOrElse(false)) {
                            architecture.getSimdExtensions().all(simdExtension -> {
                                TaskProvider<Task> moduleRootTask = this.registerBuild(new NativeSpec(extension, module, architecture, operatingSystem, simdExtension), module);
                                rootTask.configure(task -> task.dependsOn(moduleRootTask));
                            });
                        }
                        TaskProvider<Task> moduleRootTask = this.registerBuild(new NativeSpec(extension, module, architecture, operatingSystem, null), module);
                        rootTask.configure(task -> task.dependsOn(moduleRootTask));
                    });
                });
            });
        });
    }

    private TaskProvider<Task> registerBuild(NativeSpec spec, NativeModule module) {
        Directory sourceDir = this.project.getLayout().getProjectDirectory().dir("src/" + module.getSourceSet().get().getName() + "/native/" + module.getRoot().get());

        Provider<Directory> compileOutputDir = this.project.getLayout().getBuildDirectory().dir(spec.compileOutputDirectory());
        Provider<Directory> linkOutputDir = this.project.getLayout().getBuildDirectory().dir(spec.linkOutputDirectory());

        TaskProvider<NativesCompileTask> compileTask = this.project.getTasks().register(spec.compileTaskName(), NativesCompileTask.class, task -> {
            task.getSpec().set(spec);
            task.getSourceDirectory().set(sourceDir);
            task.getDestinationDirectory().set(compileOutputDir);
        });
        TaskProvider<NativesLinkTask> linkTask = this.project.getTasks().register(spec.linkTaskName(), NativesLinkTask.class, task -> {
            task.dependsOn(compileTask);

            task.getSpec().set(spec);
            task.getInput().set(compileOutputDir);
            task.getOutput().set(linkOutputDir.map(dir -> dir.file(spec.linkedLibraryFile())));
        });

        TaskProvider<Task> rootTask = this.project.getTasks().register(spec.rootTaskName(), task -> {
            task.dependsOn(compileTask, linkTask);
        });

        module.getSourceSet().get().getOutput().dir(Collections.singletonMap("builtBy", rootTask.getName()), linkOutputDir);

        return rootTask;
    }
}
