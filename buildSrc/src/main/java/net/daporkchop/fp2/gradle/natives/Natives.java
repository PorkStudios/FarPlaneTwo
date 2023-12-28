/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class Natives {
    protected static final String ROOT_TASK_NAME = "natives";
    protected static final String BUILD_DIR_NAME = "fp2_natives";

    public static final Optional<Path> CLANG_PATH = FP2GradlePlugin.findInPath("clang++");
    public static final Optional<Path> TAR_PATH = FP2GradlePlugin.findInPath("tar");

    public static final boolean CAN_COMPILE = CLANG_PATH.isPresent() && TAR_PATH.isPresent();

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

                if (!CAN_COMPILE) {
                    System.err.printf("one or more required tools is not available, not compiling native module '%s' for project '%s'\n", module.getName(), this.project.getPath());
                    return;
                }

                List<TaskProvider<? extends Task>> compilationTasks = new ArrayList<>();
                List<CompletableFuture<Boolean>> specsSupportedTasks = new ArrayList<>();

                extension.getOperatingSystems().forEach(operatingSystem -> {
                    operatingSystem.getSupportedArchitectures().get().forEach(architecture -> {
                        if (module.getSimd().getOrElse(false)) {
                            architecture.getSimdExtensions().forEach(simdExtension -> {
                                NativeSpec spec = new NativeSpec(extension, module, architecture, operatingSystem, simdExtension);
                                compilationTasks.add(this.registerBuild(spec, module, linkOutputDir));
                                specsSupportedTasks.add(this.checkSupported(spec));
                            });
                        }

                        NativeSpec spec = new NativeSpec(extension, module, architecture, operatingSystem, null);
                        compilationTasks.add(this.registerBuild(spec, module, linkOutputDir));
                        specsSupportedTasks.add(this.checkSupported(spec));
                    });
                });

                rootTask.configure(task -> {
                    for (int i = 0; i < compilationTasks.size(); i++) {
                        if (specsSupportedTasks.get(i).join()) { //only add the spec's task as a build dependency if it's actually supported
                            task.dependsOn(compilationTasks.get(i));
                        }
                    }
                });
            });
        });
    }

    private TaskProvider<? extends Task> registerBuild(NativeSpec spec, NativeModule module, Provider<Directory> linkOutputDir) {
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

        return linkTask;
    }

    private CompletableFuture<Boolean> checkSupported(NativeSpec spec) {
        if (!Files.exists(Paths.get("/dev/null"))) {
            //TODO: we don't know how to handle this...
            return CompletableFuture.completedFuture(false);
        }

        List<String> command = new ArrayList<>();
        command.add(CLANG_PATH.get().toString());
        command.add("-target");
        command.add(spec.platformString());
        command.addAll(spec.cxxFlags());
        spec.includeDirectories().forEach(d -> command.add("-I" + d));
        spec.defines().forEach((k, v) -> command.add("-D" + k + '=' + v));
        command.add("-c");
        //TODO: this is gross
        command.add(this.project.getRootProject().getProjectDir().toPath().toAbsolutePath().resolve("buildSrc/src/main/resources/net/daporkchop/fp2/gradle/natives/compile-test.cpp").toString());
        command.add("-o");
        command.add("/dev/null");

        //TODO: should we also try linking?

        Process process;
        try {
            process = new ProcessBuilder(command).redirectOutput(new File("/dev/null")).redirectError(new File("/dev/null")).start();
        } catch (IOException e) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            while (true) {
                try {
                    return process.waitFor() == 0;
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        });
    }
}
