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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author DaPorkchop_
 */
public class TestGLExtensionSet {
    private final GLExtension[] allExtensions = GLExtension.values();
    
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

        for (val ext : this.allExtensions) {
            assertFalse(empty.contains(ext), ext::name);
        }

        assertTrue(empty.containsAll(empty));
        assertFalse(empty.containsAny(empty));

        assertSame(empty, empty.addAll(empty));
        assertSame(empty, empty.removeAll(empty));

        for (val ext : this.allExtensions) {
            val set = GLExtensionSet.empty().add(ext);

            assertNotSame(empty, empty.add(ext));
            assertNotSame(empty, empty.addAll(set));

            assertSame(empty, empty.remove(ext));
            assertSame(empty, empty.removeAll(set));
        }
    }

    @Test
    public void testOneElement() {
        for (val ext : this.allExtensions) {
            val set = GLExtensionSet.empty().add(ext);

            assertEquals(1, set.size());
            assertFalse(set.isEmpty());
            assertNotEquals(GLExtensionSet.empty(), set);
            assertEquals('[' + ext.name() + ']', set.toString());

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
        for (val ext1 : this.allExtensions) {
            val set1 = GLExtensionSet.empty().add(ext1);

            for (val ext2 : this.allExtensions) {
                if (ext1 != ext2) {
                    val set2 = set1.add(ext2);

                    assertEquals(2, set2.size());
                    assertFalse(set2.isEmpty());
                    assertNotEquals(GLExtensionSet.empty(), set2);
                    assertNotEquals(set1, set2);

                    val sortedArr = new GLExtension[]{ ext1, ext2 };
                    Arrays.sort(sortedArr);

                    assertEquals('[' + sortedArr[0].name() + ", " + sortedArr[1].name() + ']', set2.toString());

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

    @Test
    public void testOf() {
        assertEquals(GLExtensionSet.empty(), GLExtensionSet.of());
        assertEquals(GLExtensionSet.empty(), GLExtensionSet.of(new GLExtension[0]));

        for (val ext : this.allExtensions) {
            val expected = GLExtensionSet.empty().add(ext);
            assertEquals(expected, GLExtensionSet.of(ext));
            assertEquals(expected, GLExtensionSet.of(new GLExtension[]{ext}));
        }

        for (val ext1 : this.allExtensions) {
            for (val ext2 : this.allExtensions) {
                val expected = GLExtensionSet.empty().add(ext1).add(ext2);
                assertEquals(expected, GLExtensionSet.of(ext1, ext2));
                assertEquals(expected, GLExtensionSet.of(new GLExtension[]{ ext1, ext2 }));
            }
        }
    }

    @Test
    public void testCollector() {
        assertEquals(GLExtensionSet.empty(), Stream.<GLExtension>empty().collect(GLExtensionSet.toExtensionSet()));

        for (val ext : this.allExtensions) {
            assertEquals(GLExtensionSet.of(ext), Stream.of(ext).collect(GLExtensionSet.toExtensionSet()));
        }

        for (val ext1 : this.allExtensions) {
            for (val ext2 : this.allExtensions) {
                assertEquals(GLExtensionSet.of(ext1, ext2), Stream.of(ext1, ext2).collect(GLExtensionSet.toExtensionSet()));
            }
        }

        assertEquals(GLExtensionSet.of(this.allExtensions), Stream.of(this.allExtensions).collect(GLExtensionSet.toExtensionSet()));
    }

    @Test
    public void testAddAllArray() {
        assertEquals(GLExtensionSet.empty(), GLExtensionSet.empty().addAll(new GLExtension[0]));

        for (val ext : this.allExtensions) {
            assertEquals(GLExtensionSet.empty().add(ext), GLExtensionSet.empty().addAll(new GLExtension[]{ ext }));
        }

        for (val ext1 : this.allExtensions) {
            for (val ext2 : this.allExtensions) {
                val expected = GLExtensionSet.empty().add(ext1).add(ext2);
                assertEquals(expected, GLExtensionSet.empty().addAll(new GLExtension[]{ ext1, ext2 }));
                assertEquals(expected, GLExtensionSet.empty().add(ext1).addAll(new GLExtension[]{ ext2 }));
            }
        }
    }

    @Test
    public void testRemoveAllArray() {
        val all = Stream.of(this.allExtensions).collect(GLExtensionSet.toExtensionSet());

        assertEquals(all, all.removeAll(new GLExtension[0]));

        for (val ext : this.allExtensions) {
            assertEquals(all.remove(ext), all.removeAll(new GLExtension[]{ ext }));
        }

        for (val ext1 : this.allExtensions) {
            for (val ext2 : this.allExtensions) {
                val expected = all.remove(ext1).remove(ext2);
                assertEquals(expected, all.removeAll(new GLExtension[]{ ext1, ext2 }));
                assertEquals(expected, all.remove(ext1).removeAll(new GLExtension[]{ ext2 }));
            }
        }
    }
}
