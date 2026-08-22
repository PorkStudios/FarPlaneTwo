# FarPlane 26.1 — Phase 5 Summary

## What's New in Phase 5

Phase 5 adds rough noise generation — the core FP2 feature that enables terrain at extreme distances.

### New Files

**Rough Generation** (`src/main/java/dev/farplane/engine/`)
- `RoughVoxelGenerator.java` — Generates terrain from noise without loading chunks
  - Samples `NoiseRouter.finalDensity()` directly
  - Uses dual contouring to find terrain surface
  - Works at any LoD level
  - No chunk loading required — terrain at 10,000+ blocks!

### Modified Files

- `AsyncTileGenerator.java` — Now uses rough generator
  - Tries exact generation first (from loaded chunks)
  - Falls back to rough generation if chunks aren't loaded
  - Higher LoD levels use rough generation directly
- `VoxelTerrainRenderer.java` — Passes Level to async generator

## How It Works

### Density Sampling
```
For each voxel corner:
  1. Sample NoiseRouter.finalDensity(x, y, z)
  2. Positive = solid, negative = air
  3. Store in density map
```

### Dual Contouring
```
For each 16³ voxel cell:
  1. Check 8 corners for type changes
  2. If surface crosses the cell:
     - Find edge crossings
     - Determine face direction
     - Assign block state
  3. Set tile data
```

### Block State Assignment
For v1, uses simple rules:
- Below sea level - 10: Stone
- Near sea level: Gravel
- At surface: Grass Block
- Near surface: Dirt
- Above: Stone

## Visual Result

**Before (Phase 4):**
- Only terrain within vanilla render distance
- Limited to ~256 blocks

**After (Phase 5):**
- Terrain visible at extreme distances
- 10,000+ block view distance possible
- Caves, overhangs, mountains all visible
- No chunk loading required for far terrain

## Known Limitations

1. **No biome variation** — Uses simple height-based block assignment
2. **No surface rules** — Doesn't use Minecraft's surface builder
3. **No structures** — Trees, villages, etc. not generated
4. **Simplified density** — Only samples finalDensity, not full noise router
5. **No population** — No ores, caves, or decorations

## Next Steps

1. **Biome-aware rough generation** — Sample biome data for proper block states
2. **Surface rules** — Use Minecraft's surface builder for grass/dirt/sand
3. **Cave generation** — Sample cave density functions
4. **Fog** — Distance-based fog matching vanilla
5. **Texture atlas** — Map block states to actual textures

## Build Instructions

Same as before:
```bash
cd /home/user/farplane
export JAVA_HOME=/path/to/jdk-25
./gradlew build
```
