package com.example.autoanchor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

/**
 * AutoAnchor
 * ----------
 * Normally, charging a Respawn Anchor with Glowstone takes 4 separate
 * right-clicks (one charge per click, max 4 charges). This mod detects
 * your first real right-click on a Respawn Anchor while holding
 * Glowstone, then automatically performs the remaining right-clicks for
 * you over the next few ticks until it's fully charged (or you run out
 * of Glowstone).
 *
 * Press NUMPAD 3 to toggle on/off.
 *
 * Written for Minecraft 26.2 (Fabric, unobfuscated official mappings).
 */
public class AutoAnchorClient implements ClientModInitializer {

    // Ticks to wait between each automatic extra charge.
    private static final int TICKS_BETWEEN_CHARGES = 2;

    private boolean enabled = true;
    private boolean lastToggleKeyState = false;

    // State for an in-progress auto-charge sequence.
    private BlockHitResult pendingHitResult = null;
    private InteractionHand pendingHand = null;
    private int remainingAutoCharges = 0;
    private int cooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!enabled) {
                return InteractionResult.PASS;
            }
            if (remainingAutoCharges > 0) {
                // Already mid-sequence, don't start another one.
                return InteractionResult.PASS;
            }

            BlockState state = world.getBlockState(hitResult.getBlockPos());
            if (!(state.getBlock() instanceof RespawnAnchorBlock)) {
                return InteractionResult.PASS;
            }
            if (!player.getItemInHand(hand).is(Items.GLOWSTONE)) {
                return InteractionResult.PASS;
            }

            int currentCharge = state.getValue(RespawnAnchorBlock.CHARGE);
            int maxCharge = 4;
            int chargesNeeded = maxCharge - currentCharge; // includes the click that's happening right now

            if (chargesNeeded <= 1) {
                // Already full, or this single click will finish it - let vanilla handle it normally.
                return InteractionResult.PASS;
            }

            // Let this real click go through normally, then schedule the rest.
            remainingAutoCharges = chargesNeeded - 1;
            pendingHitResult = hitResult;
            pendingHand = hand;
            cooldown = TICKS_BETWEEN_CHARGES;

            return InteractionResult.PASS;
        });
    }

    private void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }

        handleToggleKey(client);

        if (!enabled) {
            remainingAutoCharges = 0;
            return;
        }

        if (remainingAutoCharges <= 0 || pendingHitResult == null || pendingHand == null) {
            return;
        }

        cooldown--;
        if (cooldown > 0) {
            return;
        }

        // Stop early if we ran out of glowstone.
        if (!client.player.getItemInHand(pendingHand).is(Items.GLOWSTONE)) {
            remainingAutoCharges = 0;
            return;
        }

        client.gameMode.useItemOn(client.player, pendingHand, pendingHitResult);
        remainingAutoCharges--;
        cooldown = TICKS_BETWEEN_CHARGES;

        if (remainingAutoCharges <= 0) {
            pendingHitResult = null;
            pendingHand = null;
        }
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
