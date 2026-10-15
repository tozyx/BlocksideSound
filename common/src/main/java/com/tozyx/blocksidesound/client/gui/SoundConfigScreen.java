package com.tozyx.blocksidesound.client.gui;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.BlocksideSoundClient;
import com.tozyx.blocksidesound.client.config.BlockSelector;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import com.tozyx.blocksidesound.client.rain.RainMaterial;
import com.tozyx.blocksidesound.client.weather.WeatherSoundPolicy;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** A shared settings screen built entirely from Minecraft's own widgets. */
public final class SoundConfigScreen extends Screen {
    private enum Page { GENERAL, WEATHER, MATERIALS, MATERIAL, ADVANCED, EXCLUSIONS, OVERRIDES }

    private final Screen parent;
    private SoundConfig draft = BlocksideSoundClient.configSnapshot();
    private Page page = Page.GENERAL;
    private String materialId = "wood";
    private String selectorText = "";
    private RainMaterial overrideTarget = RainMaterial.WOOD;
    private SettingsList settings;
    private Component status;
    private double scroll;

    public SoundConfigScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int tabWidth = Math.min(96, (width - 40) / 4);
        int start = (width - tabWidth * 4 - 12) / 2;
        tab("general", Page.GENERAL, start, tabWidth);
        tab("weather", Page.WEATHER, start + tabWidth + 4, tabWidth);
        tab("materials", Page.MATERIALS, start + 2 * (tabWidth + 4), tabWidth);
        tab("advanced", Page.ADVANCED, start + 3 * (tabWidth + 4), tabWidth);

        settings = addRenderableWidget(new SettingsList(minecraft, width, Math.max(26, height - 134), 68));
        switch (page) {
            case GENERAL -> general();
            case WEATHER -> weather();
            case MATERIALS -> materials();
            case MATERIAL -> material();
            case ADVANCED -> advanced();
            case EXCLUSIONS, OVERRIDES -> rules();
        }
        settings.setScrollAmount(scroll);

        int buttonWidth = Math.min(116, (width - 32) / 3);
        int left = (width - buttonWidth * 3 - 8) / 2;
        addRenderableWidget(Button.builder(text("save"), button -> save())
                .bounds(left, height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> minecraft.setScreen(parent))
                .bounds(left + buttonWidth + 4, height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(text("defaults"), button -> reset())
                .bounds(left + 2 * (buttonWidth + 4), height - 28, buttonWidth, 20).build());
    }

    private void tab(String label, Page target, int x, int tabWidth) {
        Button button = addRenderableWidget(Button.builder(text(label), ignored -> navigate(target))
                .bounds(x, 28, tabWidth, 20).build());
        Page category = switch (page) {
            case MATERIAL, EXCLUSIONS -> Page.MATERIALS;
            case OVERRIDES -> Page.ADVANCED;
            default -> page;
        };
        button.active = category != target;
    }

    private void general() {
        toggle("enabled", draft.enabled, value -> draft.enabled = value);
        percent("volume", draft.volume, value -> draft.volume = value);
        toggle("rain_enabled", draft.rain.enabled, value -> draft.rain.enabled = value);
        percent("rain_volume", draft.rain.volume, value -> draft.rain.volume = value);
        link(text("materials"), () -> navigate(Page.MATERIALS), "materials_help");
    }

    private void weather() {
        toggle("replace_rain", draft.weather.rain.enabled, value -> draft.weather.rain.enabled = value);
        percent("ambience_volume", draft.weather.rain.volume, value -> draft.weather.rain.volume = value);
        resourceStatus("rain_assets", WeatherSoundPolicy.Event.RAIN, WeatherSoundPolicy.Event.RAIN_ABOVE);
        toggle("replace_thunder", draft.weather.thunder.enabled, value -> draft.weather.thunder.enabled = value);
        percent("thunder_volume", draft.weather.thunder.volume, value -> draft.weather.thunder.volume = value);
        resourceStatus("thunder_assets", WeatherSoundPolicy.Event.THUNDER);
    }

    private void resourceStatus(String key, WeatherSoundPolicy.Event... events) {
        int ready = 0;
        for (WeatherSoundPolicy.Event event : events) {
            var sound = minecraft.getSoundManager().getSoundEvent(event.replacement());
            if (sound != null && sound.getWeight() > 0) ready++;
        }
        Button label = Button.builder(text(key, ready, events.length), ignored -> {}).width(310).build();
        label.active = false;
        label.setTooltip(Tooltip.create(text("weather_assets_help")));
        settings.add(label);
    }

    private void materials() {
        for (RainMaterial type : RainMaterial.values()) {
            if (type == RainMaterial.VANILLA) continue;
            SoundConfig.Material material = draft.rain.materials.get(type.id());
            Component label = Component.translatable("options.generic_value", materialName(type.id()),
                    material.enabled ? percentValue(material.volume) : CommonComponents.OPTION_OFF);
            link(label, () -> openMaterial(type.id()), "material_help");
        }
        link(materialName("plant_drip"), () -> openMaterial("plant_drip"), "plant_help");
    }

    private void openMaterial(String id) {
        materialId = id;
        navigate(Page.MATERIAL);
    }

    private SoundConfig.Material currentMaterial() {
        return materialId.equals("plant_drip") ? draft.rain.plantDrips : draft.rain.materials.get(materialId);
    }

    private void material() {
        SoundConfig.Material material = currentMaterial();
        link(text("back_materials"), () -> navigate(Page.MATERIALS), null);
        toggle("material_enabled", material.enabled, value -> material.enabled = value);
        percent("material_volume", material.volume, value -> material.volume = value);
        percent("probability", material.triggerProbability, value -> material.triggerProbability = value);
        integer("interval", material.attemptIntervalTicks, 1, 1200, value -> material.attemptIntervalTicks = value);
        link(text("exclusions_count", material.exclude.size()), () -> openRules(Page.EXCLUSIONS), "exclusions_help");
    }

    private void advanced() {
        integer("max_sounds", draft.rain.maxActiveSounds, 1, 32, value -> draft.rain.maxActiveSounds = value);
        integer("radius", draft.rain.radius, 4, 32, value -> draft.rain.radius = value);
        integer("vertical", draft.rain.verticalRange, 4, 64, value -> draft.rain.verticalRange = value);
        integer("scan_interval", draft.rain.scanIntervalTicks, 1, 40, value -> draft.rain.scanIntervalTicks = value);
        integer("scan_budget", draft.rain.scanBudget, 1, 256, value -> draft.rain.scanBudget = value);
        toggle("debug_logging", draft.rain.debugLogging, value -> draft.rain.debugLogging = value);
        toggle("debug_particles", draft.rain.debugParticles, value -> draft.rain.debugParticles = value);
        link(text("overrides_count", draft.rain.overrides.size()), () -> openRules(Page.OVERRIDES), "overrides_help");
    }

    private void openRules(Page target) {
        selectorText = "";
        navigate(target);
    }

    private void rules() {
        boolean overrides = page == Page.OVERRIDES;
        link(text("back"), () -> navigate(overrides ? Page.ADVANCED : Page.MATERIAL), null);
        EditBox input = new EditBox(font, 0, 0, 310, 20, text("selector"));
        input.setMaxLength(256);
        input.setHint(Component.literal("minecraft:oak_planks / #minecraft:logs"));
        input.setValue(selectorText);
        input.setResponder(value -> selectorText = value);
        input.setTooltip(Tooltip.create(text("selector_help")));
        settings.add(input);
        if (overrides) {
            settings.add(CycleButton.<RainMaterial>builder(type -> materialName(type.id()))
                    .withValues(RainMaterial.values()).withInitialValue(overrideTarget)
                    .create(0, 0, 310, 20, text("target"), (button, value) -> overrideTarget = value));
        }
        link(text("add_rule"), this::addRule, "add_rule_help");
        if (overrides) {
            for (int index = 0; index < draft.rain.overrides.size(); index++) {
                int position = index;
                SoundConfig.Override rule = draft.rain.overrides.get(index);
                Component label = text("remove_override", index + 1, rule.match, materialName(rule.material));
                link(label, () -> {
                    draft.rain.overrides.remove(position);
                    refresh();
                }, "remove_rule_help");
            }
        } else {
            List<String> excluded = currentMaterial().exclude;
            for (int index = 0; index < excluded.size(); index++) {
                int position = index;
                link(text("remove_exclusion", excluded.get(index)), () -> {
                    excluded.remove(position);
                    refresh();
                }, "remove_rule_help");
            }
        }
    }

    private void addRule() {
        String selector = selectorText.trim();
        try {
            BlockSelector.parse(selector);
        } catch (IllegalArgumentException exception) {
            status = text("invalid_selector");
            return;
        }
        if (page == Page.OVERRIDES) {
            if (draft.rain.overrides.stream().anyMatch(rule -> rule.match.equals(selector))) {
                status = text("duplicate_rule");
                return;
            }
            SoundConfig.Override rule = new SoundConfig.Override();
            rule.match = selector;
            rule.material = overrideTarget.id();
            draft.rain.overrides.add(rule);
        } else {
            if (currentMaterial().exclude.contains(selector)) {
                status = text("duplicate_rule");
                return;
            }
            currentMaterial().exclude.add(selector);
        }
        selectorText = "";
        status = null;
        refresh();
    }

    private void toggle(String key, boolean initial, Consumer<Boolean> setter) {
        control(CycleButton.onOffBuilder(initial).create(0, 0, 310, 20, text(key),
                (button, value) -> setter.accept(value)), key);
    }

    private void percent(String key, double initial, DoubleConsumer setter) {
        control(new SettingSlider(text(key), initial, 0.0, 1.0, 0.01, setter,
                SoundConfigScreen::percentValue), key);
    }

    private void integer(String key, int initial, int min, int max, IntConsumer setter) {
        control(new SettingSlider(text(key), initial, min, max, 1.0, value -> setter.accept((int) value),
                value -> key.equals("interval") || key.equals("scan_interval")
                        ? text("ticks", (int) value, String.format(Locale.ROOT, "%.2f", value / 20.0))
                        : Component.literal(Integer.toString((int) value))), key);
    }

    private void control(AbstractWidget widget, String key) {
        widget.setTooltip(Tooltip.create(text(key + "_help")));
        settings.add(widget);
    }

    private void link(Component label, Runnable action, String tooltip) {
        Button button = Button.builder(label, ignored -> action.run()).width(310).build();
        if (tooltip != null) button.setTooltip(Tooltip.create(text(tooltip).copy().append("\n").append(label)));
        settings.add(button);
    }

    private void navigate(Page target) {
        page = target;
        scroll = 0;
        status = null;
        rebuildWidgets();
    }

    private void refresh() {
        scroll = settings.getScrollAmount();
        rebuildWidgets();
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        if (settings != null) scroll = settings.getScrollAmount();
        super.resize(minecraft, width, height);
    }

    private void save() {
        try {
            BlocksideSoundClient.saveConfig(draft);
            minecraft.setScreen(parent);
        } catch (IOException | RuntimeException exception) {
            BlocksideSound.LOGGER.error("Cannot save client sound settings", exception);
            status = text("save_failed");
        }
    }

    private void reset() {
        scroll = settings.getScrollAmount();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                draft = new SoundConfig();
                status = null;
                scroll = 0;
            }
            minecraft.setScreen(this);
        }, text("defaults"), text("defaults_confirm")));
    }

    @Override
    public void onClose() {
        switch (page) {
            case MATERIAL -> navigate(Page.MATERIALS);
            case EXCLUSIONS -> navigate(Page.MATERIAL);
            case OVERRIDES -> navigate(Page.ADVANCED);
            default -> minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 11, 0xFFFFFF);
        Component subtitle = switch (page) {
            case MATERIAL -> materialName(materialId);
            case EXCLUSIONS -> text("exclusions_title", materialName(materialId));
            case OVERRIDES -> text("overrides_title");
            default -> text("draft_hint");
        };
        graphics.drawCenteredString(font, subtitle, width / 2, 54, 0xAAAAAA);
        Component message = status != null ? status
                : text(page == Page.WEATHER ? "weather_setup_hint" : "weather_hint");
        int y = height - 61;
        for (var line : font.split(message, width - 24).stream().limit(2).toList()) {
            graphics.drawCenteredString(font, line, width / 2, y, status == null ? 0xAAAAAA : 0xFF7777);
            y += 10;
        }
    }

    static Component text(String key, Object... arguments) {
        return Component.translatable("blocksidesound.config." + key, arguments);
    }

    private static Component percentValue(double value) {
        return Component.literal(Math.round(value * 100) + "%");
    }

    private static Component materialName(String id) {
        return Component.translatable("blocksidesound.material." + id);
    }
}
