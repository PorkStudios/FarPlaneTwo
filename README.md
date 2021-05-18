# FarPlaneTwo

[![Build Status](https://jenkins.daporkchop.net/job/PorkStudios/job/FarPlaneTwo/job/master/badge/icon)](https://jenkins.daporkchop.net/job/PorkStudios/job/FarPlaneTwo/)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/PorkStudios/FarPlaneTwo)
![Lines of code](https://img.shields.io/tokei/lines/github/PorkStudios/FarPlaneTwo)
[![Discord](https://img.shields.io/discord/428813657816956929?color=7289DA&label=discord)](https://discord.gg/FrBHHCk)
[![patreon badge](https://img.shields.io/badge/dynamic/json?color=e64413&label=patreon&query=data.attributes.patron_count&suffix=%20patrons&url=https%3A%2F%2Fwww.patreon.com%2Fapi%2Fcampaigns%2F727078)](https://www.patreon.com/DaPorkchop_)

[Download here](https://jenkins.daporkchop.net/job/PorkStudios/job/FarPlaneTwo/job/master/)

## This mod is work-in-progress!

*If you do decide to download this, don't expect stuff to work correctly because it probably won't.*

**This mod currently supports the latest version of Forge for 1.12.2. It requires both [ForgeRocks](https://www.curseforge.com/minecraft/mc-mods/forgerocks) and [Mixin](https://www.curseforge.com/minecraft/mc-mods/mixin-0-7-0-8-compatibility) in order to launch correctly.**  
(Mixin isn't needed if you're using [Cubic Chunks](https://github.com/OpenCubicChunks/CubicChunks))

## **[Read the FAQ first!](https://github.com/PorkStudios/FarPlaneTwo/wiki/FAQ)**

This is a Minecraft mod which implements a Level-of-Detail (LoD) terrain renderer into the game.

The name is in reference to [Foghrye4's CWGFarPlaneView](https://www.curseforge.com/minecraft/mc-mods/cwg-far-plane-view), which is an addon for CubicWorldGen (which, in turn, is an addon for Cubic Chunks) which renders a plane at sea level with block and biome colors, and served as the original inspiration for me to make this.

My end goal is to achieve the following:

- be able to use render distances of *at least* 100 thousand blocks
- cause at most a 25% decrease in performance when using my i5-2520M's integrated graphics
- be compatible with [Cubic Chunks](https://github.com/OpenCubicChunks/CubicChunks)
- be as compatible as reasonably possible with other mods

I have exactly zero concern with doing stuff that is unsafe: for instance, [the client-side render tree](https://github.com/PorkStudios/FarPlaneTwo/blob/master/src/main/java/net/daporkchop/fp2/mode/common/client/FarRenderTree.java), which the renderer traverses when deciding which tiles to render each frame, is implemented entirely using off-heap memory.

At some point (once the internals of the mod are more polished) I'll probably write up a big section here describing exactly how a lot of the stuff works.
