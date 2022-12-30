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
 */

package net.daporkchop.fp2.core.mode.voxel;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;
import net.daporkchop.fp2.core.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.core.mode.api.IFarPosCodec;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.common.AbstractFarRenderMode;
import net.daporkchop.fp2.core.mode.voxel.ctx.VoxelClientContext;
import net.daporkchop.fp2.core.mode.voxel.ctx.VoxelServerContext;
import net.daporkchop.fp2.core.mode.voxel.server.gen.exact.DefaultExactVoxelGenerator;
import net.daporkchop.fp2.core.mode.voxel.server.scale.VoxelScalerIntersection;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.math.MathUtil;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.mode.voxel.VoxelConstants.*;

/**
 * Implementation of {@link IFarRenderMode} for the voxel rendering mode.
 *
 * @author DaPorkchop_
 */
public class VoxelRenderMode extends AbstractFarRenderMode<VoxelPos, VoxelTile> {
    public VoxelRenderMode() {
        super(STORAGE_VERSION, VMAX_LODS, VT_SHIFT);
    }

    @Override
    protected AbstractExactGeneratorCreationEvent exactGeneratorCreationEvent(@NonNull IFarLevelServer level, @NonNull IFarTileProvider<VoxelPos, VoxelTile> provider) {
        return new AbstractExactGeneratorCreationEvent(level, provider) {};
    }

    @Override
    protected IFarGeneratorExact<VoxelPos, VoxelTile> defaultExactGenerator(@NonNull IFarLevelServer level, @NonNull IFarTileProvider<VoxelPos, VoxelTile> provider) {
        return new DefaultExactVoxelGenerator(level, provider);
    }

    @Override
    protected AbstractRoughGeneratorCreationEvent roughGeneratorCreationEvent(@NonNull IFarLevelServer level, @NonNull IFarTileProvider<VoxelPos, VoxelTile> provider) {
        return new AbstractRoughGeneratorCreationEvent(level, provider) {};
    }

    @Override
    protected Optional<IFarGeneratorRough<VoxelPos, VoxelTile>> defaultRoughGenerator(@NonNull IFarLevelServer level, @NonNull IFarTileProvider<VoxelPos, VoxelTile> provider) {
        if (level.roughBlockLevelHolder().isPresent()) {
            throw new UnsupportedOperationException(); //TODO
        }

        return Optional.empty();
    }

    @Override
    protected AbstractTileProviderCreationEvent tileProviderCreationEvent(@NonNull IFarLevelServer world) {
        return new AbstractTileProviderCreationEvent(world) {};
    }

    @Override
    protected VoxelTile newTile() {
        return new VoxelTile();
    }

    @Override
    public IFarScaler<VoxelPos, VoxelTile> scaler(@NonNull IFarLevelServer level, @NonNull IFarTileProvider<VoxelPos, VoxelTile> provider) {
        return new VoxelScalerIntersection(level, provider);
    }

    @Override
    public IFarServerContext<VoxelPos, VoxelTile> serverContext(@NonNull IFarPlayerServer player, @NonNull IFarLevelServer level, @NonNull FP2Config config) {
        return new VoxelServerContext(player, level, config, this);
    }

    @Override
    public IFarClientContext<VoxelPos, VoxelTile> clientContext(@NonNull IFarLevelClient level, @NonNull FP2Config config) {
        return new VoxelClientContext(level, config, this);
    }

    @Override
    public IFarDirectPosAccess<VoxelPos> directPosAccess() {
        return VoxelDirectPosAccess.INSTANCE;
    }

    @Override
    public IFarPosCodec<VoxelPos> posCodec() {
        return VoxelPosCodec.INSTANCE;
    }

    @Override
    public IVariableSizeRecyclingCodec<VoxelTile> tileCodec() {
        return VoxelTile.CODEC;
    }

    @Override
    public IFarCoordLimits<VoxelPos> tileCoordLimits(@NonNull IntAxisAlignedBB blockCoordLimits) {
        return new VoxelCoordLimits(
                blockCoordLimits.minX(), blockCoordLimits.minY(), blockCoordLimits.minZ(),
                blockCoordLimits.maxX(), blockCoordLimits.maxY(), blockCoordLimits.maxZ());
    }

    @Override
    public VoxelPos readPos(@NonNull ByteBuf buf) {
        return new VoxelPos(buf);
    }

    @Override
    public VoxelPos readPos(@NonNull DataIn in) throws IOException {
        int level = in.readUnsignedByte();

        int interleavedHigh = in.readInt();
        long interleavedLow = in.readLong();
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);
        return new VoxelPos(level, x, y, z);
    }

    @Override
    @SneakyThrows(IOException.class)
    public VoxelPos readPos(@NonNull byte[] arr) {
        return this.readPos(DataIn.wrap(ByteBuffer.wrap(arr)));
    }

    @Override
    public void writePos(@NonNull DataOut out, @NonNull VoxelPos pos) throws IOException {
        out.writeByte(pos.level());
        out.writeInt(MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()));
        out.writeLong(MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    @Override
    @SneakyThrows(IOException.class)
    public byte[] writePos(@NonNull VoxelPos pos) {
        byte[] arr = new byte[BYTE_SIZE + INT_SIZE + LONG_SIZE];
        this.writePos(DataOut.wrap(ByteBuffer.wrap(arr)), pos);
        return arr;
    }

    @Override
    public VoxelPos[] posArray(int length) {
        return new VoxelPos[length];
    }

    @Override
    public VoxelTile[] tileArray(int length) {
        return new VoxelTile[length];
    }
}
