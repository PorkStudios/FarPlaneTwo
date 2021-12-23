/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.heightmap;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;
import net.daporkchop.fp2.core.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerServer;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.common.AbstractFarRenderMode;
import net.daporkchop.fp2.core.mode.heightmap.ctx.HeightmapClientContext;
import net.daporkchop.fp2.core.mode.heightmap.ctx.HeightmapServerContext;
import net.daporkchop.fp2.core.mode.heightmap.server.scale.HeightmapScalerMinMax;
import net.daporkchop.fp2.core.util.math.MathUtil;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;

import java.io.IOException;
import java.nio.ByteBuffer;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;

/**
 * Implementation of {@link IFarRenderMode} for the heightmap rendering mode.
 *
 * @author DaPorkchop_
 */
public class HeightmapRenderMode extends AbstractFarRenderMode<HeightmapPos, HeightmapTile> {
    public HeightmapRenderMode() {
        super(STORAGE_VERSION, HMAX_LODS, HT_SHIFT);
    }

    @Override
    protected AbstractExactGeneratorCreationEvent exactGeneratorCreationEvent(@NonNull IFarWorldServer world) {
        return new AbstractExactGeneratorCreationEvent(world) {};
    }

    @Override
    protected AbstractRoughGeneratorCreationEvent roughGeneratorCreationEvent(@NonNull IFarWorldServer world) {
        return new AbstractRoughGeneratorCreationEvent(world) {};
    }

    @Override
    protected AbstractTileProviderCreationEvent tileProviderCreationEvent(@NonNull IFarWorldServer world) {
        return new AbstractTileProviderCreationEvent(world) {};
    }

    @Override
    protected HeightmapTile newTile() {
        return new HeightmapTile();
    }

    @Override
    public IFarScaler<HeightmapPos, HeightmapTile> scaler(@NonNull IFarWorldServer world) {
        return new HeightmapScalerMinMax(world);
    }

    @Override
    public IFarServerContext<HeightmapPos, HeightmapTile> serverContext(@NonNull IFarPlayerServer player, @NonNull IFarWorldServer world, @NonNull FP2Config config) {
        return new HeightmapServerContext(player, world, config, this);
    }

    @Override
    public IFarClientContext<HeightmapPos, HeightmapTile> clientContext(@NonNull IFarWorldClient world, @NonNull FP2Config config) {
        return new HeightmapClientContext(world, config, this);
    }

    @Override
    public IFarDirectPosAccess<HeightmapPos> directPosAccess() {
        return HeightmapDirectPosAccess.INSTANCE;
    }

    @Override
    public IFarCoordLimits<HeightmapPos> tileCoordLimits(@NonNull IntAxisAlignedBB blockCoordLimits) {
        return new HeightmapCoordLimits(
                blockCoordLimits.minX(), blockCoordLimits.minZ(),
                blockCoordLimits.maxX(), blockCoordLimits.maxZ());
    }

    @Override
    public HeightmapPos readPos(@NonNull ByteBuf buf) {
        return new HeightmapPos(buf);
    }

    @Override
    public HeightmapPos readPos(@NonNull DataIn in) throws IOException {
        int level = in.readUnsignedByte();

        long interleaved = in.readLong();
        int x = MathUtil.uninterleave2_0(interleaved);
        int z = MathUtil.uninterleave2_1(interleaved);
        return new HeightmapPos(level, x, z);
    }

    @Override
    @SneakyThrows(IOException.class)
    public HeightmapPos readPos(@NonNull byte[] arr) {
        return this.readPos(DataIn.wrap(ByteBuffer.wrap(arr)));
    }

    @Override
    public void writePos(@NonNull DataOut out, @NonNull HeightmapPos pos) throws IOException {
        out.writeByte(pos.level());
        out.writeLong(MathUtil.interleaveBits(pos.x(), pos.z()));
    }

    @Override
    @SneakyThrows(IOException.class)
    public byte[] writePos(@NonNull HeightmapPos pos) {
        byte[] arr = new byte[BYTE_SIZE + LONG_SIZE];
        this.writePos(DataOut.wrap(ByteBuffer.wrap(arr)), pos);
        return arr;
    }

    @Override
    public HeightmapPos[] posArray(int length) {
        return new HeightmapPos[length];
    }

    @Override
    public HeightmapTile[] tileArray(int length) {
        return new HeightmapTile[length];
    }
}
