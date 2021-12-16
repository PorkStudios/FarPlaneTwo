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
import net.daporkchop.fp2.gradle.natives.struct.NativeArchitecture;
import net.daporkchop.fp2.gradle.natives.struct.NativeModule;
import net.daporkchop.fp2.gradle.natives.struct.NativesExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.util.Collections;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class Natives {
    private static final String ROOT_TASK_NAME = "natives";

    private static String baseOutputDirectory(NativeModule module, NativeArchitecture architecture) {
        return "natives/" + architecture.getName() + '/' + module.getName();
    }

    private static String compileOutputDirectory(NativeModule module, NativeArchitecture architecture) {
        return baseOutputDirectory(module, architecture) + "/compile/" + module.getRoot().get();
    }

    private static String linkOutputDirectory(NativeModule module, NativeArchitecture architecture) {
        return baseOutputDirectory(module, architecture) + "/link/" + module.getRoot().get();
    }

    private static String linkedLibraryPath(NativeModule module, NativeArchitecture architecture) {
        return module.getRoot().get() + '/' + architecture.getName() + '.' + architecture.getSharedLibraryExtension().get();
    }

    private static String rootTaskName(NativeModule module) {
        return "natives_" + module.getName();
    }

    private static String rootTaskName(NativeArchitecture architecture, NativeModule module) {
        return "natives_" + architecture.getName() + '_' + module.getName();
    }

    private static String compileTaskName(NativeModule module, NativeArchitecture architecture) {
        return "natives_compile_" + architecture.getName() + '_' + module.getName();
    }

    private static String linkTaskName(NativeModule module, NativeArchitecture architecture) {
        return "natives_link_" + architecture.getName() + '_' + module.getName();
    }

    @NonNull
    protected final Project project;

    public void register() {
        NativesExtension extension = this.project.getExtensions().create("natives", NativesExtension.class);

        this.project.afterEvaluate(_project -> {
            TaskProvider<Task> rootTask = this.project.getTasks().register(ROOT_TASK_NAME);

            extension.getModules().all(module -> {
                TaskProvider<Task> moduleRootTask = this.registerModule(extension, module);
                rootTask.configure(task -> task.dependsOn(moduleRootTask));
            });
        });
    }

    private TaskProvider<Task> registerModule(NativesExtension extension, NativeModule module) {
        Directory sourceDir = this.project.getLayout().getProjectDirectory().dir("src/" + module.getSourceSet().get().getName() + "/native/" + module.getRoot().get());

        TaskProvider<Task> moduleRootTask = this.project.getTasks().register(rootTaskName(module));

        extension.getArchitectures().forEach(architecture -> {
            Provider<Directory> compileOutputDir = this.project.getLayout().getBuildDirectory().dir(compileOutputDirectory(module, architecture));
            Provider<Directory> linkOutputDir = this.project.getLayout().getBuildDirectory().dir(linkOutputDirectory(module, architecture));

            TaskProvider<NativesCompileTask> compileTask = this.project.getTasks().register(compileTaskName(module, architecture), NativesCompileTask.class, task -> {
                task.getArchitecture().set(architecture);
                task.getSourceDirectory().set(sourceDir);
                task.getDestinationDirectory().set(compileOutputDir);
            });
            TaskProvider<NativesLinkTask> linkTask = this.project.getTasks().register(linkTaskName(module, architecture), NativesLinkTask.class, task -> {
                task.dependsOn(compileTask);

                task.getInput().set(compileOutputDir);
                task.getOutput().set(linkOutputDir.map(dir -> dir.file(linkedLibraryPath(module, architecture))));
            });

            TaskProvider<Task> moduleArchitectureRootTask = this.project.getTasks().register(rootTaskName(architecture, module), task -> {
                task.dependsOn(compileTask, linkTask);
            });
            moduleRootTask.configure(task -> task.dependsOn(moduleArchitectureRootTask));

            module.getSourceSet().get().getOutput().dir(Collections.singletonMap("builtBy", linkTask.getName()), linkOutputDir);
        });

        return moduleRootTask;
    }
}
