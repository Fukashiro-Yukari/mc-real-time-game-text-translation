package io.github.nepqneko.texttranslation.config;

import com.terraformersmc.modmenu.config.option.BooleanConfigOption;
import com.terraformersmc.modmenu.config.option.OptionConvertable;
import net.minecraft.client.option.Option;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

// Code from 'Mod Menu'

public class ModConfig {
    public static final BooleanConfigOption ENABLE_DEBUG = new BooleanConfigOption("enable_debug", false);
    public static final BooleanConfigOption SHOW_ORIGINAL_TEXT = new BooleanConfigOption("show_original_text", false);

    public static Option[] asOptions() {
        ArrayList<Option> options = new ArrayList<>();
        for (Field field : ModConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && OptionConvertable.class.isAssignableFrom(field.getType())) {
                try {
                    options.add(((OptionConvertable) field.get(null)).asOption());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return options.toArray(Option[]::new);
    }
}
