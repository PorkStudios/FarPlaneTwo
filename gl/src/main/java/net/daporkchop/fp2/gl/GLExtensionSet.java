/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.gl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A compact, immutable set of {@link GLExtension}s.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GLExtensionSet implements Iterable<GLExtension> {
    private static final GLExtension[] VALUES = GLExtension.values();

    private static final int WORDS = 2;
    private static final long BITS_OFFSET;

    static {
        if (VALUES.length > WORDS * Long.SIZE) {
            throw new IllegalStateException();
        }

        long[] offsets = new long[WORDS];
        for (int i = 0; i < WORDS; i++) {
            offsets[i] = PUnsafe.pork_getOffset(GLExtensionSet.class, "bits" + i);
        }
        Arrays.sort(offsets);
        for (int i = 1; i < WORDS; i++) {
            if (offsets[i] != offsets[i - 1] + Long.BYTES) {
                throw new IllegalStateException(Arrays.toString(offsets));
            }
        }
        BITS_OFFSET = offsets[0];
    }

    private static final GLExtensionSet EMPTY = new GLExtensionSet();

    /**
     * @return an empty {@link GLExtensionSet}
     */
    public static GLExtensionSet empty() {
        return EMPTY;
    }

    private long bits0;
    private long bits1;

    private static int ord2word(int ordinal) {
        return ordinal >> 6;
    }

    private long getBitsWord(int word) {
        return PUnsafe.getLong(this, BITS_OFFSET + (long) word * Long.BYTES);
    }

    private void setBitsWord(int word, long value) {
        PUnsafe.putLong(this, BITS_OFFSET + (long) word * Long.BYTES, value);
    }

    private GLExtensionSet(GLExtensionSet other) {
        for (int word = 0; word < WORDS; word++) {
            this.setBitsWord(word, other.getBitsWord(word));
        }
    }

    /**
     * Checks if this set contains the given extension.
     *
     * @param extension the extension
     * @return {@code true} if this set contains the given extension
     */
    public boolean contains(GLExtension extension) {
        int ordinal = extension.ordinal();
        return (this.getBitsWord(ord2word(ordinal)) & (1L << ordinal)) != 0L;
    }

    /**
     * Checks if this set contains all of the given extensions.
     *
     * @param extensions the extensions
     * @return {@code true} if this set contains all of the given extensions
     */
    public boolean containsAll(GLExtensionSet extensions) {
        for (int word = 0; word < WORDS; word++) {
            if ((~this.getBitsWord(word) & extensions.getBitsWord(word)) != 0L) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this set contains any of the given extensions.
     *
     * @param extensions the extensions
     * @return {@code true} if this set contains any of the given extensions
     */
    public boolean containsAny(GLExtensionSet extensions) {
        for (int word = 0; word < WORDS; word++) {
            if ((this.getBitsWord(word) & extensions.getBitsWord(word)) != 0L) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a set which contains the union of this set and the given extension.
     *
     * @param extension the extension to add
     * @return the union of this set and the given element
     */
    public GLExtensionSet add(GLExtension extension) {
        if (this.contains(extension)) {
            return this;
        }
        int ordinal = extension.ordinal();
        int word = ord2word(ordinal);

        GLExtensionSet result = new GLExtensionSet(this);
        result.setBitsWord(word, this.getBitsWord(word) | (1L << ordinal));
        return result;
    }

    /**
     * Gets a set which contains the union of this set and the given set.
     *
     * @param extensions the other set
     * @return the union of this set and the given set
     */
    public GLExtensionSet addAll(GLExtensionSet extensions) {
        if (this.containsAll(extensions)) {
            return this;
        }

        GLExtensionSet result = new GLExtensionSet();
        for (int word = 0; word < WORDS; word++) {
            result.setBitsWord(word, this.getBitsWord(word) | extensions.getBitsWord(word));
        }
        return result;
    }

    /**
     * Gets a set which contains the contents of this set without the given extension.
     *
     * @param extension the extension to remove
     * @return the contents of this set without the given extension
     */
    public GLExtensionSet remove(GLExtension extension) {
        if (!this.contains(extension)) {
            return this;
        }
        int ordinal = extension.ordinal();
        int word = ord2word(ordinal);

        GLExtensionSet result = new GLExtensionSet(this);
        result.setBitsWord(word, this.getBitsWord(word) & ~(1L << ordinal));
        return result;
    }

    /**
     * Gets a set which contains the union of this set and the compliment of the given set.
     *
     * @param extensions the other set
     * @return the union of this set and the compliment of the given set
     */
    public GLExtensionSet removeAll(GLExtensionSet extensions) {
        if (!this.containsAny(extensions)) {
            return this;
        }

        GLExtensionSet result = new GLExtensionSet();
        for (int word = 0; word < WORDS; word++) {
            result.setBitsWord(word, this.getBitsWord(word) & ~extensions.getBitsWord(word));
        }
        return result;
    }

    /**
     * @return {@code true} if this set is empty
     */
    public boolean isEmpty() {
        long acc = 0L;
        for (int word = 0; word < WORDS; word++) {
            acc |= this.getBitsWord(word);
        }
        return acc == 0L;
    }

    /**
     * @return the number of elements in this set
     */
    public int size() {
        int acc = 0;
        for (int word = 0; word < WORDS; word++) {
            acc += Long.bitCount(this.getBitsWord(word));
        }
        return acc;
    }

    @Override
    public int hashCode() {
        long acc = 0L;
        for (int word = 0; word < WORDS; word++) {
            acc = acc * 31L + this.getBitsWord(word);
        }
        return PMath.mix32(acc);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GLExtensionSet) {
            GLExtensionSet other = (GLExtensionSet) obj;
            for (int word = 0; word < WORDS; word++) {
                if (this.getBitsWord(word) != other.getBitsWord(word)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder().append('[');
        for (int word = 0; word < WORDS; word++) {
            long bits = this.getBitsWord(word);
            while (bits != 0L) {
                int index = Long.numberOfTrailingZeros(bits);
                bits &= ~(1L << index);

                if (builder.length() > 1) {
                    builder.append(", ");
                }
                builder.append(VALUES[word * Long.SIZE + index].name());
            }
        }
        return builder.append(']').toString();
    }

    @Override
    public Iterator<GLExtension> iterator() { //this is slow but i don't care
        List<GLExtension> list = new ArrayList<>(this.size());
        this.forEach(list::add);
        return Collections.unmodifiableList(list).iterator();
    }

    @Override
    public void forEach(@NonNull Consumer<? super GLExtension> action) {
        for (int word = 0; word < WORDS; word++) {
            long bits = this.getBitsWord(word);
            while (bits != 0L) {
                int index = Long.numberOfTrailingZeros(bits);
                bits &= ~(1L << index);

                action.accept(VALUES[word * Long.SIZE + index]);
            }
        }
    }
}
