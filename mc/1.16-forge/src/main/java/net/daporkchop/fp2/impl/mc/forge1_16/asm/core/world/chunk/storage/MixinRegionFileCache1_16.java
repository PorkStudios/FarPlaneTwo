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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.world.chunk.storage;

import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.chunk.storage.IMixinRegionFileCache1_16;
import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@Mixin(RegionFileCache.class)
public abstract class MixinRegionFileCache1_16 implements IMixinRegionFileCache1_16 {
    @Shadow
    protected abstract RegionFile getRegionFile(ChunkPos par1) throws IOException;

    @Shadow
    @Final
    private File folder;

    @Override
    public boolean fp2_RegionFileCache_hasChunk(ChunkPos pos) throws IOException {
        return this.getRegionFile(pos).doesChunkExist(pos); //this is technically slower than hasChunk(), but more accurate
    }

    @Override
    public Stream<Vec2i> fp2_RegionFileCache_listRegions() throws IOException {
        //see WorldOptimizer#getAllChunkPos

        return Files.list(this.folder.toPath())
                .map(path -> path.getFileName().toString())
                .map(Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$")::matcher)
                .filter(Matcher::matches)
                .map(matcher -> Vec2i.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
    }

    @Override
    public List<ChunkPos> fp2_RegionFileCache_listChunksInRegion(Vec2i regionPos) throws IOException {
        RegionFile region = this.getRegionFile(new ChunkPos(regionPos.x() << 5, regionPos.y() << 5));
        List<ChunkPos> positions = new ArrayList<>();
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                ChunkPos chunkPos = new ChunkPos((regionPos.x() << 5) + dx, (regionPos.y() << 5) + dz);
                if (region.doesChunkExist(chunkPos)) {
                    positions.add(chunkPos);
                }
            }
        }
        return positions;
    }
}
