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

package net.daporkchop.fp2.gl.opengl.attribute.struct.layout;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@SuperBuilder
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class InterleavedStructLayout extends StructLayout<InterleavedStructLayout.Member, InterleavedStructLayout.Component> {
    private final long stride;

    /**
     * @author DaPorkchop_
     */
    public interface Member extends StructLayout.Member<Member, Component> {
    }

    /**
     * @author DaPorkchop_
     */
    public interface Component extends StructLayout.Component {
    }

    /**
     * @author DaPorkchop_
     */
    @Data
    public static final class RegularMember implements Member {
        private final long offset;
        @NonNull
        private final long[] componentOffsets;

        @Override
        public int components() {
            return this.componentOffsets.length;
        }

        @Override
        public Component component(int componentIndex) {
            checkIndex(this.componentOffsets.length, componentIndex);
            return () -> this.offset + this.componentOffsets[componentIndex];
        }

        @Override
        public int children() {
            return 0;
        }

        @Override
        public Member child(int childIndex) {
            throw new IndexOutOfBoundsException(String.valueOf(childIndex));
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Data
    public static final class NestedMember implements Member {
        @NonNull
        private final Member[] children;

        @Override
        public int components() {
            return 0;
        }

        @Override
        public Component component(int componentIndex) {
            throw new IndexOutOfBoundsException(String.valueOf(componentIndex));
        }

        @Override
        public int children() {
            return this.children.length;
        }

        @Override
        public Member child(int childIndex) {
            return this.children[childIndex];
        }
    }
}
