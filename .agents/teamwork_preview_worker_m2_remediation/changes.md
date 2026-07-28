# Summary of Changes for Milestone 2 Remediation

## Files Modified

### 1. `common/src/main/java/ddraig/net/customraces/client/render/GeckoAssetResolver.java`
- **Fix Uncaught `ResourceLocationException`**: Added `addCandidate(...)` and `getDefaultLocation(...)` helper methods that safely construct `ResourceLocation` instances using `ResourceLocation.tryParse` wrapped in try-catch blocks. If candidate parsing returns empty (e.g. malformed inputs like `invalid_namespace::path`, `:missing_namespace`, or invalid characters), `parsePath` safely falls back to safe default locations (`DEFAULT_MODEL_LOCATION`, `DEFAULT_TEXTURE_LOCATION`, `DEFAULT_ANIMATION_LOCATION`) without throwing uncaught exceptions.
- **Fix Extension Normalization for `.json`**: Fixed `parsePath` logic so inputs ending with `.json` (such as `werewolf.json` or `models/were/werewolf.json`) are normalized to `.geo.json` for model resolution and `.animation.json` for animation resolution across `normalizedRelPath`, `cleanFilename`, and candidate resource locations.

### 2. `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`
- **Clean Up Dead Code**: Removed unused private method `loadAndBakeGeckoModel` (lines 164-211).
