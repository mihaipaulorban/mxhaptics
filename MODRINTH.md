# Text to paste into Modrinth

## Summary (Modrinth "Summary" field, keep it short)

> Haptic feedback on the Logitech MX Master 4 mouse for in-game events (damage, shield, air bubbles, blocks, hotbar). Requires HapticWeb for Logi Options+ and a compatible mouse. Client-side, works on any server.

---

## Long description (Modrinth "Description" field, Markdown)

Everything below this line goes into the long description on Modrinth.

---

**MX Haptics** brings real haptic feedback to Minecraft on your **Logitech MX Master 4** mouse. Every time you take damage, block an attack with your shield, run out of air, or interact with the world, the mouse vibrates with a configurable pattern and intensity. No mods, no reflexes — the mouse tells your hand what's happening.

## Features

- 6 in-game events, each with independent on/off, pattern (0–15), intensity (0–100) and cooldown:
  - **Damage taken** — enabled by default, strong feedback
  - **Shield block** (melee *and* projectiles, including arrows) — enabled
  - **Air bubble popped** — enabled, subtle warning as you drown
  - **Block broken** — disabled by default
  - **Block placed** — disabled by default
  - **Hotbar slot change** — disabled by default, very light tick
- Configurable in-game (with Cloth Config + Mod Menu) or via `mxhaptics.json`.
- Keybind "Test haptic vibration" for a one-press test any time.
- `/mxhaptics_test [pattern] [intensity]` client command for manual testing.
- Auto-reconnect with exponential backoff — if Logi Options+ isn't running the game plays normally and the mod tries again in the background.
- Fully asynchronous — the bridge WebSocket never touches the game thread.
- Cross-platform: same jar works on Windows and macOS.
- Works on any server, including vanilla. Everything is client-side.

## Requirements

1. A **Logitech MX Master 4** mouse
2. **[Logi Options+](https://www.logitech.com/software/logi-options-plus.html)** installed and running
3. The **HapticWeb** plugin for Logi Options+, installable from the Logi Marketplace (default endpoint: `wss://local.jmw.nz:41443/ws`)
4. **Minecraft 26.2**, **Fabric Loader ≥ 0.19.3**, **Fabric API 0.155.2+26.2**, **Java 25**

**Optional** for the in-game config screen: [Cloth Config API](https://modrinth.com/mod/cloth-config) and [Mod Menu](https://modrinth.com/mod/modmenu).

## Default settings

| Event | Default | Pattern | Intensity | Cooldown |
|---|---|---|---|---|
| Damage taken | Enabled | 1 | 100 | 80 ms |
| Block broken | Disabled | 0 | 75 | 80 ms |
| Air bubble popped | Enabled | 1 | 75 | 200 ms |
| Block placed | Disabled | 0 | 75 | 80 ms |
| Shield block | Enabled | 1 | 50 | 80 ms |
| Hotbar slot change | Disabled | 0 | 5 | 20 ms |

## How to install

1. Install Fabric Loader for Minecraft 26.2.
2. Drop into your `mods/` folder: **Fabric API**, `mxhaptics-1.2.0.jar`, and optionally **Cloth Config** and **Mod Menu**.
3. Start Logi Options+ with the HapticWeb plugin enabled.
4. Launch Minecraft with the Fabric profile.

You should see `haptic bridge connesso` in the game log. If not, the mod will log a single line telling you the plugin isn't available and keep retrying quietly. The game runs normally either way.

## How to test the vibration

- **Keybind (one-click):** Options → Controls → Key Binds → MX Haptics → "Test haptic vibration" — assign a key, press it any time to feel a test vibration.
- **From chat:** `/mxhaptics_test 1 75` (or `/mxhaptics_test <pattern> <intensity>`).
- **From the config screen:** open Mod Menu → MX Haptics → toggle a "Test vibration" widget ON and click "Save Changes".

## License

MIT — free to use, modify and redistribute.

## Source code

[github.com/mihaipaulorban/mxhaptics](https://github.com/mihaipaulorban/mxhaptics)
