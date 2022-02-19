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

package net.daporkchop.fp2.gradle;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gradle.deletemixin.DeleteMixin;
import net.daporkchop.fp2.gradle.natives.Natives;
import net.daporkchop.fp2.gradle.translateresources.TranslateResources;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author DaPorkchop_
 */
@Getter
public class FP2GradlePlugin implements Plugin<Project> {
    public static String makeFileName(@NonNull String fileName) {
        //put the file in the deobfedDeps folder in order to force mixin to load it as a normal mod
        // see https://github.com/SpongePowered/Mixin/issues/207
        return "deobfedDeps/" + fileName;
    }

    public static Optional<Path> findInPath(@NonNull String name) {
        for (String dir : System.getenv("PATH").split(Pattern.quote(File.pathSeparator))) {
            Path path = Paths.get(dir).resolve(name);
            if (Files.exists(path)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    private DeleteMixin deleteMixin;
    private Natives natives;
    private TranslateResources translateResources;

    @Override
    public void apply(@NonNull Project project) {
        this.deleteMixin = new DeleteMixin(project).register();
        this.natives = new Natives(project).register();
        this.translateResources = new TranslateResources(project).register();

        project.getExtensions().create("fp2", FP2GradleExtension.class, project, this);
    }
}
