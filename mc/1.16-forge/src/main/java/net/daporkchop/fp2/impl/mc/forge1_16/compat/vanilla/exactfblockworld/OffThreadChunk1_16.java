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

package net.daporkchop.fp2.impl.mc.forge1_16.compat.vanilla.exactfblockworld;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.lib.common.util.PArrays;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ChunkSerializer;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorld;

import java.util.Arrays;
import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A container around all the data in a chunk which is relevant to the exact {@link FBlockWorld} implementation.
 * <p>
 * Using my own class for this serves two purposes:<br>
 * <ul>
 *     <li>it allows me to include lighting data in the chunk, when it would normally be offloaded to {@link WorldLightManager} and only accessible from the server thread</li>
 *     <li>it allows me to avoid having to make invasive modifications to {@link ChunkSerializer} in order to skip all the many stages which modify the server world's state</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
public class OffThreadChunk1_16 {
    private static final NibbleArray NIBBLE_ARRAY_0 = new NibbleArray();
    private static final NibbleArray NIBBLE_ARRAY_15 = new NibbleArray(PArrays.filled(2048, (byte) 0xFF));

    private static final int SECTION_INDEX_MIN = -1; //inclusive
    private static final int SECTION_INDEX_MAX = 17; //exclusive
    private static final int SECTION_INDEX_COUNT = SECTION_INDEX_MAX - SECTION_INDEX_MIN;

    private static int section2index(int section) {
        return section - SECTION_INDEX_MIN;
    }

    private static boolean isSectionValid(int section) {
        return section >= SECTION_INDEX_MIN && section < SECTION_INDEX_MAX;
    }

    @Getter
    private final BiomeContainer biomes;
    private final ChunkSection[] sections = new ChunkSection[SECTION_INDEX_COUNT];
    private final NibbleArray[] blockLight = new NibbleArray[SECTION_INDEX_COUNT];
    private final NibbleArray[] skyLight;

    private final int defaultSkyLightNegativeY = 0;
    private final int defaultSkyLightPositiveY;

    @Getter
    private final int x;
    @Getter
    private final int z;

    public OffThreadChunk1_16(@NonNull ServerWorld world, @NonNull CompoundNBT rootTag) {
        //heavily based on ChunkSerializer.load

        CompoundNBT levelTag = rootTag.getCompound("Level");
        this.x = levelTag.getInt("xPos");
        this.z = levelTag.getInt("zPos");

        this.biomes = new BiomeContainer(
                world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY),
                new ChunkPos(this.x, this.z),
                world.getChunkSource().getGenerator().getBiomeSource(),
                levelTag.contains("Biomes", 11) ? levelTag.getIntArray("Biomes") : null);

        checkState(levelTag.getBoolean("isLightOn"), "isLightOn is false!");
        boolean hasSkyLight = world.dimensionType().hasSkyLight();

        Arrays.fill(this.blockLight, NIBBLE_ARRAY_0);

        if (hasSkyLight) {
            this.defaultSkyLightPositiveY = 15;
            this.skyLight = new NibbleArray[SECTION_INDEX_COUNT];
        } else {
            this.defaultSkyLightPositiveY = 0; //sky light disabled, assume level 0 everywhere
            this.skyLight = null;
        }

        levelTag.getList("Sections", 10).forEach(_sectionTag -> {
            CompoundNBT sectionTag = (CompoundNBT) _sectionTag;
            int sectionY = sectionTag.getByte("Y");

            //chunk sections
            if (sectionTag.contains("Palette", 9) && sectionTag.contains("BlockStates", 12)) {
                ChunkSection section = new ChunkSection(sectionY << 4);
                section.getStates().read(sectionTag.getList("Palette", 10), sectionTag.getLongArray("BlockStates"));
                section.recalcBlockCounts();
                if (!section.isEmpty()) {
                    this.sections[section2index(sectionY)] = section;
                }
            }

            //lighting
            if (sectionTag.contains("BlockLight", 7)) {
                this.blockLight[section2index(sectionY)] = new NibbleArray(sectionTag.getByteArray("BlockLight"));
            }

            if (hasSkyLight && sectionTag.contains("SkyLight", 7)) {
                this.skyLight[section2index(sectionY)] = new NibbleArray(sectionTag.getByteArray("SkyLight"));
            }
        });

        //sky light exists, fill in unset elements
        if (this.skyLight != null) {
            //determine the highest section index which actually contains a chunk section to determine where the light level cutoff point should be.
            //  i'm not sure if this is entirely correct, but it seems to work well enough.
            int highestSectionIndex = IntStream.range(0, this.sections.length)
                    .filter(i -> this.sections[i] != null)
                    .max().orElse(0);

            for (int sectionIndex = 0; sectionIndex < this.sections.length; sectionIndex++) {
                if (this.skyLight[sectionIndex] == null) {
                    this.skyLight[sectionIndex] = sectionIndex <= highestSectionIndex ? NIBBLE_ARRAY_0 : NIBBLE_ARRAY_15;
                }
            }
        }
    }

    public long getPositionAsLong() {
        return ChunkPos.asLong(this.x, this.z);
    }

    public BlockState getBlockState(int x, int y, int z) {
        int sectionY = y >> 4;
        if (isSectionValid(sectionY)) { //y coordinate is in range
            ChunkSection section = this.sections[section2index(sectionY)];
            if (section != null) {
                return section.getBlockState(x & 0xF, y & 0xF, z & 0xF);
            }
        }

        //invalid Y coordinate, assume everything is air
        return Blocks.AIR.defaultBlockState();
    }

    public int getBlockLight(int x, int y, int z) {
        int sectionY = y >> 4;
        if (isSectionValid(sectionY)) { //y coordinate is in range
            return this.blockLight[section2index(sectionY)].get(x & 0xF, y & 0xF, z & 0xF);
        }

        //invalid Y coordinate, assume everything is level 0
        return 0;
    }

    public int getSkyLight(int x, int y, int z) {
        int sectionY = y >> 4;
        if (isSectionValid(sectionY)) { //y coordinate is in range
            return this.skyLight[section2index(sectionY)].get(x & 0xF, y & 0xF, z & 0xF);
        }

        //invalid Y coordinate or sky light is disabled, fall back to default sky light levels
        return sectionY < 0 ? this.defaultSkyLightNegativeY : this.defaultSkyLightPositiveY;
    }
}
