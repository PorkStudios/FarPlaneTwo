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

package net.daporkchop.fp2.gradle.deletemixin;

import lombok.SneakyThrows;
import net.daporkchop.fp2.gradle.FP2GradlePlugin;
import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.InputChanges;

import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author DaPorkchop_
 */
@CacheableTransform
public abstract class DeleteMixinTransform implements TransformAction<TransformParameters.None> {
    @Inject
    protected abstract InputChanges getInputChanges();

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputArtifact
    protected abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    @SneakyThrows(IOException.class)
    public void transform(TransformOutputs outputs) {
        File file = this.getInputArtifact().get().getAsFile();
        System.out.println("un-shading mixin from " + file);

        try (ZipFile in = new ZipFile(file);
             ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputs.file(FP2GradlePlugin.makeFileName(file.getName())))))) {
            byte[] buf = new byte[4096];

            for (Enumeration<? extends ZipEntry> enumeration = in.entries(); enumeration.hasMoreElements(); ) {
                ZipEntry entry = enumeration.nextElement();
                if (!entry.getName().startsWith("org/spongepowered/")) {
                    out.putNextEntry(entry);

                    try (InputStream is = in.getInputStream(entry)) {
                        for (int i; (i = is.read(buf)) >= 0; ) {
                            out.write(buf, 0, i);
                        }
                    }
                    out.closeEntry();
                }
            }

            out.finish();
        }
    }
}
