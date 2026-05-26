package net.blouflin.photography;

import net.blouflin.photography.networking.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Photography implements ModInitializer {
	public static final String MOD_ID = "Photography";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier CAMERA_SHUTTER_SOUND = Identifier.fromNamespaceAndPath("photography","camera_shutter");
	public static SoundEvent CAMERA_SHUTTER = SoundEvent.createVariableRangeEvent(CAMERA_SHUTTER_SOUND);

	@Override
	public void onInitialize() {
		//LOGGER.info("Photography mod (by BlouFlin) loaded !");

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(this::addItemsToCreativeTab);

		PayloadTypeRegistry.playS2C().register(CreatePicturePayload.ID, CreatePicturePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(GetUsingPhotographyCameraPayload.ID, GetUsingPhotographyCameraPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(PlayCameraShutterSoundPayload.ID, PlayCameraShutterSoundPayload.CODEC);

		PayloadTypeRegistry.playC2S().register(CreateMapStatePayload.ID, CreateMapStatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CreateMapStatePayload.ID, (payload, handler) -> CreateMapStatePayload.receive(handler.player()));

		PayloadTypeRegistry.playC2S().register(SpawnPicturePayload.ID, SpawnPicturePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SpawnPicturePayload.ID, (payload, handler) -> SpawnPicturePayload.receive(handler.player(), payload.id(), payload.nbtCompound()));

		PayloadTypeRegistry.playC2S().register(SetUsingPhotographyCameraPayload.ID, SetUsingPhotographyCameraPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SetUsingPhotographyCameraPayload.ID, (payload, handler) -> SetUsingPhotographyCameraPayload.receive(handler.player(), payload.isUsingPhotographyCamera(), payload.handUsingPhotographyCamera()));
	}

	private void addItemsToCreativeTab(FabricItemGroupEntries entries) {
		ItemStack photographyCamera = new ItemStack(Items.SPYGLASS);
		photographyCamera.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, comp -> comp.update(currentNbt -> {
			currentNbt.putBoolean("isPhotographyCamera",true);
		}));
        photographyCamera.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(56774F), List.of(), List.of(), List.of()));
		photographyCamera.set(DataComponents.ITEM_NAME, Component.literal("Camera"));
		entries.addAfter(Items.MAP, photographyCamera);

		ItemStack photographicPaper = new ItemStack(Items.PAPER);
		photographicPaper.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, comp -> comp.update(currentNbt -> {
			currentNbt.putBoolean("isPhotographyEmptyMap",true);
		}));
        photographicPaper.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(56775F), List.of(), List.of(), List.of()));
		photographicPaper.set(DataComponents.ITEM_NAME, Component.translatableWithFallback("photography:empty_map", "Photographic Paper"));
		entries.addBefore(Items.WRITABLE_BOOK, photographicPaper);
	}

//    private void countMaps(MapIdComponent id) throws IOException {
//        // create a list of maps made using the mod with an associated timestamp
//		try {
//			Files.createFile(FabricLoader.getInstance().getConfigDir());
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		String mapCount = id.asString() + ;
//
//		Files.writeString(, id);
//	}
}