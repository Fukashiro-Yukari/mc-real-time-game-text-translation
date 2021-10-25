package io.github.nepqneko.texttranslation;

import com.google.common.collect.Maps;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.nepqneko.texttranslation.config.ModConfigManager;
import io.github.nepqneko.texttranslation.translation.CustomTranslationStorage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class RealTimeGameTextTranslation implements ClientModInitializer {
    public static final String NAME = "RealTimeGameTextTranslation";
    public static final String MOD_ID = "texttranslation";
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();

    private static final String TL_GOOGLE = "google translate";
    private static final String TL_MANUAL = "manual translation";

    public static CustomTranslationStorage EnglishTranslations = null;
    public static Map<String, Map<String, String>> TranslationsMap = Maps.newHashMap();
    public static Map<String, String> UntranslatedMap = Maps.newHashMap();

    public static ArrayList<String> TranslationBlockKeys = new ArrayList<>();
    public static ArrayList<String> DontTranslationTexts = new ArrayList<>();

    static {
        TranslationBlockKeys.add("attribute.modifier");
        TranslationBlockKeys.add("potion.with");
        TranslationBlockKeys.add("potion.potency");
        TranslationBlockKeys.add("enchantment.level");
        TranslationBlockKeys.add(".minecraft.");
        TranslationBlockKeys.add("key.keyboard");
        TranslationBlockKeys.add("options.generic_value");
        TranslationBlockKeys.add("options.framerate");
        TranslationBlockKeys.add("menu.online");
        TranslationBlockKeys.add("pack.incompatible.compatible");

        DontTranslationTexts.add("Minecraft");
        DontTranslationTexts.add("Discord");

        UntranslatedMap.put("_comment", "This file is an automatically generated untranslated file, you can rename this file to create your own translation file. File uniformly uses UTF-8 encoding.");
    }

    private static boolean makeDirs() {
        boolean isSuccess = true;
        Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), MOD_ID, TL_GOOGLE);
        File dir = new File(path.toString());

        if (!dir.exists())
            isSuccess = dir.mkdirs();

        Path path2 = Path.of(FabricLoader.getInstance().getConfigDir().toString(), MOD_ID, TL_MANUAL);
        File dir2 = new File(path2.toString());

        if (!dir2.exists())
            isSuccess = dir2.mkdirs();

        return isSuccess;
    }

    public static void saveTranslationFiles() {
        if (!makeDirs()) System.err.println("Couldn't create folder");

        Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), MOD_ID, TL_GOOGLE);

        for (String lang : TranslationsMap.keySet()) {
            File file = new File(path.toFile(), lang + ".json");
            String json = GSON.toJson(TranslationsMap.get(lang));

            try (FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8)) {
                fileWriter.write(json);
            } catch (IOException e) {
                System.err.println("Couldn't save google translation '" + lang + ".json' file");
                e.printStackTrace();
            }
        }

        Path path2 = Path.of(FabricLoader.getInstance().getConfigDir().toString(), MOD_ID, TL_MANUAL);
        File file2 = new File(path2.toFile(), "en_us.json");
        String json = GSON.toJson(UntranslatedMap);

        try (FileWriter fileWriter = new FileWriter(file2, StandardCharsets.UTF_8)) {
            fileWriter.write(json);
        } catch (IOException e) {
            System.err.println("Couldn't save manual translation 'en_us.json' file");
            e.printStackTrace();
        }
    }

    public static void initTranslationFiles() {
        if (!makeDirs()) System.err.println("Couldn't create folder");

        Gson gson = new GsonBuilder().create();
        Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), MOD_ID, TL_GOOGLE);
        File dir = new File(path.toString());
        FilenameFilter filter = (f, name) -> name.endsWith(".json");

        for (String name : Objects.requireNonNull(dir.list(filter))) {
            String lang = name.substring(0, name.indexOf('.'));
            Path filepath = Path.of(path.toFile().toString(), name);
            String json = null;

            try {
                json = Files.readString(filepath);
            } catch (IOException e) {
                System.err.println("Couldn't open google translation '" + name + "' file");
                e.printStackTrace();
            }

            if (json == null) continue;

            Type typeOfHashMap = new TypeToken<Map<String, String>>() {
            }.getType();

            TranslationsMap.put(lang, gson.fromJson(json, typeOfHashMap));
        }

        Path path2 = Path.of(FabricLoader.getInstance().getConfigDir().toString(), MOD_ID, TL_MANUAL);
        Path filepath = Path.of(path2.toFile().toString(), "en_us.json");

        if (!new File(filepath.toString()).exists()) return;

        String json = null;

        try {
            json = Files.readString(filepath);
        } catch (IOException e) {
            System.err.println("Couldn't open manual translation 'en_us.json' file");
            e.printStackTrace();
        }

        if (json == null) return;

        Type typeOfHashMap = new TypeToken<Map<String, String>>() {
        }.getType();

        UntranslatedMap.putAll(gson.fromJson(json, typeOfHashMap));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onInitializeClient() {
        LOGGER.info(NAME + " Initialize");

        ModConfigManager.initializeConfig();
        initTranslationFiles();
    }
}
