# Architecture Decision Records — KMP-MapBox

This document records high‑level architecture decisions for KMP-MapBox. 
Each ADR contains: context, decision, rationale/alternatives and consequences.

---

## ADR-001: Project modularization and public artifact

#### Context
Need to support Android and Web consumer apps while maximizing shared map and domain logic, and publish a reusable library for third-party consumption.

#### Decision
Adopt a clear module split:
- shared — Kotlin Multiplatform library (common + expect/actual) exposing a platform-agnostic map API. 
Publish as io.github.tiagopraia:kmp-mapbox.
- androidApp — Android sample/consumer using Compose UI and Mapbox Android SDK.
- webApp — Web sample/consumer using Compose HTML (DOM-based) and Mapbox GL JS.

#### Alternatives
- Keeping UI code out of shared reduces API surface and prevents leaking platform types.
- Alternative: place UI primitives in shared — not possible because Mapbox GL JS is not compatible
with overlay components of Kotlin, only HTML accepted.

#### Consequences
- Consumers get a minimal, stable API they can use regardless of platform.
- Changes to shared require changes in Android and Web separately, which means versions for both
can be different.

---

## ADR-002: Mapping provider selection & escape hatch

#### Context
Project constraints are academic/non-commercial and want the best developer experience across Android and Web.

#### Decision
Primary mapping provider: Mapbox (Mapbox Android SDK on Android, Mapbox GL JS on Web).
This decision happens because Mapbox as the libraries for the targets needed, has free plan for scholars
and has a stable and evolved code.

#### Alternatives
- Mapbox provides mature, fully featured SDKs. MapLibre (open source) was an acceptable fallback, but
in the making of the library it wasn't complete neither in Android nor Web, so, even if fully free,
it was decided to change to Mapbox. (They are similar, change, if needed, is not hard)

#### Consequences
- Project is subject to Mapbox account and licensing/pricing changes.

---

## ADR-003: Geolocation provider selection

#### Context
The provider for the geolocation of user and points in Map.

#### Decision
For consistency, it was decided to use the Geolocation provided by Mapbox library for
latitude and longitude of user location.
For points in the map (and for altitude both in points and user location), it was decided to
use a style with DEM (Digital Elevation Models).

#### Alternatives
- Compass Kmp library (https://github.com/jordond/compass) was an alternative, but appeared some
  problems of synchronization and initialization, in the beginning, so we ruled it out.
- To get altitude, in Android, we had the option of using Android Location API, which gives better
  values, but it wouldn't be consistent with the way we're evaluating altitude for clicks.

#### Consequences
Getting possibly worst values for altitude and making Map slower because of the Digital
Elevation Model, which makes the map 3D.

---

## ADR-004: Web rendering and UI framework

#### Context
Mapbox GL JS requires a real DOM element to mount a WebGL canvas and cannot be
embedded directly into a Skia canvas-based Compose UI tree.

#### Decision
Use Compose HTML (DOM-based Compose) for the webApp UI so Mapbox GL JS can mount to an actual DOM node.
Provide a small JS wrapper in shared/jsMain to bind the MapController to a DOM container passed from Compose HTML.
It's possible to use ComposeViewport and Kotlin Components if, and only if, these components don't overlay
the map.
The problem is that Skia canvas creates an WebGL and Mapbox GL JS creates an WebGL, you can't overlay
WebGl's.

#### Alternatives
- Using Compose for Web (canvas-based) would prevent direct Mapbox integration.
- Create a workaround to connect 2 WebGL's (no solution found for this)
- Change Mapbox to another library with WASM target (no solution found for this).
- Don't use full screen in Web (Not a good solution).

#### Consequences
- webApp uses a DOM-oriented compose flavor; Android remains Compose UI. 2 different UI's needed.

---

## ADR-005: Publishing, versioning, and releases

#### Context
Library should be consumable via Maven Central with clear release practices.

#### Decision
- Use semantic versioning (MAJOR.MINOR.PATCH).
- Use GitHub Actions to publish signed artifacts to Maven Central on tags following "vX.Y.Z".
- Require conventional commits for automated changelog generation and release notes.
- Every outdated version needs to be valued as dead and should always be changed to the newer version.

#### Alternatives
- Automated publishing reduces manual steps and errors.

#### Consequences
- Add signing and Sonatype configuration to CI; keep release keys in CI secrets.
