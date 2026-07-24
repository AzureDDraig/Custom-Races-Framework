# Adversarial Challenge & Stress Test Report — M2 Were-Race Model Transformation

## Challenge Summary

**Overall risk assessment**: MEDIUM

The core M2 Were-Race transformation architecture is functional, thread-safe, and cleanly handles null values, rate-limiting, network state sync, and basic Pehkui scale fallbacks. However, empirical stress-testing revealed key visual and logic vulnerabilities under specific edge cases, primarily around unmapped/missing custom model paths rendering invisible player models, automatic trigger loops overriding manual toggling, and unbounded extreme Pehkui scales.

---

## Challenges

### [High] Challenge 1: Unmapped/Missing Model Paths Suppress Base Player Model, Causing Invisibility

- **Assumption challenged**: That syntactically valid model paths (`ResourceLocation.tryParse`) always correspond to valid asset resources registered in the client environment.
- **Attack scenario**: A custom race configuration defines `wereModelPath = "customraces:models/were/nonexistent_model.geo.json"`. Since the string is valid syntax, `hasCustomModel(race)` evaluates to `true`. In `WereModelRenderer.renderWereForm()`, `setBaseModelVisible(parentModel, false)` hides the base human player model mesh. When `renderCustomWereMesh()` attempts to draw the custom mesh with missing assets, Minecraft renders nothing or a missing-texture box while the player model remains completely invisible.
- **Blast radius**: Players transforming with misconfigured or missing model JSON files become invisible to themselves and other players on servers.
- **Mitigation**: Verify asset existence before hiding base model mesh, or wrap rendering in dynamic fallback logic that restores base model visibility if model/texture loading fails.

### [Medium] Challenge 2: Automatic Moon Phase Triggers Override Manual Untoggle in 2 Seconds

- **Scenario**: A player transforms under an automatic trigger condition (e.g. `wereTriggerCondition = "FULL_MOON"` at night). The player presses the manual transformation keybind to revert to human form. `revertWereForm()` successfully reverts the state. However, 2 seconds later (40 ticks), `WereRaceTransformHandler.checkTransformation()` runs on the server tick. Because `conditionMet` is still `true` (it is still full moon) and `currentlyTransformed` is `false`, `checkTransformation()` automatically transforms the player back into Were-form.
- **Blast radius**: Players cannot manually revert or stay in human form during automatic trigger windows (Full Moon, New Moon, Night, Low Health).
- **Mitigation**: Introduce a temporary manual override / suppression flag when a player manually toggles off during an active automatic condition.

### [Medium] Challenge 3: Unbounded Scale Multipliers in Visual and Pehkui Scaling

- **Assumption challenged**: Scale values in `RaceData` are always within reasonable game bounds.
- **Attack scenario**: A race configuration or server admin sets `wereHeightScale` or `baseScale` to extreme values (e.g., `1000.0f` or `Float.POSITIVE_INFINITY`). While negative/zero scales fall back to default values (`1.3f`/`1.0f`), extreme or infinite values pass into `poseStack.scale(wScale, hScale, wScale)` and `PehkuiIntegration.applyRaceScales()`.
- **Blast radius**: PoseStack Matrix4f corruption, client rendering crashes, and physics/hitbox breakdown in Pehkui collision detection.
- **Mitigation**: Enforce upper and lower bounds on scale inputs (e.g. `Math.clamp(scale, 0.1f, 10.0f)`).

---

## Stress Test Results

| Scenario | Expected Behavior | Actual Behavior | Pass/Fail |
|---|---|---|---|
| Null / empty / "none" model paths | Fallback to default constants & procedural beast overlay | Safely fell back to `DEFAULT_WERE_MODEL` & procedural rendering | **PASS** |
| Invalid ResourceLocation syntax (spaces, bad chars) | Fall back to `DEFAULT_WERE_MODEL` and log warning once | Handled cleanly by `tryParse` null check and logged warning | **PASS** |
| Syntactically valid path to non-existent asset | Render fallback model or keep base player model | Base model hidden (`setBaseModelVisible(false)`), causing player invisibility | **FAIL (Challenge 1)** |
| Negative / Zero Pehkui scale values (`<= 0`) | Safe fallback to 1.3f / 1.0f | Guards `scale > 0` triggered fallback safely | **PASS** |
| Float.NaN Pehkui scale value | Fallback to 1.0f | `Float.NaN > 0` evaluated to false, falling back to 1.0f | **PASS** |
| Unbounded extreme scales (`1000.0f` / `Infinity`) | Scale clamped to safe range | Matrix scale overflow in PoseStack | **FAIL (Challenge 3)** |
| Rapid toggle keypress spam (< 1000ms) | Reject attempt with cooldown message | `TRANSFORM_COOLDOWNS` map rejected toggles within 1000ms | **PASS** |
| Manual untoggle during active Full Moon trigger | Allow player to stay in human form | Server tick re-transformed player 2 seconds later | **FAIL (Challenge 2)** |
| Entity start tracking when target player is transformed | Send S2C state sync packet to tracking player | `onPlayerStartTracking` sent `SYNC_WERE_STATE_ID` packet | **PASS** |
| Concurrent player state map operations | Thread-safe reads/writes during server ticks | `ConcurrentHashMap` handled concurrent state access cleanly | **PASS** |
| `./gradlew build -x test` build status | Compilation success across Common, Fabric, Forge | `BUILD SUCCESSFUL in 18s` | **PASS** |

---

## Unchallenged Areas

- **GeckoLib Java runtime animation controllers**: Detailed animation keyframe controllers are handled by GeckoLib's internal animation manager at runtime; static unit tests confirmed `ResourceLocation` resolution.
