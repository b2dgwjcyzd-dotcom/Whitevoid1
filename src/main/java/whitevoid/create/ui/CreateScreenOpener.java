package whitevoid.create.ui;

import net.minecraft.client.MinecraftClient;
import whitevoid.WhiteVoidClient;

public final class CreateScreenOpener {
    private CreateScreenOpener() {}

    public static void open() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new CreateScreen(WhiteVoidClient.createCore())));
    }
}
