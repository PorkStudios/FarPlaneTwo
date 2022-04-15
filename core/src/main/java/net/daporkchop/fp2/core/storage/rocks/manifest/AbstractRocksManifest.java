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
import lombok.SneakyThrows;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksWriteAccess;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

import java.nio.charset.StandardCharsets;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractRocksManifest<M> {
    protected static final String SEPARATOR = ".";

    protected static String escape(@NonNull String raw) { //uses the same mangling scheme as JNI method names
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) { //the character can be used without changes
                builder.append(c);
            } else if (c == '_') {
                builder.append("_1");
            } else if (c == ';') {
                builder.append("_2");
            } else if (c == '[') {
                builder.append("_3");
            } else {
                builder.append(String.format("_0%04x", (int) c));
            }
        }
        return builder.toString();
    }

    protected static String unescape(@NonNull String escaped) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) { //the character can be used without changes
                builder.append(c);
            } else if (c == '_') {
                c = escaped.charAt(i++);
                switch (c) {
                    case '0': { //decode 4-digit hex sequence
                        String next = escaped.substring(i, i += 4);
                        builder.append((char) Integer.parseUnsignedInt(next));
                        break;
                    }
                    case '1':
                        builder.append('_');
                        break;
                    case '2':
                        builder.append(';');
                        break;
                    case '3':
                        builder.append('[');
                        break;
                    default:
                        throw new IllegalArgumentException("illegal escape sequence: _" + c);
                }
            } else {
                builder.append(String.format("_0%04x", (int) c));
            }
        }
        return builder.toString();
    }

    protected static byte[] increment(@NonNull byte[] src) {
        checkArg(src.length > 0, "cannot increment zero-length byte[]");

        byte[] dst = src.clone();
        for (int i = src.length - 1; i >= 0 && ++dst[i] == 0; i--) {
            if (i == 0) { //we reached the end of the array...
                throw new IllegalStateException("cannot increment byte[] as all bits were already set!");
            }
        }
        return dst;
    }

    @NonNull
    protected final ColumnFamilyHandle columnFamily;
    @NonNull
    protected final String inode;

    @SneakyThrows(RocksDBException.class)
    public void clear(@NonNull IRocksWriteAccess access) {
        //do a simple deleteRange on the whole prefix space
        byte[] keyBase = (this.inode + SEPARATOR).getBytes(StandardCharsets.UTF_8);
        access.deleteRange(this.columnFamily, keyBase, increment(keyBase));
    }
}
