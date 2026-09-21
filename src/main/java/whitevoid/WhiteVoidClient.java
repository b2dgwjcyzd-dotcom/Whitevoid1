package whitevoid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import whitevoid.create.core.CreateCore;

public final class WhiteVoidClient implements ClientModInitializer {
    public static final String MOD_ID = "whitevoid";
    public static final Logger LOGGER = LoggerFactory.getLogger("WhiteVoid");

    private static CreateCore createCore;

    @Override
    public void onInitializeClient() {
        createCore = new CreateCore();
        createCore.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> createCore.tickAutosave());

        LOGGER.info("WhiteVoid initialized — CREATE core ready.");
    }

    public static CreateCore createCore() {
        if (createCore == null) {
            throw new IllegalStateException("WhiteVoid has not been initialized yet");
        }
        return createCore;
    }
}
