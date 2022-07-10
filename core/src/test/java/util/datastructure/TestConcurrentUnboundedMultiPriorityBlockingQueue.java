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

package util.datastructure;

import net.daporkchop.fp2.core.util.datastructure.ConcurrentUnboundedMultiPriorityBlockingQueue;
import net.daporkchop.lib.common.util.PorkUtil;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;

/**
 * @author DaPorkchop_
 */
public class TestConcurrentUnboundedMultiPriorityBlockingQueue {
    @Test
    public void testOrdering() throws InterruptedException {
        /*
         * As printing will likely take longer than adding a single string to the queue, this should print out something along the lines of:
         *
         * jeff1
         * asdf
         * jeff2
         * jeff3
         * jeff4
         * jeff5
         * zzzz
         */

        BlockingQueue<String> queue = new ConcurrentUnboundedMultiPriorityBlockingQueue<>(Comparator.comparingInt(str -> str.charAt(0)));

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String str = queue.take();
                    System.out.println(str);
                    if ("zzzz".equals(str)) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                //swallow exception and exit quietly
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        thread.start();

        PorkUtil.sleep(1000L);
        queue.add("jeff1");
        queue.add("jeff2");
        queue.add("jeff3");
        queue.add("jeff4");
        queue.add("jeff5");
        queue.add("asdf");
        queue.add("zzzz");

        thread.join();
    }

    @Test
    public void testLess() throws InterruptedException {
        /*
         * This should print exactly the following:
         *
         * asdf
         * ffff
         */

        ConcurrentUnboundedMultiPriorityBlockingQueue<String> queue = new ConcurrentUnboundedMultiPriorityBlockingQueue<>(Comparator.comparingInt(str -> str.charAt(0)));

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String str = queue.takeLess("gggg");
                    System.out.println(str);
                    if ("ffff".equals(str)) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                //swallow exception and exit quietly
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        thread.start();

        queue.add("jeff1");
        queue.add("jeff2");
        queue.add("jeff3");
        queue.add("jeff4");
        queue.add("jeff5");
        queue.add("asdf");
        queue.add("g");
        queue.add("ffff");

        thread.join();
    }
}
