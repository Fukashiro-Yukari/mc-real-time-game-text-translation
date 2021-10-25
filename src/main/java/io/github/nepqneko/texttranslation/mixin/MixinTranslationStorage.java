package io.github.nepqneko.texttranslation.mixin;

import io.github.nepqneko.texttranslation.RealTimeGameTextTranslation;
import io.github.nepqneko.texttranslation.translation.CustomTranslationStorage;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TranslationStorage.class)
public abstract class MixinTranslationStorage {
    @Inject(at = @At("RETURN"), method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;)Lnet/minecraft/client/resource/language/TranslationStorage;")
    private static void load(ResourceManager resourceManager, List<LanguageDefinition> definitions, CallbackInfoReturnable<TranslationStorage> cir) {
        RealTimeGameTextTranslation.EnglishTranslations = CustomTranslationStorage.load(resourceManager, definitions);
    }
}
