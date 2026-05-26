package net.blouflin.photography.client;

import net.blouflin.photography.networking.SetUsingPhotographyCameraPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;

public class PhotographyHud {

    public static boolean isUsingPhotographyCamera = false;
    public static float spyglassFlashOpacity = 0.0f;
    public static float spyglassScale = 0.5f;
    public static boolean canTakePhoto = false;
    public static boolean isTakingPhoto = false;
    public static boolean isHUDhidden;
    public static String handUsingPhotographyCamera = InteractionHand.MAIN_HAND.name();
    public static double zoomAmount;
    public static double defaultMouseSensitivity;
    public static final Identifier CAMERA_SCOPE = Identifier.fromNamespaceAndPath("photography","camera_scope"); // requires path of textures/gui/sprites/
    public static final Identifier CAMERA_SCOPE_CLEAR = Identifier.fromNamespaceAndPath("photography","camera_scope_clear");
    public static final Identifier CAMERA_SCOPE_FLASH = Identifier.fromNamespaceAndPath("photography","camera_scope_flash");
    public static Identifier CAMERA_SCOPE_TO_RENDER = CAMERA_SCOPE;
    private static final Minecraft client = Minecraft.getInstance();
    private static final KeyMapping escapeKeybinding = new KeyMapping("key.keyboard.escape", GLFW.GLFW_KEY_ESCAPE, KeyMapping.Category.MISC);

    private static CompletableFuture<Void> screenshotFuture;
    public static void setScreenshotFuture(CompletableFuture<Void> future) {
        screenshotFuture = future;
    }

    public static void renderPhotographyCameraOverlay(GuiGraphicsExtractor context) {

        float f = client.getDeltaTracker().getGameTimeDeltaTicks();
        spyglassScale = Mth.lerp(0.5f * f, spyglassScale, 1.125f);

        if (client.options.getCameraType().isFirstPerson() && client.gui.screen() == null) {
            if (isTakingPhoto) {
                CAMERA_SCOPE_TO_RENDER = CAMERA_SCOPE_CLEAR;
            }

            if (!client.gui.hud.isHidden()) { client.gui.hud.toggle(); }
            checkIsPhotographyCameraOpen(client);
            if (!isHUDhidden) {
                renderSpyglassOverlay(context, spyglassScale);
            }
            spyglassFlashOpacity = Mth.lerp(0.1f * f, spyglassFlashOpacity, 0.0125f);

            if (spyglassScale >= 1.1f && spyglassFlashOpacity <= 0.1f && !isTakingPhoto) {
                canTakePhoto = true;
            } else {
                canTakePhoto = false;
            }

            if (escapeKeybinding.isDown()) {
                stopRenderPhotographyCameraOverlay();
            }
        } else {
            stopRenderPhotographyCameraOverlay();
        }

        if (screenshotFuture != null) {
            screenshotFuture.complete(null);
            screenshotFuture = null;
        }
    }

    public static void stopRenderPhotographyCameraOverlay() {
        client.options.sensitivity().set(defaultMouseSensitivity);
        if (client.gui.hud.isHidden() != isHUDhidden) { client.gui.hud.toggle(); }
        spyglassFlashOpacity = 0.0f;
        spyglassScale = 0.5f;
        canTakePhoto = false;
        zoomAmount = 1.0f;
        PhotographyHud.isUsingPhotographyCamera = false;
        client.player.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0f, 1.0f);
        SetUsingPhotographyCameraPayload payload = new SetUsingPhotographyCameraPayload(isUsingPhotographyCamera, handUsingPhotographyCamera);
        ClientPlayNetworking.send(payload);
    }

    public static void checkIsPhotographyCameraOpen(Minecraft client) {
        boolean isPhotographyCamera = false;
        String toContain = "isPhotographyCamera:1b";
        Player player = client.player;
        InteractionHand hand = InteractionHand.valueOf(handUsingPhotographyCamera);
        if (player.getItemInHand(hand).getComponents().has(DataComponents.CUSTOM_DATA)) {
            isPhotographyCamera = player.getItemInHand(hand).getComponents().get(DataComponents.CUSTOM_DATA).toString().contains(toContain);
        }
        if (!isPhotographyCamera) {
            if (PhotographyHud.isUsingPhotographyCamera) {
                stopRenderPhotographyCameraOverlay();
            }
        }
    }

    private static void renderSpyglassOverlay(GuiGraphicsExtractor context, float scale) {
        float f;
        float g = f = (float)Math.min(context.guiWidth(), context.guiHeight());
        float h = Math.min((float)context.guiWidth() / f, (float)context.guiHeight() / g) * scale;
        int i = Mth.floor(f * h);
        int j = Mth.floor(g * h);
        int k = (context.guiWidth() - i) / 2;
        int l = (context.guiHeight() - j) / 2;
        int m = k + i;
        int n = l + j;

        context.blitSprite(RenderPipelines.GUI_TEXTURED, CAMERA_SCOPE_TO_RENDER, k, l, i, j);

        context.blitSprite(RenderPipelines.GUI_TEXTURED, CAMERA_SCOPE_FLASH, k, l, i, j, spyglassFlashOpacity);

        context.fill(RenderPipelines.GUI, 0, n, context.guiWidth(), context.guiHeight(), CommonColors.BLACK);
        context.fill(RenderPipelines.GUI, 0, 0, context.guiWidth(), l, CommonColors.BLACK);
        context.fill(RenderPipelines.GUI, 0, l, k, n, CommonColors.BLACK);
        context.fill(RenderPipelines.GUI, m, l, context.guiWidth(), n, CommonColors.BLACK);

        //context.drawText(MinecraftClient.getInstance().textRenderer, "Hello, world!", k, l, 0xFFFFFFFF, false);

    }
}