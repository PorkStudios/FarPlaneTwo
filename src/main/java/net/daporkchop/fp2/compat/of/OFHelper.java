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

package net.daporkchop.fp2.compat.of;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * Optifine compatibility code.
 *
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class OFHelper {
    //TODO: make intellij stop rearranging this class somehow

    public static final boolean OF;
    public static final String OF_VERSION;

    static { //copy/pasted from io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksMixinLoader
        String ofVersion = null;
        try {
            Class<?> optifineInstallerClass = Class.forName("optifine.Installer");
            Method getVersionHandler = optifineInstallerClass.getMethod("getOptiFineVersion");
            ofVersion = ((String) getVersionHandler.invoke(null));
            ofVersion = ofVersion.replace("_pre", "");
            ofVersion = ofVersion.substring(ofVersion.length() - 2);

            Constants.FP2_LOG.info("Detected Optifine version: {}", ofVersion);
        } catch (ClassNotFoundException e) {
            Constants.FP2_LOG.info("No Optifine detected");
        } catch (Exception e) {
            ofVersion = "E1";
            Constants.FP2_LOG.error("Optifine detected, but could not detect version. It may not work. Assuming Optifine E1...", e);
        } finally {
            OF_VERSION = ofVersion;
            OF = ofVersion != null;
        }
    }

    public static final int OF_DEFAULT = !OF ? -1 : PUnsafe.pork_getStaticField(GameSettings.class, "DEFAULT").getInt();
    public static final int OF_FAST = !OF ? -1 : PUnsafe.pork_getStaticField(GameSettings.class, "FAST").getInt();
    public static final int OF_FANCY = !OF ? -1 : PUnsafe.pork_getStaticField(GameSettings.class, "FANCY").getInt();
    public static final int OF_OFF = !OF ? -1 : PUnsafe.pork_getStaticField(GameSettings.class, "OFF").getInt();
    public static final int OF_SMART = !OF ? -1 : PUnsafe.pork_getStaticField(GameSettings.class, "SMART").getInt();
    public static final int OF_ANIM_ON = !OF ? -1 : PUnsafe.pork_getStaticField(GameSettings.class, "ANIM_ON").getInt();
    public static final int OF_ANIM_GENERATED = !OF ? -1 : PUnsafe.pork_getStaticField(GameSettings.class, "ANIM_GENERATED").getInt();
    public static final int OF_ANIM_OFF = !OF ? -1 : PUnsafe.pork_getStaticField(GameSettings.class, "ANIM_OFF").getInt();

    public static final long OF_AALEVEL_OFFSET = !OF ? -1L : PUnsafe.pork_getOffset(GameSettings.class, "ofAaLevel");
    public static final long OF_FASTRENDER_OFFSET = !OF ? -1L : PUnsafe.pork_getOffset(GameSettings.class, "ofFastRender");
    public static final long OF_FOGTYPE_OFFSET = !OF ? -1L : PUnsafe.pork_getOffset(GameSettings.class, "ofFogType");
    public static final long OF_BETTERGRASS_OFFSET = !OF ? -1L : PUnsafe.pork_getOffset(GameSettings.class, "ofBetterGrass");

    public static final int OF_SHADERS_ENTITYATTRIB = 10;
    public static final int OF_SHADERS_MIDTEXCOORDATTRIB = 11;
    public static final int OF_SHADERS_TANGENTATTRIB = 12;

    private static final MethodHandle OF_CONFIG_ISSHADERS = !OF ? null : Constants.staticHandle("Config", boolean.class, "isShaders");

    private static final MethodHandle OF_SHADERSRENDER_BEGINTERRAINSOLID = !OF ? null : Constants.staticHandle("net.optifine.shaders.ShadersRender", void.class, "beginTerrainSolid");
    private static final MethodHandle OF_SHADERSRENDER_BEGINTERRAINCUTOUTMIPPED = !OF ? null : Constants.staticHandle("net.optifine.shaders.ShadersRender", void.class, "beginTerrainCutoutMipped");
    private static final MethodHandle OF_SHADERSRENDER_BEGINTERRAINCUTOUT = !OF ? null : Constants.staticHandle("net.optifine.shaders.ShadersRender", void.class, "beginTerrainCutout");
    private static final MethodHandle OF_SHADERSRENDER_ENDTERRAIN = !OF ? null : Constants.staticHandle("net.optifine.shaders.ShadersRender", void.class, "endTerrain");
    private static final MethodHandle OF_SHADERSRENDER_PRERENDERCHUNKLAYER = !OF ? null : Constants.staticHandle("net.optifine.shaders.ShadersRender", void.class, "preRenderChunkLayer", BlockRenderLayer.class);
    private static final MethodHandle OF_SHADERSRENDER_POSTRENDERCHUNKLAYER = !OF ? null : Constants.staticHandle("net.optifine.shaders.ShadersRender", void.class, "postRenderChunkLayer", BlockRenderLayer.class);
    private static final MethodHandle OF_SHADERS_BEGINWATER = !OF ? null : Constants.staticHandle("net.optifine.shaders.Shaders", void.class, "beginWater");
    private static final MethodHandle OF_SHADERS_ENDWATER = !OF ? null : Constants.staticHandle("net.optifine.shaders.Shaders", void.class, "endWater");

    public static final String OF_DEFINE_SHADERS = "OPTIFINE_SHADERS";

    @SneakyThrows(Throwable.class)
    public static boolean of_Config_isShaders() {
        return OF && (boolean) OF_CONFIG_ISSHADERS.invokeExact();
    }

    @SneakyThrows(Throwable.class)
    public static void of_ShadersRender_beginTerrainSolid() {
        OF_SHADERSRENDER_BEGINTERRAINSOLID.invokeExact();
    }

    @SneakyThrows(Throwable.class)
    public static void of_ShadersRender_beginTerrainCutoutMipped() {
        OF_SHADERSRENDER_BEGINTERRAINCUTOUTMIPPED.invokeExact();
    }

    @SneakyThrows(Throwable.class)
    public static void of_ShadersRender_beginTerrainCutout() {
        OF_SHADERSRENDER_BEGINTERRAINCUTOUT.invokeExact();
    }

    @SneakyThrows(Throwable.class)
    public static void of_ShadersRender_endTerrain() {
        OF_SHADERSRENDER_ENDTERRAIN.invokeExact();
    }

    @SneakyThrows(Throwable.class)
    public static void of_ShadersRender_preRenderChunkLayer(BlockRenderLayer layer) {
        OF_SHADERSRENDER_PRERENDERCHUNKLAYER.invokeExact(layer);
    }

    @SneakyThrows(Throwable.class)
    public static void of_ShadersRender_postRenderChunkLayer(BlockRenderLayer layer) {
        OF_SHADERSRENDER_POSTRENDERCHUNKLAYER.invokeExact(layer);
    }

    @SneakyThrows(Throwable.class)
    public static void of_Shaders_beginWater() {
        OF_SHADERS_BEGINWATER.invokeExact();
    }

    @SneakyThrows(Throwable.class)
    public static void of_Shaders_endWater() {
        OF_SHADERS_ENDWATER.invokeExact();
    }
}
