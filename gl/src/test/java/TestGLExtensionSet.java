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

import lombok.val;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author DaPorkchop_
 */
public class TestGLExtensionSet {
    @Test
    public void testEmpty() {
        val empty = GLExtensionSet.empty();

        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());
        assertEquals(GLExtensionSet.empty(), empty);
        assertEquals("[]", empty.toString());

        for (val ext : empty) {
            throw new IllegalStateException(ext.name());
        }
        empty.forEach(ext -> {
            throw new IllegalStateException(ext.name());
        });
        assertArrayEquals(new GLExtension[0], empty.toArray());

        for (val ext : GLExtension.values()) {
            assertFalse(empty.contains(ext), ext::name);
        }

        assertTrue(empty.containsAll(empty));
        assertFalse(empty.containsAny(empty));

        assertSame(empty, empty.addAll(empty));
        assertSame(empty, empty.removeAll(empty));

        for (val ext : GLExtension.values()) {
            val set = GLExtensionSet.empty().add(ext);

            assertNotSame(empty, empty.add(ext));
            assertNotSame(empty, empty.addAll(set));

            assertSame(empty, empty.remove(ext));
            assertSame(empty, empty.removeAll(set));
        }
    }

    @Test
    public void testOneElement() {
        for (val ext : GLExtension.values()) {
            val set = GLExtensionSet.empty().add(ext);

            assertEquals(1, set.size());
            assertFalse(set.isEmpty());
            assertNotEquals(GLExtensionSet.empty(), set);
            assertEquals("[" + ext + "]", set.toString());

            val itr = set.iterator();
            assertTrue(itr.hasNext());
            assertEquals(ext, itr.next());
            assertFalse(itr.hasNext());
            assertThrows(NoSuchElementException.class, itr::next);

            val list = new ArrayList<GLExtension>();
            set.forEach(list::add);
            assertEquals(Collections.singletonList(ext), list);

            assertArrayEquals(new GLExtension[]{ ext }, set.toArray());

            assertSame(set, set.add(ext));
            assertSame(set, set.addAll(set));

            assertEquals(GLExtensionSet.empty(), set.remove(ext));
            assertEquals(GLExtensionSet.empty(), set.removeAll(set));
        }
    }

    @Test
    public void testTwoElements() {
        for (val ext1 : GLExtension.values()) {
            val set1 = GLExtensionSet.empty().add(ext1);

            for (val ext2 : GLExtension.values()) {
                if (ext1 != ext2) {
                    val set2 = set1.add(ext2);

                    assertEquals(2, set2.size());
                    assertFalse(set2.isEmpty());
                    assertNotEquals(GLExtensionSet.empty(), set2);
                    assertNotEquals(set1, set2);

                    val sortedArr = new GLExtension[]{ ext1, ext2 };
                    Arrays.sort(sortedArr);

                    val itr = set2.iterator();
                    assertTrue(itr.hasNext());
                    assertEquals(sortedArr[0], itr.next());
                    assertTrue(itr.hasNext());
                    assertEquals(sortedArr[1], itr.next());
                    assertFalse(itr.hasNext());
                    assertThrows(NoSuchElementException.class, itr::next);

                    val list = new ArrayList<GLExtension>();
                    set2.forEach(list::add);
                    assertEquals(Arrays.asList(sortedArr), list);

                    assertArrayEquals(sortedArr, set2.toArray());
                }
            }
        }
    }
}
