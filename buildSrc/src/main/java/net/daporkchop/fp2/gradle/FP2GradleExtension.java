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

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gradle.deletemixin.DeleteMixin;
import net.daporkchop.fp2.gradle.translateresources.struct.TranslateResourcesOptions;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class FP2GradleExtension extends GroovyObjectSupport {
    @NonNull
    protected final Project project;
    @NonNull
    protected final FP2GradlePlugin plugin;

    //
    // deletemixin
    //

    public Dependency deleteMixin(Object dependency) {
        return this.deleteMixin(dependency, null);
    }

    public Dependency deleteMixin(Object dependency, Closure<?> closure) {
        return ((ModuleDependency) this.project.getDependencies().create(dependency, closure))
                .attributes(attributeContainer -> attributeContainer.attribute(DeleteMixin.ATTRIBUTE, true));
    }

    //
    // translateresources
    //

    public void translateResources(Action<? super TranslateResourcesOptions.TranslateResourcesOptionsBuilder> configureAction) {
        this.plugin.translateResources().translateResources(TranslateResourcesOptions.buildFrom(configureAction));
    }
}
