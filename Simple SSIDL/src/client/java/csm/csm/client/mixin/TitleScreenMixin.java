package csm.csm.client.mixin;

import csm.csm.client.gui.AccountSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addAccountButton(CallbackInfo ci) {
        int centerX = this.width / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int yPos = this.height / 4 + 48 - 24;

        Button accountButton = Button.builder(
                Component.literal("Choose Account"),
                button -> Minecraft.getInstance().setScreen(new AccountSelectionScreen(this)))
                .bounds(centerX - buttonWidth / 2, yPos, buttonWidth, buttonHeight)
                .build();
        this.addRenderableWidget(accountButton);
    }
}