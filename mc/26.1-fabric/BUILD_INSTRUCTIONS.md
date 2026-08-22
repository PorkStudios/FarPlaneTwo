# FarPlane 26.1 — Build Instructions

## Prerequisites

- **Java 25** (Temurin recommended): https://adoptium.net/
- **Internet connection** (Gradle downloads dependencies)

## Building

```bash
cd /home/user/farplane
./gradlew build
```

The built JAR will be at `build/libs/farplane-0.1.0.jar`.

## Installing for Testing

1. Install Minecraft 26.1.2 with Fabric Loader 0.19.3
2. Copy `build/libs/farplane-0.1.0.jar` to `.minecraft/mods/`
3. Also install Fabric API 0.155.2+26.1.2
4. Launch Minecraft

## What's Implemented (Phases 0-2)

### Phase 0: Fabric Skeleton
- `build.gradle` with Loom 1.17-SNAPSHOT, official mappings, Java 25
- `fabric.mod.json` with correct dependencies
- Split source sets (main + client)

### Phase 1: Debug Height Grid
- `VoxelTerrainRenderer` renders a ring of colored quads beyond view distance
- Loaded chunks: uses `Heightmap.Types.MOTION_BLOCKING` + biome grass color
- Unloaded chunks: placeholder noise around sea level
- Updates when player moves ≥16 blocks or view distance changes

### Phase 2: Voxel Tile Engine
- **`EngineConstants`**: All FP2 constants (T_SHIFT=4, T_VOXELS=16, edge maps, etc.)
- **`TilePos`**: Record with octree navigation (up/down), Manhattan distance, hash
- **`TileData`**: Voxel sample (x,y,z position, edges, states[3], biome, light)
- **`Tile`**: On-heap sparse 16³ storage (index[] + parallel arrays)
- **`BlockSampleSource`**: Reads loaded chunks from `ClientLevel` without triggering generation
- **`ExactVoxelGenerator`**: Port of FP2's `AbstractExactVoxelGenerator.generate()` — dual-contour edge detection
- **`VoxelBaker`**: Converts tiles to renderable quads (blocky mesh for v1)
- **`VoxelScaler`**: Simplified 8-child → 1-parent scaling
- **`VoxelTerrainRenderer`**: Generates tiles for loaded chunks, renders with `RenderTypes.debugQuads()`

## Known Limitations (v1)

1. **Synchronous generation** — tiles generate on the render thread (Phase 3 adds async)
2. **Blocky mesh** — uses cell corners, not dual-contour vertex positions (smooth mesh later)
3. **No neighbor stitching** — baker only uses primary tile, not 8 neighbors
4. **No texture atlas** — all faces are solid green (Phase 4 adds atlas UVs)
5. **No LoD scaling** — only level 0 tiles are generated (VoxelScaler exists but isn't wired)
6. **No disk cache** — tiles regenerate each session (Phase 3)
7. **No networking** — single-player only for now (Phase 3)
8. **Biome tint placeholder** — uses biome hash, not actual grass/foliage colors

## File Structure

```
src/main/java/dev/farplane/
  Farplane.java                    — ModInitializer, config loading
  config/FarplaneConfig.java       — JSON config (maxLevels, cutoffDistance, etc.)
  engine/
    EngineConstants.java           — FP2 constants
    TilePos.java                   — Tile position in octree
    TileData.java                  — Voxel sample data
    Tile.java                      — On-heap sparse 16³ storage
    BlockSampleSource.java         — Reads loaded chunks
    ExactVoxelGenerator.java       — Dual-contour edge detection
    VoxelScaler.java               — LoD scaling (8→1)

src/client/java/dev/farplane/
  client/
    FarplaneClient.java            — ClientModInitializer, event registration
    VoxelTerrainRenderer.java      — Main renderer (debug grid + voxel tiles)
  engine/
    VoxelBaker.java                — Tile → renderable quads
```

## License

Adapted MIT from FarPlaneTwo. See LICENSE and NOTICE files.
Original author: DaPorkchop_ (PorkStudios)
Original project: https://github.com/PorkStudios/FarPlaneTwo
