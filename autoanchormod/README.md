# AutoAnchor — Fabric Mod (Minecraft 26.2)

Right-click a **Respawn Anchor** with **Glowstone** once, and it
automatically finishes charging it to the max (4 charges) instead of
requiring 4 separate manual right-clicks. Press **Numpad 3** to toggle
on/off — a chat message confirms the state.

Built for **Minecraft 26.2**, using **Fabric Loader** + **Fabric API**,
Java 25, official (unobfuscated) mappings.

## How it works

It listens for your real right-click on a Respawn Anchor while holding
Glowstone. That first click proceeds completely normally. Then, over
the next several ticks (small delay between each, so they register
reliably), it automatically performs the remaining right-clicks needed
to bring the anchor to full charge — stopping early if you run out of
Glowstone, or if the anchor is already full.

Entirely client-side — no mixins, no packet spoofing, no server-side
changes.

Same caveat as the other mods here: servers with anti-cheat may flag
unnaturally fast repeated block interactions. Meant for singleplayer or
servers you control / have permission to use it on.

## Building the jar (via GitHub Actions)

1. Push/upload this project to a GitHub repository.
2. Actions tab → wait for the run to go green → Artifacts →
   `autoanchor-jar`.
3. Extract the downloaded zip to get the real `.jar`.

## Installing

1. Fabric Loader for Minecraft **26.2** (fabricmc.net).
2. Fabric API for **26.2** (Modrinth/CurseForge).
3. Both jars into your `mods` folder.
4. Launch with the Fabric profile.

## Using it

- Have Glowstone in hand, right-click a Respawn Anchor once — it fills
  the rest automatically.
- Press Numpad 3 to toggle on/off.

## Tuning

```java
private static final int TICKS_BETWEEN_CHARGES = 2;
```

Ticks between each automatic charge attempt. Lower = faster fill, but
risk of missed charges if too low (same tradeoff as the other mods in
this series).

## Toggle key

Numpad 3 — change `GLFW.GLFW_KEY_KP_3` in `handleToggleKey()` for a
different key.
