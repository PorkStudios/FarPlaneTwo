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
import lombok.SneakyThrows;
import net.daporkchop.fp2.gradle.FP2GradlePlugin;
import net.daporkchop.fp2.gradle.natives.struct.NativeModule;
import net.daporkchop.fp2.gradle.natives.struct.NativesExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class Natives {
    protected static final String ROOT_TASK_NAME = "natives";
    protected static final String BUILD_DIR_NAME = "fp2_natives";

    public static final Optional<Path> CLANG_PATH = FP2GradlePlugin.findInPath("clang++");
    public static final Optional<Path> TAR_PATH = FP2GradlePlugin.findInPath("tar");

    @SneakyThrows({ InterruptedException.class, IOException.class })
    public static void launchProcessAndWait(List<String> command) {
        Process process = new ProcessBuilder().redirectErrorStream(true).command(command).start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            byte[] buf = new byte[512];
            for (int i; (i = in.read(buf)) >= 0; ) {
                baos.write(buf, 0, i);
            }
        }
        process.waitFor();

        if (process.exitValue() != 0) {
            throw new IllegalStateException("process exited with non-zero exit code " + process.exitValue()
                                            + "\ncommand line: " + String.join(" ", command)
                                            + "\nstdout+stderr:\n" + new String(baos.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    @NonNull
    protected final Project project;

    public void register() {
        if (!CLANG_PATH.isPresent() || !TAR_PATH.isPresent()) { //do nothing
            return;
        }

        NativesExtension extension = this.project.getExtensions().create("natives", NativesExtension.class);

        this.project.afterEvaluate(_project -> {
            TaskProvider<Task> rootTask = this.project.getTasks().register(ROOT_TASK_NAME);

            Map<SourceSet, Provider<Directory>> linkOutputDirsBySourceSet = new HashMap<>();

            extension.getModules().forEach(module -> {
                Provider<Directory> linkOutputDir = linkOutputDirsBySourceSet.computeIfAbsent(module.getSourceSet().get(), sourceSet -> {
                    Provider<Directory> dir = this.project.getLayout().getBuildDirectory().dir(BUILD_DIR_NAME + "/link/" + sourceSet.getName());
                    sourceSet.getOutput().dir(Collections.singletonMap("builtBy", ROOT_TASK_NAME), dir);
                    return dir;
                });

                extension.getOperatingSystems().forEach(operatingSystem -> {
                    operatingSystem.getSupportedArchitectures().get().forEach(architecture -> {
                        if (module.getSimd().getOrElse(false)) {
                            architecture.getSimdExtensions().forEach(simdExtension -> {
                                TaskProvider<Task> moduleRootTask = this.registerBuild(new NativeSpec(extension, module, architecture, operatingSystem, simdExtension), module, linkOutputDir);
                                rootTask.configure(task -> task.dependsOn(moduleRootTask));
                            });
                        }
                        TaskProvider<Task> moduleRootTask = this.registerBuild(new NativeSpec(extension, module, architecture, operatingSystem, null), module, linkOutputDir);
                        rootTask.configure(task -> task.dependsOn(moduleRootTask));
                    });
                });
            });
        });
    }

    private TaskProvider<Task> registerBuild(NativeSpec spec, NativeModule module, Provider<Directory> linkOutputDir) {
        Provider<Directory> downloadOutputDir = this.project.getLayout().getBuildDirectory().dir(spec.librariesOutputDirectory());
        Provider<Directory> compileOutputDir = this.project.getLayout().getBuildDirectory().dir(spec.compileOutputDirectory());

        spec.includeDirectories().add(downloadOutputDir.get().getAsFile().getAbsolutePath());

        TaskProvider<NativesDownloadTask> downloadTask = this.project.getTasks().register(spec.librariesTaskName(), NativesDownloadTask.class, task -> {
            task.getSpec().set(spec);
            task.getDestinationDirectory().set(downloadOutputDir);
        });
        TaskProvider<NativesCompileTask> compileTask = this.project.getTasks().register(spec.compileTaskName(), NativesCompileTask.class, task -> {
            task.dependsOn(downloadTask);

            task.getSpec().set(spec);
            task.inputs().from(module.getSourceSet().get().getAllSource().getSrcDirs().stream()
                    .map(dir -> dir.toPath().getParent().resolve("native/" + spec.moduleRoot()).toFile())
                    .toArray());

            task.includes().from(task.inputs());
            task.includes().from(spec.includeDirectories().toArray());
            task.getOutputDirectory().set(compileOutputDir);
        });
        TaskProvider<NativesLinkTask> linkTask = this.project.getTasks().register(spec.linkTaskName(), NativesLinkTask.class, task -> {
            task.dependsOn(compileTask);

            task.getSpec().set(spec);
            task.inputs().from(compileOutputDir);
            task.getOutput().set(linkOutputDir.map(dir -> dir.file(spec.linkedLibraryFile())));
        });

        return this.project.getTasks().register(spec.rootTaskName(), task -> {
            task.dependsOn(downloadTask, compileTask, linkTask);
        });
    }
}
