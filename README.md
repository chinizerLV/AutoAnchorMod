# AutoAnchor — Fabric Mod (Minecraft 26.2)

Right-click a **Respawn Anchor** with **Glowstone** once - that single
charge is already enough. A couple ticks later, the mod automatically
switches your hotbar away from Glowstone and right-clicks the anchor
again, triggering an explosion (per vanilla behaviour: any charged
Respawn Anchor used without Glowstone outside the Nether explodes).
Press **Numpad 3** to toggle the whole mod on/off.

Built for **Minecraft 26.2**, using **Fabric Loader** + **Fabric API**,
Java 25, official (unobfuscated) mappings.

## How it works

1. Detects your real right-click on a Respawn Anchor while holding
   Glowstone. That charging click proceeds normally (adds 1 charge).
2. A couple ticks later, switches your selected hotbar slot to
   `DETONATE_SLOT` (make sure that slot does NOT hold Glowstone).
3. Right-clicks the anchor again - since it now has a charge and you're
   no longer holding Glowstone, this triggers the explosion (outside
   the Nether).

Entirely client-side - no mixins, no packet spoofing beyond a normal
hotbar-switch packet, no server-side changes.

**This will actually destroy blocks and can hurt/kill nearby players.**
Same caveat as before: servers with anti-cheat may flag unnaturally
fast repeated interactions. Meant for singleplayer or servers you
control / have explicit permission to use it on.

## Building the jar (via GitHub Actions)

1. Push/upload this project to a GitHub repository.
2. Actions tab -> wait for green -> Artifacts -> `autoanchor-jar`.
3. Extract the downloaded zip for the real `.jar`.

## Installing

1. Fabric Loader for Minecraft **26.2** (fabricmc.net).
2. Fabric API for **26.2** (Modrinth/CurseForge).
3. Both jars into your `mods` folder.
4. Launch with the Fabric profile.

## Using it

- Have Glowstone in hand, right-click a Respawn Anchor once - it
  charges and detonates automatically within about 2 ticks.
- Press Numpad 3 to toggle on/off (turn it off if you just want to
  charge an anchor normally without it exploding).

## Tuning

In `AutoAnchorClient.java`:

```java
private static final int TICKS_BEFORE_DETONATE = 2;
private static final int DETONATE_SLOT = 0;
```

- `TICKS_BEFORE_DETONATE`: delay between the charging click and the
  detonating click.
- `DETONATE_SLOT`: hotbar slot (0 = slot "1" in-game) to switch to
  before detonating. Make sure it doesn't contain Glowstone, or it'll
  just add another charge instead of exploding.

## Toggle key

Numpad 3 - `GLFW.GLFW_KEY_KP_3` in `handleToggleKey()`.
