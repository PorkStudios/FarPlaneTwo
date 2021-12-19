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

package net.daporkchop.fp2.util;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomTerrainGenerator;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Various constants and helper methods used throughout the mod.
 *
 * @author DaPorkchop_
 */
@UtilityClass
@Deprecated
public class Constants {
    public static final int GTH_SHIFT = 2; //generation tile shift (horizontal)
    public static final int GTH_MASK = (1 << GTH_SHIFT) - 1;
    public static final int GTH_SIZE = 1 << GTH_SHIFT; //generation tile size

    public static final int GTV_SHIFT = GTH_SHIFT + 1; //generation tile shift (vertical)
    public static final int GTV_MASK = (1 << GTV_SHIFT) - 1;
    public static final int GTV_SIZE = 1 << GTV_SHIFT;

    public static final boolean FP2_TEST = Boolean.parseBoolean(System.getProperty("fp2.test", "false"));

    public static Logger FP2_LOG = new SimpleLogger("[fp2 bootstrap]", Level.INFO, true, false, true, false, "[yyyy/MM/dd HH:mm:ss:SSS]", null, new PropertiesUtil("log4j2.simplelog.properties"), System.out);

    public static final boolean CC = !FP2_TEST && Loader.isModLoaded("cubicchunks");
    public static final boolean CWG = !FP2_TEST && Loader.isModLoaded("cubicgen");

    @SideOnly(Side.CLIENT)
    public static Minecraft MC;

    static {
        if (!FP2_TEST && fp2().hasClient()) { //initialize client-only fields here to prevent errors
            MC = Minecraft.getMinecraft();
        }
    }

    /**
     * Checks whether or not the given {@link World} is a Cubic Chunks world.
     *
     * @param world the {@link World} to check
     * @return whether or not the given {@link World} is a Cubic Chunks world
     */
    public static boolean isCubicWorld(@NonNull World world) {
        return CC && world instanceof ICubicWorld && ((ICubicWorld) world).isCubicWorld();
    }

    /**
     * Checks whether or not the given {@link WorldServer} uses Cubic World Gen.
     *
     * @param world the {@link WorldServer} to check
     * @return whether or not the given {@link WorldServer} uses Cubic World Gen
     */
    public static boolean isCwgWorld(@NonNull WorldServer world) {
        return CWG && getTerrainGenerator(world) instanceof CustomTerrainGenerator;
    }

    /**
     * Gets the terrain generator for the given {@link WorldServer}.
     *
     * @param world the {@link WorldServer} to get the terrain generator for
     * @return the terrain generator instance. May be an arbitrary type (e.g. {@link IChunkGenerator} or {@link ICubeGenerator})
     */
    public static Object getTerrainGenerator(@NonNull WorldServer world) {
        return isCubicWorld(world)
                ? ((ICubicWorldServer) world).getCubeGenerator() //this is a Cubic Chunks world, so we want to use the cube generator
                : world.getChunkProvider().chunkGenerator;
    }

    /**
     * Gets the coordinate limits for the given world.
     *
     * @param world the {@link WorldServer} to get the Y coordinate limits for
     * @return the coordinate limits
     */
    public static IntAxisAlignedBB getBounds(@NonNull World world) {
        int minY = 0;
        int maxY = world.getHeight() - 1;

        if (isCubicWorld(world)) { //this is a cubic chunks world, use cubic chunks API to get the real world height limits
            minY = ((ICubicWorld) world).getMinHeight();
            maxY = ((ICubicWorld) world).getMaxHeight() - 1;
        }

        final int HORIZONTAL_LIMIT = 30_000_000; //TODO: hard-coding this is probably a bad idea, but there don't seem to be any variables or methods i can use to get it
        return new IntAxisAlignedBB(-HORIZONTAL_LIMIT, minY, -HORIZONTAL_LIMIT, HORIZONTAL_LIMIT, maxY, HORIZONTAL_LIMIT);
    }

    private static Class<?> toClass(@NonNull Object in) {
        if (in instanceof Class) {
            return uncheckedCast(in);
        } else if (in instanceof String) {
            return PorkUtil.classForName((String) in);
        } else {
            throw new IllegalArgumentException(PorkUtil.className(in));
        }
    }

    public static MethodHandle staticHandle(@NonNull Object clazz, @NonNull Object returnType, @NonNull String name, @NonNull Object... params) {
        try {
            MethodType type = MethodType.methodType(toClass(returnType), Arrays.stream(params).map(Constants::toClass).toArray(Class[]::new));
            return MethodHandles.lookup().findStatic(toClass(clazz), name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
