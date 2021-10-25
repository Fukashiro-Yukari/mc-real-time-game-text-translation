package io.github.nepqneko.texttranslation.mixin;

import io.github.nepqneko.texttranslation.RealTimeGameTextTranslation;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;)V", shift = At.Shift.AFTER, remap = false), method = "stop")
    private void onStopping(CallbackInfo ci) {
        RealTimeGameTextTranslation.saveTranslationFiles();
    }
}
