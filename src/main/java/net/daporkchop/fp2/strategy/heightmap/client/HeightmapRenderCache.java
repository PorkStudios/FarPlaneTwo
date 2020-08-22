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

package net.daporkchop.fp2.strategy.heightmap.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.base.client.AbstractFarRenderCache;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.FixedSizeAllocator;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.open.LongObjOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;

import java.nio.ByteBuffer;

import static net.daporkchop.fp2.strategy.heightmap.client.HeightmapRenderHelper.*;
import static net.daporkchop.fp2.strategy.heightmap.client.HeightmapRenderer.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderCache extends AbstractFarRenderCache<HeightmapPos, HeightmapPiece, HeightmapRenderTile, HeightmapRenderIndex> {
    public HeightmapRenderCache(@NonNull HeightmapRenderer renderer) {
        super(renderer, new HeightmapRenderIndex(), HEIGHTMAP_RENDER_SIZE);
    }

    @Override
    protected HeightmapRenderTile createTile(HeightmapRenderTile parent, @NonNull HeightmapPos pos) {
        return new HeightmapRenderTile(this, parent, pos);
    }

    public void tileAdded(@NonNull HeightmapRenderTile tile) {
        tile.neighbors[0] = tile;
        tile.neighbors[1] = this.getTile(tile.x, tile.z + 1, tile.level);
        tile.neighbors[2] = this.getTile(tile.x + 1, tile.z, tile.level);
        tile.neighbors[3] = this.getTile(tile.x + 1, tile.z + 1, tile.level);

        HeightmapRenderTile t = this.getTile(tile.x, tile.z - 1, tile.level);
        if (t != null) {
            t.neighbors[1] = tile;
        }
        t = this.getTile(tile.x - 1, tile.z, tile.level);
        if (t != null) {
            t.neighbors[2] = tile;
        }
        t = this.getTile(tile.x - 1, tile.z - 1, tile.level);
        if (t != null) {
            t.neighbors[3] = tile;
        }
    }

    public void tileRemoved(@NonNull HeightmapRenderTile tile) {
        HeightmapRenderTile t = this.getTile(tile.x, tile.z - 1, tile.level);
        if (t != null) {
            t.neighbors[1] = null;
        }
        t = this.getTile(tile.x - 1, tile.z, tile.level);
        if (t != null) {
            t.neighbors[2] = null;
        }
        t = this.getTile(tile.x - 1, tile.z - 1, tile.level);
        if (t != null) {
            t.neighbors[3] = null;
        }
    }
}
