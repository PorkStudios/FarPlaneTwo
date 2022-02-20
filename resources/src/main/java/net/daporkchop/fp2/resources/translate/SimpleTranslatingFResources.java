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

package net.daporkchop.fp2.resources.translate;

import lombok.NonNull;
import net.daporkchop.fp2.resources.FResources;
import net.daporkchop.lib.common.function.io.IOSupplier;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public abstract class SimpleTranslatingFResources extends AbstractTranslatingFResources {
    private final List<Translator> translators = new ArrayList<>();

    public SimpleTranslatingFResources(@NonNull FResources next, @NonNull Translator... translators) {
        super(next);
        this.translators.addAll(Arrays.asList(translators));
    }

    protected void addTranslator(@NonNull Translator translator) {
        this.translators.add(translator);
    }

    @Override
    protected Optional<IOSupplier<InputStream>> getResource(@NonNull Path path, @NonNull FResources next) {
        return this.translators.stream()
                .map(translator -> translator.getFromDstName(path, next))
                .filter(Optional::isPresent)
                .findFirst()
                .orElseGet(() -> next.getResource(path));
    }

    @Override
    protected IOSupplier<Stream<Path>> listResources(@NonNull FResources next) {
        return () -> Stream.concat(
                next.listResources().getThrowing()
                        .flatMap(srcPath ->
                                this.translators.stream()
                                        .map(translator -> translator.expandSrcNames(srcPath, next))
                                        .filter(Optional::isPresent)
                                        .findFirst()
                                        .orElseGet(() -> Optional.of(() -> Stream.of(srcPath)))
                                        .get().get()),
                this.translators.stream().flatMap(translator -> translator.globalSrcNames(next)
                        .map(Supplier::get)
                        .orElseGet(Stream::empty)));
    }

    /**
     * @author DaPorkchop_
     */
    public interface Translator {
        Optional<IOSupplier<Stream<Path>>> expandSrcNames(@NonNull Path srcPath, @NonNull FResources next);

        Optional<IOSupplier<Stream<Path>>> globalSrcNames(@NonNull FResources next);

        Optional<IOSupplier<InputStream>> getFromDstName(@NonNull Path dstPath, @NonNull FResources next);
    }
}
