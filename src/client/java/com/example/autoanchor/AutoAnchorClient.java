package com.example.autoanchor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

/**
 * AutoAnchor (v2 - one charge is enough)
 * ---------------------------------------
 * A Respawn Anchor only needs ONE charge to explode when used without
 * Glowstone outside the Nether - it doesn't need to be fully charged.
 * This version detects your real right-click on a Respawn Anchor while
 * holding Glowstone (which adds that single charge), then almost
 * immediately switches your hotbar away from Glowstone and right-clicks
 * the anchor again to trigger the explosion.
 *
 * Press NUMPAD 3 to toggle the whole mod on/off.
 *
 * Written for Minecraft 26.2 (Fabric, unobfuscated official mappings).
 */
public class AutoAnchorClient implements ClientModInitializer {

    // Ticks to wait after the charging click before triggering detonation.
    private static final int TICKS_BEFORE_DETONATE = 2;

    // Hotbar slot (0-indexed: 0 = slot "1" shown in-game) to switch to before
    // the detonating right-click. Make sure this slot does NOT contain Glowstone.
    private static final int DETONATE_SLOT = 0;

    private boolean enabled = true;
    private boolean lastToggleKeyState = false;

    private boolean awaitingDetonate = false;
    private int detonateCooldown = 0;
    private BlockHitResult pendingHitResult = null;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!enabled || awaitingDetonate) {
                return InteractionResult.PASS;
            }

            BlockState state = world.getBlockState(hitResult.getBlockPos());
            if (!(state.getBlock() instanceof RespawnAnchorBlock)) {
                return InteractionResult.PASS;
            }
            if (!player.getItemInHand(hand).is(Items.GLOWSTONE)) {
                return InteractionResult.PASS;
            }

            // Let this charging click go through normally, then queue the
            // detonation step - a single charge is already enough to blow.
            pendingHitResult = hitResult;
            awaitingDetonate = true;
            detonateCooldown = TICKS_BEFORE_DETONATE;

            return InteractionResult.PASS;
        });
    }

    private void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }

        handleToggleKey(client);

        if (!enabled) {
            awaitingDetonate = false;
            return;
        }

        if (!awaitingDetonate || pendingHitResult == null) {
            return;
        }

        detonateCooldown--;
        if (detonateCooldown > 0) {
            return;
        }

        // Switch hotbar to the designated (non-glowstone) slot.
        client.player.getInventory().setSelectedSlot(DETONATE_SLOT);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(DETONATE_SLOT));

        // Right-click the anchor again - even 1 charge + no glowstone +
        // outside the Nether = vanilla explosion behaviour.
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, pendingHitResult);

        awaitingDetonate = false;
        pendingHitResult = null;
    }

    private void handleToggleKey(Minecraft client) {
        long windowHandle = GLFW.glfwGetCurrentContext();
        boolean toggleKeyDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_KP_3) == GLFW.GLFW_PRESS;

        if (toggleKeyDown && !lastToggleKeyState) {
            enabled = !enabled;
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.literal("AutoAnchor: " + (enabled ? "ON" : "OFF"))
                );
            }
        }
        lastToggleKeyState = toggleKeyDown;
    }
}
