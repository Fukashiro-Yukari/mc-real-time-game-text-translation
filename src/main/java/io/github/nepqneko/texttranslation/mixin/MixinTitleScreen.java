package io.github.nepqneko.texttranslation.mixin;

import io.github.nepqneko.texttranslation.RealTimeGameTextTranslation;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
    private boolean hasLog = false;

    @Inject(at = @At("HEAD"), method = "init()V")
    private void init(CallbackInfo info) {
        if (!hasLog) {
            hasLog = true;

            if (RealTimeGameTextTranslation.IsSuccessfullyLoaded)
                RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation function is loaded");
            else
                RealTimeGameTextTranslation.LOGGER.error("[" + RealTimeGameTextTranslation.FULL_NAME + "] Not loaded properly, translation function cannot be loaded");
        }
    }
}
