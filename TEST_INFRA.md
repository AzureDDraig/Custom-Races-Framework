# E2E Test Infra: Custom Races Framework Model Streaming

## Test Philosophy
- Opaque-box, requirement-driven verification derived directly from ORIGINAL_REQUEST.md.
- Multi-tier testing methodology:
  - Tier 1: Core Feature Verification (pack building, HTTP hosting, SHA-1 computation, packet serialization, fallback).
  - Tier 2: Boundary & Corner Cases (empty config folders, corrupt zips, invalid SHA-1, missing model paths, network timeouts).
  - Tier 3: Cross-Feature Interactions (server pack update + client cache invalidation, live reload + model hot-swap, Pehkui scaling + body parts).
  - Tier 4: Real-World Workload Scenarios (dedicated server startup, multi-client handshake, transformation during combat, graceful skin fallback).
  - Tier 5: Adversarial Coverage Hardening (white-box stress testing and integrity audits).

## Feature Coverage Matrix
| # | Feature | Requirement | Tier 1 | Tier 2 | Tier 3 | Tier 4 |
|---|---------|-------------|:------:|:------:|:------:|:------:|
| 1 | Server Pack Zip Generation | R1 | 5 | 5 | ✓ | ✓ |
| 2 | SHA-1 Calculation & Change Detection | R1 | 5 | 5 | ✓ | ✓ |
| 3 | Local Embedded HTTP Server | R1 | 5 | 5 | ✓ | ✓ |
| 4 | Client Handshake & Packet Delivery | R2 | 5 | 5 | ✓ | ✓ |
| 5 | Client-Side In-Memory Mounting | R2 | 5 | 5 | ✓ | ✓ |
| 6 | Dynamic GeckoLib Cache Registration | R3 | 5 | 5 | ✓ | ✓ |
| 7 | Missing Texture & Checkerboard Prevention | R3 | 5 | 5 | ✓ | ✓ |
| 8 | Were-Form & Body Part Attachment Rendering | R4 | 5 | 5 | ✓ | ✓ |

## Test Architecture
- Framework: JUnit 5 in `common/src/test/java/`
- Test Runner: `./gradlew test`
- Verification semantics: All tests must pass with 0 failures, 0 errors.
