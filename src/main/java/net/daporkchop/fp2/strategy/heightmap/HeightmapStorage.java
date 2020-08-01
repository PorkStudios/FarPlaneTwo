/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.strategy.heightmap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.base.server.AbstractFarStorage;
import net.daporkchop.fp2.strategy.common.server.IFarStorage;
import net.daporkchop.ldbjni.LevelDB;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.primitive.map.IntObjMap;
import net.daporkchop.lib.primitive.map.concurrent.IntObjConcurrentHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.IOException;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapStorage extends AbstractFarStorage<HeightmapPos, HeightmapPiece> {
    public HeightmapStorage(@NonNull WorldServer world) {
        super(world);
    }

    @Override
    public RenderMode mode() {
        return RenderMode.HEIGHTMAP;
    }
}
