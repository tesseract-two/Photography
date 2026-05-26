package net.blouflin.photography.mixin;

import net.blouflin.photography.client.PhotographyHud;
import net.blouflin.photography.networking.CreateMapStatePayload;
import net.blouflin.photography.networking.SetUsingPhotographyCameraPayload;
import net.blouflin.photography.player.PlayerIsUsingCamera;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(SpyglassItem.class)
public abstract class SpyglassItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void injected(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        cir.setReturnValue(new InteractionResult.Pass());

        if (world.isClientSide()) {

            boolean isPhotographyCamera = false;
            String toContain = "isPhotographyCamera:1b";
            Minecraft client = Minecraft.getInstance();

            if (user.getItemInHand(hand).getComponents().has(DataComponents.CUSTOM_DATA)) {
                isPhotographyCamera = user.getItemInHand(hand).getComponents().get(DataComponents.CUSTOM_DATA).toString().contains(toContain);
            }

            if (isPhotographyCamera) {
                if (client.options.getCameraType().isFirstPerson()) {
                    if (PhotographyHud.isUsingPhotographyCamera) {
                        if (Objects.equals(PhotographyHud.handUsingPhotographyCamera, hand.toString())) {
                            if (PhotographyHud.canTakePhoto) {
                                PhotographyHud.canTakePhoto = false;
                                PhotographyHud.isTakingPhoto = true;
                                CreateMapStatePayload payload = new CreateMapStatePayload();
                                ClientPlayNetworking.send(payload);
                            }
                        }
                    } else {
                        PhotographyHud.zoomAmount = 1.0f;
                        PhotographyHud.handUsingPhotographyCamera = hand.toString();
                        PhotographyHud.defaultMouseSensitivity = client.options.sensitivity().get();
                        PhotographyHud.isHUDhidden = client.gui.hud.isHidden();
                        if (!client.gui.hud.isHidden()) { client.gui.hud.toggle(); }
                        PhotographyHud.isUsingPhotographyCamera = true;
                        user.playSound(SoundEvents.SPYGLASS_USE, 1.0f, 1.0f);
                        SetUsingPhotographyCameraPayload payload = new SetUsingPhotographyCameraPayload(PhotographyHud.isUsingPhotographyCamera, PhotographyHud.handUsingPhotographyCamera);
                        ClientPlayNetworking.send(payload);
                    }
                }
            } else {
                user.playSound(SoundEvents.SPYGLASS_USE, 1.0f, 1.0f);
                user.awardStat(Stats.ITEM_USED.get(Items.SPYGLASS));
                cir.setReturnValue(ItemUtils.startUsingInstantly(world, user, hand));
            }
        } else {
            boolean isPhotographyCamera = false;
            String toContain = "isPhotographyCamera:1b";

            if (user.getItemInHand(hand).getComponents().has(DataComponents.CUSTOM_DATA)) {
                isPhotographyCamera = user.getItemInHand(hand).getComponents().get(DataComponents.CUSTOM_DATA).toString().contains(toContain);
            }
            if (!isPhotographyCamera) {
                if (!((PlayerIsUsingCamera) user).isUsingPhotographyCamera()) {
                    user.playSound(SoundEvents.SPYGLASS_USE, 1.0f, 1.0f);
                    user.awardStat(Stats.ITEM_USED.get(Items.SPYGLASS));
                    cir.setReturnValue(ItemUtils.startUsingInstantly(world, user, hand));
                }
            }
        }
    }
}
