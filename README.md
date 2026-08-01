# AutoAnchor — Fabric Mod (Minecraft 26.2)

Right-click a **Respawn Anchor** with **Glowstone** once, and it
automatically finishes charging it to max (4 charges). Once fully
charged, it automatically switches your hotbar away from glowstone and
right-clicks the anchor again — which, per **vanilla Minecraft
behaviour**, detonates a fully-charged Respawn Anchor when used without
Glowstone outside the Nether. Press **Numpad 3** to toggle the whole mod
on/off.

Built for **Minecraft 26.2**, using **Fabric Loader** + **Fabric API**,
Java 25, official (unobfuscated) mappings.

## How it works

1. Detects your real right-click on a Respawn Anchor while holding
   Glowstone. That first click proceeds normally.
2. Automatically performs the remaining right-clicks (small delay
   between each) until the anchor reaches full charge, or you run out
   of Glowstone.
3. Once full, waits a short moment, switches your selected hotbar slot
   to `DETONATE_SLOT` (make sure this slot doesn't hold Glowstone), and
   right-clicks the anchor one more time - triggering the explosion.

This is entirely using vanilla game mechanics automated together - the
mod doesn't invent new explosion behaviour, it just performs the same
actions a player could do manually, faster and hands-free.

**This will actually destroy blocks and can hurt/kill nearby players**,
same as manually triggering an anchor explosion would. Worth being sure
that's actually what you want before using it around other people or
builds you care about.

Entirely client-side - no mixins, no packet spoofing beyond what a
normal hotbar-switch already sends, no server-side changes.

Same caveat as the other mods here: servers with anti-cheat may flag
unnaturally fast repeated interactions. Meant for singleplayer or
servers you control / have permission to use it on.

## Building the jar (via GitHub Actions)

1. Push/upload this project to a GitHub repository.
2. Actions tab -> wait for the run to go green -> Artifacts ->
   `autoanchor-jar`.
3. Extract the downloaded zip to get the real `.jar`.

## Installing

1. Fabric Loader for Minecraft **26.2** (fabricmc.net).
2. Fabric API for **26.2** (Modrinth/CurseForge).
3. Both jars into your `mods` folder.
4. Launch with the Fabric profile.

## Using it

- Have Glowstone in hand, right-click a Respawn Anchor once - it fills
  and then detonates automatically.
- Press Numpad 3 to toggle the whole thing on/off (useful if you just
  want to charge one normally without it exploding).

## Tuning

In `AutoAnchorClient.java`:

```java
private static final int TICKS_BETWEEN_CHARGES = 2;
private static final int TICKS_BEFORE_DETONATE = 4;
private static final boolean DETONATE_AFTER_CHARGE = true;
private static final int DETONATE_SLOT = 0;
```

- `TICKS_BETWEEN_CHARGES`: delay between each automatic charge attempt.
- `TICKS_BEFORE_DETONATE`: delay after full charge before triggering
  the detonating click - gives a small buffer to actually move away if
  needed.
- `DETONATE_AFTER_CHARGE`: set to `false` to disable auto-detonation
  entirely and just have the "auto-fill charge" behaviour from before.
- `DETONATE_SLOT`: which hotbar slot (0 = the first slot, shown as "1"
  in-game) to switch to before the detonating click. **Make sure this
  slot is empty or holds something other than Glowstone**, or it'll
  just charge again instead of detonating.

## Toggle key

Numpad 3 - change `GLFW.GLFW_KEY_KP_3` in `handleToggleKey()` for a
different key.
