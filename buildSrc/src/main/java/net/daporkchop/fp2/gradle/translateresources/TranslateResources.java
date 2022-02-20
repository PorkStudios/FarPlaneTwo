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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gradle.translateresources.struct.TranslateResourcesOptions;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class TranslateResources {
    protected static final String ROOT_TASK_NAME = "translateResources";
    protected static final String BUILD_DIR_NAME = "fp2_translateResources";

    @NonNull
    protected final Project project;

    protected TaskProvider<Task> rootTask;

    private final Map<SourceSet, Provider<Directory>> resourceOutputDirsBySourceSet = new HashMap<>();

    public TranslateResources register() {
        this.rootTask = this.project.getTasks().register(ROOT_TASK_NAME);

        return this;
    }

    private Provider<Directory> resourceOutputDir(SourceSet _sourceSet) {
        return this.resourceOutputDirsBySourceSet.computeIfAbsent(_sourceSet, sourceSet -> {
            Provider<Directory> dir = this.project.getLayout().getBuildDirectory().dir(BUILD_DIR_NAME + '/' + sourceSet.getName());
            sourceSet.getOutput().dir(Collections.singletonMap("builtBy", ROOT_TASK_NAME), dir);
            return dir;
        });
    }

    public void translateResources(TranslateResourcesOptions options) {
        this.project.getTasks().register(ROOT_TASK_NAME + '_' + options.getSourceSet().getName() + '_' + options.getTargetVersion(), TranslateResourcesTask.class, task -> {
            task.getTargetFormat().set(options.getTargetVersion());
            task.getInputDirectory().set(this.project.getRootDir().toPath().resolve("core/resources/").toFile());
            task.getOutputDirectory().set(this.resourceOutputDir(options.getSourceSet()));

            this.rootTask.get().dependsOn(task);
        });
    }
}
