package csm.csm.client;

import net.fabricmc.api.ClientModInitializer;

public class AccountModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("[CSM] Account mod initialized");
    }
}