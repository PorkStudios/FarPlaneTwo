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

package net.daporkchop.fp2.core.storage.rocks.manifest;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.compression.context.PDeflater;
import net.daporkchop.lib.compression.context.PInflater;
import net.daporkchop.lib.compression.zstd.Zstd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractRocksManifest<M> extends ReentrantReadWriteLock {
    @NonNull
    private final Path filePath;

    private transient Thread snapshotThread = null;

    protected abstract int version();

    public void assertReadLock() {
        //no-op: we can't check this.readLock().isHeldByCurrentThread()
    }

    public void assertWriteLock() {
        checkState(this.writeLock().isHeldByCurrentThread(), "cannot modify manifest without holding a write lock!");
    }

    public void clear() {
        this.assertWriteLock();

        this.clear0();
    }

    protected abstract void clear0();

    public M load() throws FStorageException {
        //the state is going to be modified, we need a write lock
        this.writeLock().lock();
        try {
            //read from the given input stream, and roll back to initial state if something goes wrong
            this.snapshot();
            try {
                if (PFiles.checkFileExists(this.filePath)) { //manifest file exists, read from it
                    try (PInflater inflater = Zstd.PROVIDER.inflater();
                         DataIn in = inflater.decompressionStream(DataIn.wrap(Files.newInputStream(this.filePath, READ)))) {
                        this.read0(in.readVarInt(), in);
                    }
                } else { //manifest file doesn't exist, clear this instance and then save it
                    this.clear0();
                    this.save();
                }
            } catch (Exception e) {
                this.rollback();
                throw new FStorageException("failed to load manifest", e);
            } finally {
                this.clearSnapshot();
            }

            return uncheckedCast(this);
        } finally {
            this.writeLock().unlock();
        }
    }

    protected abstract void read0(int version, DataIn in) throws IOException;

    protected void save() throws FStorageException {
        //we need a write lock because we're going to be modifying the file on disk
        this.assertWriteLock();

        Path tempFile = this.filePath.resolveSibling(this.filePath.getFileName().toString() + ".tmp");

        try {
            //write data to the temporary manifest file, synchronously
            try (PDeflater deflater = Zstd.PROVIDER.deflater();
                 DataOut out = deflater.compressionStream(DataOut.wrap(Files.newOutputStream(tempFile, WRITE, CREATE, TRUNCATE_EXISTING, SYNC, DSYNC)), 64 << 10)) {
                out.writeVarInt(this.version());
                this.write0(out);
            }

            //atomically replace the existing manifest file, if any
            Files.move(tempFile, this.filePath, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (Exception e) {
            this.rollback();
            throw new FStorageException("failed to save manifest", e);
        }
    }

    protected abstract void write0(DataOut out) throws IOException;

    public void snapshot() {
        this.assertWriteLock();

        checkState(this.snapshotThread == null, "a snapshot already exists");
        this.snapshotThread = Thread.currentThread();

        this.snapshot0();
    }

    protected abstract void snapshot0();

    public void rollback() {
        this.assertWriteLock();

        checkState(this.snapshotThread != null, "no snapshot currently exists");
        checkState(this.snapshotThread == Thread.currentThread(), "existing snapshot is owned by thread %s", this.snapshotThread);

        this.rollback0();
    }

    protected abstract void rollback0();

    public void clearSnapshot() {
        this.assertWriteLock();

        checkState(this.snapshotThread != null, "no snapshot currently exists");
        checkState(this.snapshotThread == Thread.currentThread(), "existing snapshot is owned by thread %s", this.snapshotThread);

        this.clearSnapshot0();
        this.snapshotThread = null;
    }

    protected abstract void clearSnapshot0();

    public void runWithReadLock(Consumer<M> action) {
        boolean needsLock = !this.writeLock().isHeldByCurrentThread();
        if (needsLock) { //only acquire a read lock if we don't already hold a write lock
            this.readLock().lock();
        }
        try {
            action.accept(uncheckedCast(this));
        } finally {
            if (needsLock) {
                this.readLock().unlock();
            }
        }
    }

    public <R> R getWithReadLock(Function<M, R> action) {
        boolean needsLock = !this.writeLock().isHeldByCurrentThread();
        if (needsLock) { //only acquire a read lock if we don't already hold a write lock
            this.readLock().lock();
        }
        try {
            return action.apply(uncheckedCast(this));
        } finally {
            if (needsLock) {
                this.readLock().unlock();
            }
        }
    }

    public void runWithWriteLock(Consumer<M> action) {
        //we don't want to allow users to acquire a write lock whenever they feel like, so we assume it's already been acquired from update()
        this.assertWriteLock();
        action.accept(uncheckedCast(this));
    }

    public <R> R getWithWriteLock(Function<M, R> action) {
        //we don't want to allow users to acquire a write lock whenever they feel like, so we assume it's already been acquired from update()
        this.assertWriteLock();
        return action.apply(uncheckedCast(this));
    }

    public void update(Consumer<M> action) throws FStorageException {
        //acquire write lock because we're about to be modifying the manifest data
        this.writeLock().lock();
        try {
            this.snapshot();
            try {
                //update self using the given modification action
                action.accept(uncheckedCast(this));

                //save the modified data
                this.save();
            } catch (Exception e) { //something went wrong, roll back changes and rethrow
                this.rollback();
                throw new FStorageException("failed to update manifest", e);
            } finally {
                this.clearSnapshot();
            }
        } finally {
            this.writeLock().unlock();
        }
    }
}
