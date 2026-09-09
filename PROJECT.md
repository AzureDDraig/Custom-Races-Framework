# Project: Dynamic Were & Body Part GeckoLib Model Streaming via Server Resource Pack

## Architecture
- **Common Module (`common/`)**:
  - `ServerPackManager`: Scans `config/custom_races/`, generates `customraces-server-pack.zip` (`pack.mcmeta` format 15), computes SHA-1 hash.
  - `ServerPackHttpServer`: Lightweight embedded Java 17 `HttpServer` serving `customraces-server-pack.zip` on configurable port (default 25585).
  - `ServerPackInfoPacket`: Network packet transmitting pack URL, SHA-1 hash, and byte size to connecting clients.
  - `ClientPackManager`: Client-side SHA-1 cache manager (`config/custom_races/cache/`), background downloader, and dynamic resource pack injector (`FallbackResourceManager.push`).
  - `GeckoLibCacheInjector`: Runtime injection into `GeckoLibCache` using `GeometryTree` and `BakedModelFactory` for lag-free hot model/animation registration.
  - `GeckoAssetResolver` & `WereModelRenderer`: Texture validation, missing texture prevention, base player mesh suppression guardrails, and procedural fallback.
  - `PlayerRaceLayer` & `CustomRaceModelRenderer`: Dynamic body part attachment rendering with 9-DOF transforms.
- **Fabric Module (`fabric/`)** & **Forge Module (`forge/`)**:
  - Shared entrypoints delegating to Common module; platform-neutral Architectury networking and lifecycle events.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Server Lifecycle Hooks | Architectury `SERVER_STARTING` (pack build & HTTP start) and `SERVER_STOPPING` (HTTP stop) | M1 | Survey 1 |
| 2 | Asset Scanner & Packager | Compiles `config/custom_races/models/`, `textures/`, `animations/` into `customraces-server-pack.zip` | M1 | Survey 1 |
| 3 | Pack Metadata Generator | Generates valid `pack.mcmeta` with `pack_format: 15` | M1 | Survey 1 |
| 4 | SHA-1 Hash Calculation | Computes 40-character lowercase hex SHA-1 for change detection | M1 | Survey 1 |
| 5 | Embedded HTTP Server | Built-in Java 17 `HttpServer` serving the pack on port 25585 | M1 | Survey 1 |
| 6 | Reload Command Support | `/customraces reload` & `/custom_races reload` rebuilds pack, updates SHA-1, and syncs clients | M1 | Survey 1 |
| 7 | Network Packet Handshake | Dedicated `customraces:server_pack_info` packet (URL, SHA-1, size) | M2 | Survey 2 |
| 8 | SHA-1 Client Cache | Caches pack as `config/custom_races/cache/pack-<sha1>.zip`; skips download if hash matches | M2 | Survey 2 |
| 9 | In-Memory Dynamic Mounting | Wraps zip in `FilePackResources` and pushes into `MultiPackResourceManager` / `FallbackResourceManager` | M2 | Survey 2 |
| 10 | Singleplayer Direct Mount | Direct filesystem mount for local/singleplayer worlds without HTTP roundtrip | M2 | Survey 2 |
| 11 | GeckoLib Resource Discovery | Pack layout places models under `assets/customraces/geo/` and animations under `assets/customraces/animations/` | M3 | Survey 3 |
| 12 | Dynamic Hot Model Baking | Bakes models/animations into `GeckoLibCache` using `GeometryTree` and `BakedModelFactory` (<5 ms, 0 freeze) | M3 | Survey 3 |
| 13 | Missing Texture Prevention | Dynamic registration into `TextureManager`, texture existence validation, fallback to player skin or dark fur | M3 | Survey 3 |
| 14 | Mesh Suppression Guardrails | Only hide base player mesh when custom GeckoLib model renders successfully; never leave player invisible | M3 | Survey 3 |
| 15 | Were-Form Streamed Rendering | Transformed players resolve GeckoLib models and textures directly from dynamic pack | M4 | Survey 3 |
| 16 | Procedural Were-Beast Fallback | Graceful fallback to ears, snout, and glowing red eyes on player mesh if model is missing or invalid | M4 | Survey 3 |
| 17 | Body Part Attachments | 6 attachment presets (ears, horns, halo, wings, tail, extra legs) with safe 9-DOF transforms and 1st-person suppression | M4 | Survey 3 |
| 18 | Multi-Platform Build & Verification | 100% `./gradlew test` passing, 0-error `./gradlew build -x test` for Fabric and Forge, updated `CHANGELOG.md` | M4 | Requirements |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Server Pack Generator & HTTP Server | `ServerPackManager`, `ServerPackHttpServer`, pack zip builder, SHA-1 computation, commands & lifecycle | none | DONE |
| 2 | M2: Client Handshake & In-Memory Injection | `ServerPackInfoPacket`, client download & SHA-1 cache, `ClientPackManager`, `FallbackResourceManager.push` | M1 | DONE |
| 3 | M3: Dynamic GeckoLib Registration & Cache | `GeckoLibCacheInjector`, hot baking via `GeometryTree`/`BakedModelFactory`, checkerboard prevention, mesh guardrails | M2 | DONE |
| 4 | M4: Were-Form Rendering & Build Verification | Complete Were-form & body part rendering with streamed assets, tests, multi-platform build, and CHANGELOG | M3 | DONE |

## Interface Contracts
### ServerPackManager ↔ ServerPackHttpServer (M1)
- `Path getGeneratedPackPath()`: returns path to `customraces-server-pack.zip`.
- `String getPackSha1()`: returns 40-char lowercase hex SHA-1 hash of the zip file.
- `void buildPack()`: scans directories, creates zip, calculates hash.
- `void startServer(int port)` / `void stopServer()`: manages HTTP server lifecycle.

### Server ↔ Client Network Handshake (M1 ↔ M2)
- Packet: `customraces:server_pack_info`
- Payload:
  - `String packUrl` (e.g. `http://<server-ip>:25585/customraces-server-pack.zip`)
  - `String sha1Hash` (40-character hex)
  - `long sizeBytes`
  - `boolean required`

### ClientPackManager ↔ GeckoLibCacheInjector (M2 ↔ M3)
- `void onPackMounted(Path packZipPath)`: called when dynamic pack is mounted into `ResourceManager`.
- Triggers `GeckoLibCacheInjector.reloadCustomRacesAssets(ResourceManager or ZipFile)`.

### GeckoLibCacheInjector ↔ WereModelRenderer & PlayerRaceLayer (M3 ↔ M4)
- `boolean isModelBaked(ResourceLocation modelLocation)`: returns true if model exists in `GeckoLibCache.getBakedModels()`.
- `ResourceLocation resolveTexture(Player player, String texturePath)`: returns validated texture or fallback.
- `boolean renderWereForm(...)`: returns true only if model rendered; else restores base player mesh visibility.

## Code Layout
- `common/src/main/java/ddraig/net/customraces/pack/`:
  - `ServerPackManager.java`: Pack packaging, zip creation, SHA-1 calculation.
  - `ServerPackHttpServer.java`: Lightweight embedded HTTP server.
  - `ClientPackManager.java`: Client-side cache, background download, and resource pack injection.
- `common/src/main/java/ddraig/net/customraces/network/`:
  - `ServerPackInfoPacket.java` (or integrated in `ModPackets.java` / `ClientPacketHandler.java`).
- `common/src/main/java/ddraig/net/customraces/client/render/`:
  - `GeckoLibCacheInjector.java`: Dynamic GeckoLib model/animation injection.
  - `GeckoLibWereRenderer.java`: Hot model baking fixes and rendering.
  - `WereModelRenderer.java`: Transformation state, mesh visibility guardrails, fallback rendering.
  - `PlayerRaceLayer.java`: Player layer rendering, body part attachments.
  - `GeckoAssetResolver.java`: Texture and asset resolution, checkerboard prevention.
- `common/src/main/java/ddraig/net/customraces/command/`:
  - `CustomRacesCommands.java`: `/customraces reload` and aliases.
