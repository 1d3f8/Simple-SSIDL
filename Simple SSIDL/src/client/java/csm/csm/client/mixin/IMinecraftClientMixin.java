package csm.csm.client.mixin;

import csm.csm.client.gui.AccountSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class IMinecraftClientMixin {

    @Inject(method = "getUser", at = @At("HEAD"), cancellable = true)
    private void onGetUser(CallbackInfoReturnable<User> cir) {
        User overridden = AccountSelectionScreen.getOverriddenUser();
        if (overridden != null) {
            cir.setReturnValue(overridden);
        }
    }
}