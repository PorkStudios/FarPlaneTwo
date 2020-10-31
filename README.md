# FarPlaneTwo

[![Build Status](https://jenkins.daporkchop.net/job/PorkStudios/job/FarPlaneTwo/job/master/badge/icon)](https://jenkins.daporkchop.net/job/PorkStudios/job/FarPlaneTwo/)
[![Discord](https://img.shields.io/discord/428813657816956929.svg)](https://discord.gg/FrBHHCk)

Very much a work-in-progress, if you do decide to download this don't expect stuff to work correctly because it probably won't.

This is a Minecraft mod which implements a Level-of-Detail (LoD) terrain renderer into the game.

The name is in reference to [Foghrye4's CWGFarPlaneView](https://www.curseforge.com/minecraft/mc-mods/cwg-far-plane-view), which is an addon for CubicWorldGen (which, in turn, is an addon for Cubic Chunks) which renders a plane at sea level with block and biome colors, and served as the original inspiration for me to make this.

My end goal is to achieve the following:
- be able to use render distances of *at least* 100 thousand blocks
- cause at most a 25% decrease in performance when using my i5-2520M's integrated graphics
- be compatible with [Cubic Chunks](https://github.com/OpenCubicChunks/CubicChunks)
- be as compatible as reasonably possible with other mods

I have exactly zero concern with doing stuff that is unsafe: for instance, [the client-side render tree](https://github.com/PorkStudios/FarPlaneTwo/blob/master/src/main/java/net/daporkchop/fp2/mode/common/client/AbstractFarRenderTree.java), which the renderer traverses when deciding which tiles to render each frame, is implemented entirely using off-heap memory.

At some point (once the internals of the mod are more polished) I'll probably write up a big section here describing exactly how a lot of the stuff works.
