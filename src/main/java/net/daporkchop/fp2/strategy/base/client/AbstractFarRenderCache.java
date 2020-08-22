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

package net.daporkchop.fp2.strategy.base.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.FixedSizeAllocator;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import net.minecraft.client.renderer.culling.ICamera;

import java.nio.ByteBuffer;
import java.util.Map;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractFarRenderCache<POS extends IFarPos, P extends IFarPiece<POS>, T extends AbstractFarRenderTile<POS, I, T>, I extends AbstractFarRenderIndex<POS, T, I>> {
    protected final AbstractFarRenderer<POS, P, T, I> renderer;

    protected final Map<POS, T> roots = new ObjObjOpenHashMap<>();

    protected final ShaderStorageBuffer dataSSBO = new ShaderStorageBuffer();
    protected final ByteBuffer zeroData;
    protected final Allocator dataAllocator;

    protected final ShaderStorageBuffer indexSSBO = new ShaderStorageBuffer();
    protected final I index;

    protected final int bakedSize;

    public AbstractFarRenderCache(@NonNull AbstractFarRenderer<POS, P, T, I> renderer, @NonNull I index, int bakedSize) {
        this.renderer = renderer;
        this.index = index;
        this.bakedSize = positive(bakedSize, "bakedSize");

        int size = glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
        LOGGER.info(PStrings.fastFormat("Max SSBO size: %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

        this.zeroData = Constants.createByteBuffer(this.bakedSize);
        this.dataAllocator = new FixedSizeAllocator(this.bakedSize, (oldSize, newSize) -> {
            LOGGER.debug("Growing data SSBO from {} to {} bytes", oldSize, newSize);

            try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                //grow SSBO
                glBufferData(GL_SHADER_STORAGE_BUFFER, newSize, GL_STATIC_DRAW);

                //re-upload data
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0L, this.zeroData);
                this.roots.forEach((l, root) -> root.forEach(tile -> {
                    if (tile.hasAddress()) {
                        glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.address, tile.renderData);
                    }
                }));
            }
        });

        //allocate zero block
        checkState(this.dataAllocator.alloc(this.bakedSize) == 0L);
    }

    public abstract T createTile(T parent, @NonNull POS pos);

    public abstract void tileAdded(@NonNull T tile);

    public abstract void tileRemoved(@NonNull T tile);

    public void addPiece(@NonNull P piece, @NonNull ByteBuf bakedData) {
        POS pos = piece.pos();
        POS rootPos = uncheckedCast(pos.upTo(this.renderer.maxLevel));
        T rootTile = this.roots.get(rootPos);
        if (rootTile == null) {
            //create root tile if absent
            this.roots.put(rootPos, rootTile = this.createTile(null, rootPos));
        }
        T tile = rootTile.findOrCreateChild(pos);
        if (!tile.hasAddress()) {
            //allocate address for tile
            tile.assignAddress(this.dataAllocator.alloc(this.bakedSize));
        }

        //TODO: set tile min- and maxY

        //copy baked data into tile and upload to GPU
        bakedData.readBytes(tile.renderData);
        tile.renderData.clear();
        try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.address, tile.renderData);
        }
    }

    public void removePiece(@NonNull POS pos) {
        POS rootPos = uncheckedCast(pos.upTo(this.renderer.maxLevel));
        T rootTile = this.roots.get(rootPos);
        if (rootTile != null) {
            T unloadedTile = rootTile.findChild(pos);
            if (unloadedTile != null && unloadedTile.hasAddress()) {
                //free address
                this.dataAllocator.free(unloadedTile.address);

                //inform tile that the address has been freed
                if (unloadedTile.dropAddress()) {
                    this.roots.remove(rootPos);
                }
                return;
            }
        }
        Constants.LOGGER.warn("Attempted to unload already non-existent piece at {}!", pos);
    }

    public T getTile(@NonNull POS pos) {
        T rootTile = this.roots.get(PorkUtil.<POS>uncheckedCast(pos.upTo(this.renderer.maxLevel)));
        return rootTile != null ? rootTile.findChild(pos) : null;
    }

    public int rebuildIndex(@NonNull Volume[] ranges, @NonNull ICamera frustum) {
        this.index.reset();
        this.roots.forEach((l, tile) -> tile.select(ranges, frustum, this.index));
        if (this.index.size == 0) {
            return 0;
        }

        try (ShaderStorageBuffer ssbo = this.indexSSBO.bind()) {
            this.index.upload(GL_SHADER_STORAGE_BUFFER);
        }
        return this.index.size;
    }

    public void bindIndex() {
        this.indexSSBO.bindSSBO(2);
        this.dataSSBO.bindSSBO(3);
    }
}
