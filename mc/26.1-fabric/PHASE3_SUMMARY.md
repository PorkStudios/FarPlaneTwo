# FarPlane 26.1 — Phase 3 Summary

## What's New in Phase 3

Phase 3 adds async tile generation, visibility tracking, disk caching, and Fabric networking infrastructure.

### New Files

**Storage Layer** (`src/main/java/dev/farplane/engine/storage/`)
- `TileStorage.java` — Interface for tile storage backends
- `FileTileStorage.java` — File-based storage implementation
  - Stores tiles as files: `{root}/tiles/{level}/{x}/{y}/{z}.dat`
  - Async I/O with dedicated thread
  - Simple format for v1 (will be enhanced with proper serialization)

**Tracking System** (`src/main/java/dev/farplane/engine/tracking/`)
- `TileTracker.java` — Visibility tracking (adapted from FP2 `Tracker`)
  - Maintains cube of tiles around player per LoD level
  - Triggers updates when player moves ≥8 blocks
  - Notifies listeners for load/unload events
  - Sorts tiles by priority (level first, then Manhattan distance)

**Async Generation** (`src/main/java/dev/farplane/engine/`)
- `AsyncTileGenerator.java` — Background tile generation
  - Uses thread pool (configurable `terrainThreads`)
  - Loads from disk cache first, generates if missing
  - Supports scaling from child tiles for higher LoD levels
  - Unloads distant tiles automatically

**Networking** (`src/main/java/dev/farplane/engine/network/`)
- `FarplaneNetworking.java` — Fabric networking infrastructure
  - Registers payloads for tile data, unload, and config sync
  - Uses Fabric Networking API v1
  - Ready for server→client tile streaming

### Modified Files

- `ExactVoxelGenerator.java` — Added constructor accepting `BlockSampleSource`
- `VoxelTerrainRenderer.java` — Rewritten to use async generator and tracker
- `Farplane.java` — Registers network payloads on init
- `build.gradle` — Notes about fabric-networking-api dependency

## Architecture

```
Player Movement
    ↓
TileTracker.update()
    ↓ (computes visible positions)
    ↓ (notifies listeners for added/removed)
    ↓
AsyncTileGenerator.requestGeneration()
    ↓ (checks disk cache)
    ↓ (generates if missing)
    ↓ (saves to disk)
    ↓
VoxelBaker.bake()
    ↓
VoxelTerrainRenderer.render()
```

## Known Limitations

1. **Tile serialization incomplete** — FileTileStorage write/read is placeholder
2. **Client-side only** — Tracker runs on client; server-side TileProvider not yet implemented
3. **No network sync** — Packets registered but not wired to tile streaming
4. **No LoD scaling in tracker** — Only level 0 tiles are generated
5. **Cache directory** — Uses `farplane_cache` in working directory (should be world-specific)

## Next Steps (Phase 4)

1. Complete tile serialization for disk cache
2. Implement server-side TileProvider
3. Wire networking for server→client tile streaming
4. Port shaders for proper rendering
5. Add LoD scaling to tracker

## Build Instructions

Same as before:
```bash
cd /home/user/farplane
export JAVA_HOME=/path/to/jdk-25
./gradlew build
```
