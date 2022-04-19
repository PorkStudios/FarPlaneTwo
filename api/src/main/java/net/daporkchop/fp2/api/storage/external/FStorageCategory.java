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

package net.daporkchop.fp2.api.storage.external;

import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;

import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A container which can store {@link FStorageItem items} and other categories. This allows organizing individual storage entries into a kind of directory structure.
 *
 * @author DaPorkchop_
 */
public interface FStorageCategory extends AutoCloseable {
    /**
     * Closes this category.
     * <p>
     * All children of this category must be closed prior to calling this method.
     */
    @Override
    void close() throws FStorageException;

    //
    // CATEGORIES
    //

    /**
     * @return a {@link Set} containing the names of all the child categories that exist
     */
    Set<String> allCategories() throws FStorageException;

    /**
     * Opens the child category with the given name, failing it if it doesn't exist.
     *
     * @param name the name of the category to open
     * @return the category
     * @throws NoSuchElementException if no category with the given name exists
     * @throws IllegalStateException  if the category is currently open
     */
    FStorageCategory openCategory(@NonNull String name) throws FStorageException, NoSuchElementException, IllegalStateException;

    /**
     * Opens the child category with the given name, creating it if it doesn't exist.
     *
     * @param name the name of the category to open
     * @return the category
     * @throws IllegalStateException if the category is currently open
     */
    FStorageCategory openOrCreateCategory(@NonNull String name) throws FStorageException, IllegalStateException;

    /**
     * Deletes the child category with the given name, and all of its children. The category must not be open.
     *
     * @param name the name of the category to delete
     * @throws NoSuchElementException if no category with the given name exists
     * @throws IllegalStateException  if the category is currently open
     */
    void deleteCategory(@NonNull String name) throws FStorageException, NoSuchElementException, IllegalStateException;

    //
    // ITEMS
    //

    /**
     * @return a {@link Set} containing the names of all the child items that exist
     */
    Set<String> allItems() throws FStorageException;

    /**
     * Opens the child item with the given name, failing it if it doesn't exist.
     *
     * @param name    the name of the item to open
     * @param factory the {@link FStorageItemFactory} to use to initialize the item
     * @return the item
     * @throws NoSuchElementException if no item with the given name exists
     * @throws IllegalStateException  if the item is currently open
     */
    <I extends FStorageItem> I openItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, NoSuchElementException, IllegalStateException;

    /**
     * Opens the child item with the given name, creating it if it doesn't exist.
     *
     * @param name    the name of the item to open
     * @param factory the {@link FStorageItemFactory} to use to initialize the item
     * @return the item
     * @throws IllegalStateException if the item is currently open
     */
    <I extends FStorageItem> I openOrCreateItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, IllegalStateException;

    /**
     * Deletes the child item with the given name. The item must not be open.
     *
     * @param name the name of the item to delete
     * @throws NoSuchElementException if no item with the given name exists
     * @throws IllegalStateException  if the item is currently open
     */
    void deleteItem(@NonNull String name) throws FStorageException, NoSuchElementException, IllegalStateException;
}
