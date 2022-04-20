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

package net.daporkchop.fp2.impl.mc.forge1_12_2.server.world.level;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;
import net.daporkchop.fp2.core.server.world.level.AbstractLevelServer;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.server.ATMinecraftServer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.TerrainGeneratorInfo1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.level.IFarLevel1_12;
import net.minecraft.world.WorldServer;

import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
@Getter
public class FarLevelServer1_12 extends AbstractLevelServer<FP2Forge1_12_2, WorldServer> implements IFarLevel1_12 {
    public FarLevelServer1_12(@NonNull FP2Forge1_12_2 fp2, @NonNull WorldServer implWorld, @NonNull FStorageCategory storageCategory) {
        super(fp2, implWorld, storageCategory);
    }

    @Override
    protected WorkerManager createWorkerManager() {
        return new DefaultWorkerManager(
                ((ATMinecraftServer1_12) this.implWorld().getMinecraftServer()).getServerThread(),
                ServerThreadMarkedFutureExecutor.getFor(this.implWorld().getMinecraftServer()));
    }

    @Override
    public Path levelDirectory() {
        return this.implWorld().getChunkSaveLocation().toPath();
    }

    @Override
    public TerrainGeneratorInfo terrainGeneratorInfo() {
        return new TerrainGeneratorInfo1_12_2(this.implWorld());
    }

    @Override
    public int seaLevel() {
        return this.implWorld().getSeaLevel();
    }
}
