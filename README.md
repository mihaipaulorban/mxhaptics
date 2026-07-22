# MX Haptics

Client-side Fabric mod for **Minecraft Java Edition 26.2** that sends haptic
feedback to the **Logitech MX Master 4** mouse while you play, through a local
plugin of the **Logi Options+** Actions SDK.

Free, open source (MIT), no accounts, no in-app purchases. Runs the same on
Windows and macOS: only standard Java networking, no native code. Works on any
server, including vanilla — everything happens client-side.

## Supported events

| Event | Default | Pattern | Intensity | Cooldown |
|---|---|---|---|---|
| Damage taken | **enabled** | 1 | 100 | 80 ms |
| Block broken | disabled | 0 | 75 | 80 ms |
| Air bubble popped | **enabled** | 1 | 75 | 200 ms |
| Block placed | disabled | 0 | 75 | 80 ms |
| Shield block | **enabled** | 1 | 50 | 80 ms |
| Hotbar slot change | disabled | 0 | 5 | 20 ms |

Each event is independently configurable: on/off, pattern (0–15), intensity
(0–100), and minimum cooldown between two triggers.

## Requirements

**For haptic feedback** (without these the mod still runs, it just won't vibrate):

1. A **Logitech MX Master 4** mouse
2. **Logi Options+** installed and running (Windows or macOS)
3. The **HapticWeb** plugin for Logi Options+ (available on the Logi Marketplace
   — default endpoint: `wss://local.jmw.nz:41443/ws`)

**To play:**

- Minecraft Java Edition **26.2**
- **Fabric Loader ≥ 0.19.3** and **Fabric API 0.155.2+26.2** (or a version
  compatible with 26.2)
- **Java 25**

**Optional** (for the in-game config screen):

- [Cloth Config API](https://modrinth.com/mod/cloth-config)
- [Mod Menu](https://modrinth.com/mod/modmenu)

Without these two mods you configure by editing `config/mxhaptics.json` (created
automatically on first launch with all fields ready to edit).

## Installation

1. Install Fabric Loader for Minecraft 26.2 (see fabricmc.net).
2. Drop these into `mods/`: **Fabric API**, `mxhaptics-1.2.0.jar`, and — if you
   want the config screen — **Cloth Config** and **Mod Menu**.
3. Start Logi Options+ with HapticWeb enabled, then launch the game.
4. In the game log you'll see `haptic bridge connesso a wss://…`. If the plugin
   isn't running you'll see a single `haptic bridge non disponibile` line and
   the mod will keep retrying quietly in the background (exponential backoff,
   1 s → 60 s). The game continues normally either way.

## Testing the vibration

- **Keybind (one-click):** open `Options → Controls → Key Binds`, find "MX
  Haptics" → "Test haptic vibration", assign a key. Pressing that key fires a
  fixed test vibration (pattern 1, intensity 75) any time.
- **In-screen:** open MX Haptics from Mod Menu, toggle "Test vibration" ON in
  the General category (or in any event's category), then click "Save Changes".
  Per-event test toggles use the pattern/intensity as edited in that session.
- **From chat:** type `/mxhaptics_test 1 75` (or
  `/mxhaptics_test <pattern> <intensity>`).

## Configuration

### From Mod Menu (with Cloth Config)

Mod Menu → MX Haptics → gear icon. A **General** category for the bridge URL
and TLS option, plus one category per event (Enabled, Pattern, Intensity,
Cooldown). Saving reconnects the bridge with the new settings.

### From file (`config/mxhaptics.json`)

```json
{
  "configVersion": 3,
  "bridgeUrl": "wss://local.jmw.nz:41443/ws",
  "trustAllCertificates": false,
  "events": {
    "damage":       { "enabled": true,  "pattern": 1, "intensity": 100, "cooldownMs": 80 },
    "blockBreak":   { "enabled": false, "pattern": 0, "intensity": 75,  "cooldownMs": 80 },
    "airBubble":    { "enabled": true,  "pattern": 1, "intensity": 75,  "cooldownMs": 200 },
    "blockPlace":   { "enabled": false, "pattern": 0, "intensity": 75,  "cooldownMs": 80 },
    "shieldBlock":  { "enabled": true,  "pattern": 1, "intensity": 50,  "cooldownMs": 80 },
    "hotbarChange": { "enabled": false, "pattern": 0, "intensity": 5,   "cooldownMs": 20 }
  }
}
```

- `bridgeUrl`: change it if you use a plugin other than HapticWeb or a
  different port.
- `trustAllCertificates`: set to `true` **only** if your local plugin uses a
  self-signed TLS certificate (not needed with HapticWeb).
- The file is read at game launch; if you edit it by hand, restart the game
  (or save once from the GUI, which reloads the connection).

## Build from source

Requires **JDK 25**. Gradle wrapper is included, so no separate Gradle install
needed.

```bash
./gradlew build
# Windows: gradlew.bat build
```

The output jar is `build/libs/mxhaptics-1.2.0.jar`.

For the dev environment: `./gradlew runClient` (uncomment the `localRuntime`
lines in `build.gradle` if you want Cloth Config and Mod Menu in dev too).

## How it works

- `HapticBridge` — `java.net.http.WebSocket` client on a single daemon thread:
  connects on start-up, auto-reconnects with exponential backoff, serialised
  and fully async sends. **No network call ever runs on the game thread.**
- `HapticManager` — single trigger entry point: applies enabled/cooldown per
  event, forwards to the bridge.
- `TickObservers` — at the end of each client tick detects damage taken
  (health drop), air bubbles (bubble-icon drop), hotbar change (selected slot
  change) and shield block (shield item durability increase while blocking —
  works for melee and projectiles, including arrows).
- `BlockItemMixin` — client-side mixin on `BlockItem#place` for block placement.
- Fabric API event `ClientPlayerBlockBreakEvents.AFTER` — for block breaks.

## Wire protocol

The mod sends binary WebSocket frames of **2 bytes**: `[pattern, intensity]`
where `pattern` is 0–15 and `intensity` is 0–100. HapticWeb reads only the first
byte and ignores the second, so the mod is fully backward-compatible; plugins
that support intensity can use the second byte.

## License

MIT — see [LICENSE](LICENSE).
