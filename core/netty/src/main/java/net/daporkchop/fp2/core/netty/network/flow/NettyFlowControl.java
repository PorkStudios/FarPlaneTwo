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

package net.daporkchop.fp2.core.netty.network.flow;

import io.netty.channel.Channel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.daporkchop.fp2.core.network.flow.FlowControl;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class NettyFlowControl {
    public static FlowControl createFlowControlFor(Channel channel) {
        if (channel instanceof LocalChannel) { //don't bother with flow control for local channels (singleplayer), the data isn't even leaving the current process
            return FlowControl.none();
        } else if (Epoll.isAvailable() && channel instanceof EpollSocketChannel) {
            return epoll((EpollSocketChannel) channel);
        } else {
            fp2().log().warn("Flow control not implemented for %s (%s), using fallback implementation!", channel.getClass().getName(), channel.remoteAddress());
            return generic(channel);
        }
    }

    private static FlowControl epoll(EpollSocketChannel channel) {
        fp2().log().info("Channel %s initial TCP_NOTSENT_LOWAT: %s", channel.remoteAddress(), channel.config().getTcpNotSentLowAt());
        channel.config().setTcpNotSentLowAt(16L << 10); //16KiB: https://blog.cloudflare.com/http-2-prioritization-with-nginx/

        return generic(channel);
    }

    private static FlowControl generic(Channel channel) {
        return bytes -> {
            val outboundBuffer = channel.unsafe().outboundBuffer();
            return outboundBuffer != null && outboundBuffer.totalPendingWriteBytes() == 0L;
        };
    }
}
