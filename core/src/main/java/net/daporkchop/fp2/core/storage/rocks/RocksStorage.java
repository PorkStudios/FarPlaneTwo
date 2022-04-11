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

package net.daporkchop.fp2.core.storage.rocks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorage;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageItem;
import net.daporkchop.fp2.api.storage.external.FStorageItemFactory;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.compression.context.PDeflater;
import net.daporkchop.lib.compression.context.PInflater;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompressionOptions;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.FlushOptions;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link FStorage} built on <a href="https://github.com/facebook/rocksdb">rocksdb</a>.
 *
 * @author DaPorkchop_
 */
public abstract class RocksStorage<DB extends RocksDB> extends ReentrantReadWriteLock implements FStorage {
    static {
        RocksDB.loadLibrary(); //ensure rocksdb is loaded (ForgeRocks does this automatically, but whatever)
    }

    protected static final ReadOptions READ_OPTIONS = new ReadOptions();
    protected static final WriteOptions WRITE_OPTIONS = new WriteOptions();
    protected static final FlushOptions FLUSH_OPTIONS = new FlushOptions().setWaitForFlush(true).setAllowWriteStall(true);

    private final Path root;

    private final DB db;

    private final DBOptions dbOptions = this.createDBOptions();
    private final Map<FStorageColumnHintsInternal, ColumnFamilyOptions> cfOptions = new ObjObjOpenHashMap<>();

    private final Map<String, ColumnFamilyHandle> openColumnFamilyHandles = new ObjObjOpenHashMap<>();
    private final Set<String> usedColumnFamilyNames = new TreeSet<>();

    @Getter
    private final RocksStorageManifest manifest;

    private final RocksStorageCategory rootCategory;

    private volatile boolean open = true;

    protected RocksStorage(Path root) throws FStorageException {
        this.root = PFiles.ensureDirectoryExists(root);

        this.writeLock().lock();
        try {
            //try to load the manifest
            Optional<RocksStorageManifest> optionalManifest = this.loadManifest();
            if (optionalManifest.isPresent()) { //manifest exists
                this.manifest = optionalManifest.get();
            } else { //manifest doesn't exist, nuke the folder and create a new storage
                PFiles.rmContents(root);
                this.manifest = new RocksStorageManifest();

                //save the new manifest
                this.writeManifest();
            }

            //prepare column families for load
            List<String> cfNames = new ArrayList<>();
            List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();
            this.manifest.allColumnFamilies().forEach((name, hints) -> {
                cfNames.add(name);
                cfDescriptors.add(new ColumnFamilyDescriptor(name.getBytes(StandardCharsets.UTF_8), this.cfOptions.computeIfAbsent(hints, this::createColumnFamilyOptionsForHints)));
            });

            //default column family must be registered
            cfNames.add(new String(RocksDB.DEFAULT_COLUMN_FAMILY).intern());
            cfDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));

            try {
                //open db
                List<ColumnFamilyHandle> cfHandles = new ArrayList<>();
                this.db = this.createDB(this.dbOptions, root.toString(), cfDescriptors, cfHandles);

                //save all the opened column family handles
                for (int i = 0; i < cfNames.size(); i++) {
                    this.openColumnFamilyHandles.put(cfNames.get(i), cfHandles.get(i));
                }
            } catch (RocksDBException e) {
                throw new FStorageException("failed to open db", e);
            }

            //create root storage category
            this.rootCategory = new RocksStorageCategory.Root(this, this.manifest.rootCategory());
        } finally {
            this.writeLock().unlock();
        }
    }

    protected Optional<RocksStorageManifest> loadManifest() throws FStorageException {
        Path manifestPath = this.root.resolve("manifest_v0");

        try {
            if (!PFiles.checkFileExists(manifestPath)) { //manifest doesn't exist
                return Optional.empty();
            }

            //read manifest data
            byte[] compressedManifest = Files.readAllBytes(manifestPath);

            //decompress it
            byte[] decompressedManifest = new byte[Zstd.PROVIDER.frameContentSize(Unpooled.wrappedBuffer(compressedManifest))];
            try (PInflater inflater = Zstd.PROVIDER.inflater()) {
                checkState(inflater.decompress(Unpooled.wrappedBuffer(compressedManifest), Unpooled.wrappedBuffer(decompressedManifest).clear()));
            }

            //deserialize it
            return Optional.of(RocksStorageManifest.read(DataIn.wrap(Unpooled.wrappedBuffer(decompressedManifest), false)));
        } catch (IOException e) {
            throw new FStorageException("exception while loading manifest", e);
        }
    }

    protected void writeManifest() throws FStorageException {
        checkState(this.writeLock().isHeldByCurrentThread(), "cannot modify manifest without holding a write lock!");

        Path manifestPath = this.root.resolve("manifest_v0");
        Path tempManifestPath = this.root.resolve("manifest_tmp");

        ByteBuf uncompressed = ByteBufAllocator.DEFAULT.buffer();
        ByteBuf compressed = ByteBufAllocator.DEFAULT.buffer();
        try {
            //serialize manifest data
            this.manifest.write(DataOut.wrap(uncompressed, false));

            //compress it
            try (PDeflater deflater = Zstd.PROVIDER.deflater()) {
                deflater.compressGrowing(uncompressed, compressed);
            }

            //copy buffer contents to a byte[] and write it to a file synchronously
            byte[] arr = new byte[compressed.readableBytes()];
            compressed.readBytes(arr);
            Files.write(tempManifestPath, arr, CREATE, TRUNCATE_EXISTING, WRITE, SYNC, DSYNC);

            //new manifest data has been synced to disk, do an atomic move
            Files.move(tempManifestPath, manifestPath, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (IOException e) {
            throw new FStorageException("exception while loading manifest", e);
        } finally {
            compressed.release();
            uncompressed.release();
        }
    }

    protected abstract DB createDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException;

    protected DBOptions createDBOptions() {
        return new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true)
                .setAllowConcurrentMemtableWrite(true)
                .setKeepLogFileNum(1L);
    }

    protected void closeDBOptions(@NonNull DBOptions options) {
        options.close();
    }

    protected ColumnFamilyOptions createColumnFamilyOptionsForHints(@NonNull FStorageColumnHintsInternal hints) {
        ColumnFamilyOptions options = new ColumnFamilyOptions();

        if (hints.compressability() != FStorageColumnHintsInternal.Compressability.NONE) {
            options.setCompressionType(CompressionType.ZSTD_COMPRESSION)
                    .setCompressionOptions(new CompressionOptions().setEnabled(true)
                            .setMaxDictBytes(64 << 10)
                            .setZStdMaxTrainBytes(64 << 10));
        }

        return options;
    }

    protected void closeColumnFamilyOptions(@NonNull ColumnFamilyOptions options) {
        if (options.compressionOptions() != null) { //compression options are set, close them
            options.compressionOptions().close();
        }

        options.close();
    }

    @Override
    public void close() throws FStorageException {
        this.writeLock().lock();
        try {
            this.ensureOpen();
            this.open = false;

            try {
                //flush all column families
                this.db.flush(FLUSH_OPTIONS, new ArrayList<>(this.openColumnFamilyHandles.values()));

                //delete any column families whose deletion is pending
                this.cleanColumnFamiliesPendingDeletion();

                //close all column families, then the db itself
                this.openColumnFamilyHandles.values().forEach(ColumnFamilyHandle::close);
            } finally {
                this.db.close();
            }
        } catch (RocksDBException e) {
            throw new FStorageException("failed to close db", e);
        } finally {
            //release allocated Options instances
            this.dbOptions.close();
            this.cfOptions.values().forEach(this::closeColumnFamilyOptions);

            this.writeLock().unlock();
        }
    }

    //
    // State management helpers
    //

    public void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("storage has been closed!");
        }
    }

    public ColumnFamilyHandle beginUsingColumnFamily(@NonNull String columnFamilyName) throws FStorageException {
        return this.beginUsingColumnFamilies(Collections.singleton(columnFamilyName)).get(columnFamilyName);
    }

    public Map<String, ColumnFamilyHandle> beginUsingColumnFamilies(@NonNull Collection<String> columnFamilyNames) throws FStorageException {
        checkState(this.writeLock().isHeldByCurrentThread(), "cannot begin using column families without holding a write lock!");

        columnFamilyNames.forEach(columnFamilyName -> {
            checkState(!this.usedColumnFamilyNames.contains(columnFamilyName), "column family '%s' is already being used", columnFamilyName);

            checkState(this.manifest.allColumnFamilies().containsKey(columnFamilyName), "column family '%s' isn't declared in the manifest", columnFamilyName);
            checkState(!this.manifest.columnFamiliesPendingDeletion().contains(columnFamilyName), "column family '%s' is pending deletion", columnFamilyName);
        });

        Map<String, ColumnFamilyHandle> out = new ObjObjOpenHashMap<>(columnFamilyNames.size());

        List<String> columnFamiliesToCreateNames = new ArrayList<>();
        List<ColumnFamilyDescriptor> columnFamiliesToCreateDescriptors = new ArrayList<>();

        columnFamilyNames.stream().distinct().forEach(columnFamilyName -> {
            ColumnFamilyHandle handle = this.openColumnFamilyHandles.get(columnFamilyName);
            if (handle != null) { //the named column family is already open
                out.put(columnFamilyName, handle);
            } else { //enqueue the column family to be created
                FStorageColumnHintsInternal hints = this.manifest.allColumnFamilies().get(columnFamilyName);

                columnFamiliesToCreateNames.add(columnFamilyName);
                columnFamiliesToCreateDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName.getBytes(StandardCharsets.UTF_8), this.createColumnFamilyOptionsForHints(hints)));
            }
        });

        if (!columnFamiliesToCreateNames.isEmpty()) { //we need to create some column families
            try {
                List<ColumnFamilyHandle> handles = this.db.createColumnFamilies(columnFamiliesToCreateDescriptors);

                //save the created column families into the map
                for (int i = 0; i < columnFamiliesToCreateNames.size(); i++) {
                    this.openColumnFamilyHandles.put(columnFamiliesToCreateNames.get(i), handles.get(i));
                    out.put(columnFamiliesToCreateNames.get(i), handles.get(i));
                }
            } catch (RocksDBException e) {
                throw new FStorageException("failed to create column families", e);
            }
        }

        //now that all of the column family handles have been retrieved, mark all of them as used simultaneously
        // (doing it this way ensures that we won't be in an invalid state if we fail halfway through)
        this.usedColumnFamilyNames.addAll(columnFamilyNames);

        return out;
    }

    public void stopUsingColumnFamily(@NonNull String columnFamilyName) throws FStorageException {
        this.stopUsingColumnFamilies(Collections.singleton(columnFamilyName));
    }

    public void stopUsingColumnFamilies(@NonNull Collection<String> columnFamilyNames) throws FStorageException {
        checkState(this.writeLock().isHeldByCurrentThread(), "cannot stop using column families without holding a write lock!");

        if (!this.usedColumnFamilyNames.containsAll(columnFamilyNames)) {
            columnFamilyNames.forEach(columnFamilyName -> checkState(!this.usedColumnFamilyNames.contains(columnFamilyName), "column family '%s' is not being used", columnFamilyName));
        }

        //un-mark all the column families as used
        this.usedColumnFamilyNames.removeAll(columnFamilyNames);

        if (columnFamilyNames.stream().anyMatch(this.manifest.columnFamiliesPendingDeletion()::contains)) { //some of the column families are pending deletion, try to clean up the pending deletion queue
            this.cleanColumnFamiliesPendingDeletion();
        }
    }

    public void cleanColumnFamiliesPendingDeletion() throws FStorageException {
        checkState(this.writeLock().isHeldByCurrentThread(), "cannot clean column families without holding a write lock!");

        if (this.manifest.columnFamiliesPendingDeletion().isEmpty()) { //nothing to do
            return;
        }

        //filter out column families which aren't open (if they aren't open, that means they were never actually created so we can just remove them from the pending deletion queue)
        Set<String> columnFamilyNamesToDelete = this.manifest.columnFamiliesPendingDeletion().stream()
                .filter(columnFamilyName -> this.openColumnFamilyHandles.containsKey(columnFamilyName) && !this.usedColumnFamilyNames.contains(columnFamilyName))
                .collect(Collectors.toSet());

        if (!columnFamilyNamesToDelete.isEmpty()) {
            try {
                //get handles of all the column families we're about to delete and drop them
                this.db.dropColumnFamilies(columnFamilyNamesToDelete.stream()
                        .map(this.openColumnFamilyHandles::get)
                        .collect(Collectors.toList()));
            } catch (RocksDBException e) {
                throw new FStorageException("failed to delete column families", e);
            }

            //the column families have been dropped

            //remove and close their handles
            columnFamilyNamesToDelete.forEach(columnFamilyName -> this.openColumnFamilyHandles.remove(columnFamilyName).close());

            this.manifest.snapshot();
            try {
                //remove the column families from the manifest and un-mark them as pending deletion
                this.manifest.allColumnFamilies().keySet().removeAll(columnFamilyNamesToDelete);
                this.manifest.columnFamiliesPendingDeletion().removeAll(columnFamilyNamesToDelete);

                //save the updated manifest
                this.writeManifest();
            } catch (Exception e) {
                this.manifest.rollback();
                throw new FStorageException("failed to update manifest after column family deletion", e);
            } finally {
                this.manifest.clearSnapshot();
            }
        }
    }

    //
    // FStorageCategory methods
    // (these all delegate to this.rootCategory)
    //

    @Override
    public Set<String> allCategories() {
        return this.rootCategory.allCategories();
    }

    @Override
    public Map<String, FStorageCategory> openCategories() {
        return this.rootCategory.openCategories();
    }

    @Override
    public FStorageCategory openCategory(@NonNull String name) throws FStorageException, NoSuchElementException {
        return this.rootCategory.openCategory(name);
    }

    @Override
    public FStorageCategory openOrCreateCategory(@NonNull String name) throws FStorageException {
        return this.rootCategory.openOrCreateCategory(name);
    }

    @Override
    public void deleteCategories(@NonNull Collection<String> names) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.rootCategory.deleteCategories(names);
    }

    @Override
    public Set<String> allItems() {
        return this.rootCategory.allItems();
    }

    @Override
    public Map<String, ? extends FStorageItem> openItems() {
        return this.rootCategory.openItems();
    }

    @Override
    public <I extends FStorageItem> I openItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, NoSuchElementException {
        return this.rootCategory.openItem(name, factory);
    }

    @Override
    public <I extends FStorageItem> I openOrCreateItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException {
        return this.rootCategory.openOrCreateItem(name, factory);
    }

    @Override
    public void deleteItems(@NonNull Collection<String> names) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.rootCategory.deleteItems(names);
    }

    /**
     * Implementation of {@link RocksStorage} backed by an {@link OptimisticTransactionDB}.
     *
     * @author DaPorkchop_
     */
    protected static class OptimisticRocksStorage extends RocksStorage<OptimisticTransactionDB> {
        protected OptimisticRocksStorage(Path root) throws FStorageException {
            super(root);
        }

        @Override
        protected OptimisticTransactionDB createDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
            return OptimisticTransactionDB.open(dbOptions, path, columnFamilyDescriptors, columnFamilyHandles);
        }
    }
}
