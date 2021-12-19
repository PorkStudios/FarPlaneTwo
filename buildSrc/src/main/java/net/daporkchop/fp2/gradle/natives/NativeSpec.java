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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gradle.natives.struct.NativeArchitecture;
import net.daporkchop.fp2.gradle.natives.struct.NativeModule;
import net.daporkchop.fp2.gradle.natives.struct.NativeOperatingSystem;
import net.daporkchop.fp2.gradle.natives.struct.NativeSIMDExtension;
import net.daporkchop.fp2.gradle.natives.struct.NativesExtension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@Getter
@EqualsAndHashCode
public final class NativeSpec implements Serializable {
    private final String moduleName;
    private final String moduleRoot;

    private final String architectureName;

    private final String operatingSystemName;
    private final String operatingSystemSharedLibraryExtension;

    private final String simdExtensionName;

    private final Set<String> libraries = new HashSet<>();

    private final List<String> cxxFlags = new ArrayList<>();
    private final List<String> linkerFlags = new ArrayList<>();
    private final Set<String> includeDirectories = new HashSet<>();
    private final Map<String, String> defines = new HashMap<>();

    public NativeSpec(@NonNull NativesExtension extension, @NonNull NativeModule module, @NonNull NativeArchitecture architecture, @NonNull NativeOperatingSystem operatingSystem, NativeSIMDExtension simdExtension) {
        this.moduleName = module.getName();
        this.moduleRoot = module.getRoot().get();

        this.architectureName = architecture.getName();

        this.operatingSystemName = operatingSystem.getName();
        this.operatingSystemSharedLibraryExtension = operatingSystem.getSharedLibraryExtension().get();

        this.simdExtensionName = Optional.ofNullable(simdExtension).map(NativeSIMDExtension::getName).orElse(null);

        Stream.of(extension, module, architecture, operatingSystem, simdExtension).filter(Objects::nonNull)
                .forEach(options -> {
                    this.libraries.addAll(options.getLibraries().getOrElse(Collections.emptySet()));
                    this.cxxFlags.addAll(options.getCxxFlags().getOrElse(Collections.emptyList()));
                    this.linkerFlags.addAll(options.getLinkerFlags().getOrElse(Collections.emptyList()));
                    this.includeDirectories.addAll(options.getIncludeDirectories().getOrElse(Collections.emptySet()));
                });

        this.defines.put("FP2_MODULE", this.moduleRoot.replace('/', '_'));
        if (simdExtension != null) {
            this.defines.put("_FP2_VEC_SIZE", String.valueOf(simdExtension.getRegisterWidth().get()));
        }
    }

    public String platformString() {
        return this.architectureName + '-' + this.operatingSystemName;
    }

    public String descriptionText() {
        return this.platformString() + Optional.ofNullable(this.simdExtensionName).map(" with "::concat).orElse("");
    }

    private String outputDirectory(String stage) {
        return Natives.BUILD_DIR_NAME + '/' + stage + '/' + this.moduleName + '/' + this.platformString() + Optional.ofNullable(this.simdExtensionName).map(name -> '/' + name).orElse("");
    }

    public String librariesOutputDirectory() {
        return this.outputDirectory("libraries") + "/libraries";
    }

    public String compileOutputDirectory() {
        return this.outputDirectory("compile");
    }

    public String linkedLibraryFile() {
        return this.moduleRoot + '/' + this.platformString() + Optional.ofNullable(this.simdExtensionName).map(name -> '/' + name).orElse("") + '.' + this.operatingSystemSharedLibraryExtension;
    }

    public String rootTaskName() {
        return "natives_" + this.platformString() + Optional.ofNullable(this.simdExtensionName).map(name -> '_' + name).orElse("") + '_' + this.moduleName;
    }

    public String librariesTaskName() {
        return this.rootTaskName() + "_libraries";
    }

    public String compileTaskName() {
        return this.rootTaskName() + "_compile";
    }

    public String linkTaskName() {
        return this.rootTaskName() + "_link";
    }
}
