package io.github.nepqneko.texttranslation.gui;

import io.github.nepqneko.texttranslation.RealTimeGameTextTranslation;
import io.github.nepqneko.texttranslation.config.ModConfig;
import io.github.nepqneko.texttranslation.config.ModConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.TranslatableText;

import java.util.List;

// Code from 'Mod Menu'

public class ModOptionsScreen extends GameOptionsScreen {
    private final Screen previous;
    private ButtonListWidget buttonList;

    @SuppressWarnings("resource")
    public ModOptionsScreen(Screen previous) {
        super(previous, MinecraftClient.getInstance().options, new TranslatableText(RealTimeGameTextTranslation.MOD_ID + ".options"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        this.buttonList = new ButtonListWidget(this.client, this.width, this.height, 32, this.height - 32, 25);
        this.buttonList.addAll(ModConfig.asOptions());
        this.addSelectableChild(this.buttonList);
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height - 27, 200, 20, ScreenTexts.DONE, (button) -> {
            ModConfigManager.save();
            assert this.client != null;
            this.client.setScreen(this.previous);
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.buttonList.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 0xffffff);
        super.render(matrices, mouseX, mouseY, delta);
        List<OrderedText> tooltip = getHoveredButtonTooltip(this.buttonList, mouseX, mouseY);
        if (tooltip != null) {
            this.renderOrderedTooltip(matrices, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void removed() {
        ModConfigManager.save();
    }
}
