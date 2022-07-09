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
import net.daporkchop.fp2.core.util.threading.locks.multi.StampedSignaller;
import net.daporkchop.fp2.core.util.threading.locks.multi.SyncAggregator;
import net.daporkchop.lib.common.util.PorkUtil;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;

/**
 * @author DaPorkchop_
 */
public class TestConcurrentUnboundedMultiPriorityBlockingQueue {
    @Test
    public void test() throws InterruptedException {
        BlockingQueue<String> queue = new ConcurrentUnboundedMultiPriorityBlockingQueue<>(Comparator.comparingInt(str -> str.charAt(0)));

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String str = queue.take();
                    System.out.println(str);
                    if ("exit".equals(str)) {
                        return;
                    }
                }
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
        queue.add("aeff");

        PorkUtil.sleep(1000L);
        thread.interrupt();

        thread.join();
    }

    @Test
    public void testSignaller() throws InterruptedException {
        StampedSignaller signaller = new StampedSignaller();

        long stamp = signaller.stamp();
        signaller.signalAll();
        SyncAggregator.awaitFirst(signaller.prepareAwait(stamp));

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    SyncAggregator.awaitFirst(signaller.prepareAwait());
                    System.out.println("signalled");
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        thread.start();

        for (int i = 0; i < 5; i++) {
            PorkUtil.sleep(100L);
            System.out.println("signalling");
            signaller.signalAll();
        }

        thread.interrupt();
        thread.join();
    }
}
