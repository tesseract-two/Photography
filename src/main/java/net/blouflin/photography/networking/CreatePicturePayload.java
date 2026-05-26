package net.blouflin.photography.networking;

import net.blouflin.image2map.Image2Map;
import net.blouflin.image2map.renderer.MapRenderer;
import net.blouflin.photography.PhotographyUtil;
import net.blouflin.photography.client.PhotographyHud;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public record CreatePicturePayload(Integer id, CompoundTag nbtCompound) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CreatePicturePayload> ID = CustomPacketPayload.createType("photography_create_picture");
    public static final StreamCodec<FriendlyByteBuf, CreatePicturePayload> CODEC = StreamCodec.ofMember((value, buf) -> buf.writeInt(value.id).writeNbt(value.nbtCompound), buf -> new CreatePicturePayload(buf.readInt(),buf.readNbt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void receive(Minecraft client, Integer id, CompoundTag nbtCompound) {

        CompletableFuture<Void> future = new CompletableFuture<>();

        client.execute(() -> {

            HolderLookup.Provider registryLookup = client.player.registryAccess();
            MapItemSavedData mapState = PhotographyUtil.fromNbt(nbtCompound);

            PhotographyHud.CAMERA_SCOPE_TO_RENDER = PhotographyHud.CAMERA_SCOPE_CLEAR;

            PhotographyHud.setScreenshotFuture(future);

            future.thenRun(() -> {
                //ScreenshotRecorder.saveScreenshot(client.runDirectory, client.getFramebuffer(), (text) -> {});

                PhotographyHud.CAMERA_SCOPE_TO_RENDER = PhotographyHud.CAMERA_SCOPE;
                PhotographyHud.spyglassFlashOpacity = 1.0f;
                PhotographyHud.isTakingPhoto = false;

                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), (nativeImage -> {
                    int[] pixels = nativeImage.getPixels();
                    BufferedImage bufferedImage = new BufferedImage(nativeImage.getWidth(), nativeImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    bufferedImage.setRGB(0, 0, nativeImage.getWidth(), nativeImage.getHeight(), pixels, 0, nativeImage.getWidth());
                    //System.out.println("bufferedImage: "+bufferedImage);
                    nativeImage.close();

                    try {
                        bufferedImage = CreatePicturePayload.crop(bufferedImage, bufferedImage.getHeight(), bufferedImage.getHeight());
                        //Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(), (text) -> {});

                        // TODO Debug
                        //System.out.println("bufferedImage: "+bufferedImage);

                        //System.out.println("Printing nbtCompound from CreatePicturePayload: " + nbtCompound);
                        MapItemSavedData mapState1 = MapRenderer.render(bufferedImage, Image2Map.DitherMode.FLOYD, id, mapState);
                        //System.out.println("Printing nbtCompound from CreatePicturePayload after: " + nbtCompound);

                        // TODO Debug
                        //System.out.println("mapstate1: "+mapState1);

                        SpawnPicturePayload payload = new SpawnPicturePayload(id, nbtCompound);
                        ClientPlayNetworking.send(payload);

                        // TODO Debug
                        //System.out.println("Cropping succeeded!");
                        //System.out.println("payload: "+payload);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));
            });
        });
    }

    public static BufferedImage crop(BufferedImage bufferedImage, int targetWidth, int targetHeight) throws IOException {
        // TODO: This cropping system doesn't work when the image height is greater than the image width
        //System.out.println("bufferedImage width: "+bufferedImage.getWidth() + " bufferedImage height: "+bufferedImage.getHeight());

        int height = bufferedImage.getHeight();
        int width = bufferedImage.getWidth();
        // TODO Debug
        //System.out.println("Height: "+height+" Width: "+width);

        int xc = 0, yc = 0;

        // Coordinates of the image's top-left corner
        if (targetHeight > width) {
            targetWidth = width;

            xc = (targetWidth - width) / 2;
            yc = (height - width) / 2;

            targetHeight = width;

        } else {
            xc = (width - targetWidth) / 2;
            yc = (height - targetHeight) / 2;
        }
        // TODO Debug
        //System.out.println("xc: "+xc+" yc: "+yc);

        // Crop
        BufferedImage croppedImage = bufferedImage.getSubimage(
                xc,
                yc,
                targetWidth, // width
                targetHeight // height
        );
        // TODO Debug
        //System.out.println("croppedImage: "+croppedImage);

        return croppedImage;
    }
}