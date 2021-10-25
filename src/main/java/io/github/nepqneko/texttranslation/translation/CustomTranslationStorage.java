package io.github.nepqneko.texttranslation.translation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.ReorderingUtil;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class CustomTranslationStorage extends Language {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, String> translations;
    private final boolean rightToLeft;

    private CustomTranslationStorage(Map<String, String> translations, boolean rightToLeft) {
        this.translations = translations;
        this.rightToLeft = rightToLeft;
    }

    public static CustomTranslationStorage load(ResourceManager resourceManager, List<LanguageDefinition> definitions) {
        Map<String, String> map = Maps.newHashMap();
        String string = "lang/en_us.json";

        for (String string2 : resourceManager.getAllNamespaces()) {
            try {
                Identifier identifier = new Identifier(string2, string);
                load(resourceManager.getAllResources(identifier), map);
            } catch (FileNotFoundException ignored) {
            } catch (Exception var11) {
                LOGGER.warn("Skipped language file: {}:{} ({})", string2, string, var11.toString());
            }
        }

        return new CustomTranslationStorage(ImmutableMap.copyOf(map), false);
    }

    private static void load(List<Resource> resources, Map<String, String> translationMap) {
        for (Resource resource : resources) {
            try {
                InputStream inputStream = resource.getInputStream();

                try {
                    Objects.requireNonNull(translationMap);
                    Language.load(inputStream, translationMap::put);
                } catch (Throwable var8) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                inputStream.close();
            } catch (IOException var9) {
                LOGGER.warn("Failed to load translations from {}", resource, var9);
            }
        }
    }

    public String get(String key) {
        return (String) this.translations.getOrDefault(key, key);
    }

    public boolean hasTranslation(String key) {
        return this.translations.containsKey(key);
    }

    public boolean isRightToLeft() {
        return this.rightToLeft;
    }

    public OrderedText reorder(StringVisitable text) {
        return ReorderingUtil.reorder(text, this.rightToLeft);
    }
}
