package com.tozyx.blocksidesound.client.gui;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Quantized vanilla slider; arrow keys offer precise one-step adjustments. */
final class SettingSlider extends AbstractSliderButton {
    private final Component label;
    private final double minimum;
    private final double maximum;
    private final double step;
    private final DoubleConsumer setter;
    private final DoubleFunction<Component> formatter;

    SettingSlider(Component label, double initial, double minimum, double maximum, double step,
                  DoubleConsumer setter, DoubleFunction<Component> formatter) {
        super(0, 0, 310, 20, Component.empty(), (initial - minimum) / (maximum - minimum));
        this.label = label;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.setter = setter;
        this.formatter = formatter;
        updateMessage();
    }

    private double settingValue() {
        return Math.clamp(minimum + Math.round(value * (maximum - minimum) / step) * step,
                minimum, maximum);
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable("options.generic_value", label, formatter.apply(settingValue())));
    }

    @Override
    protected void applyValue() {
        setter.accept(settingValue());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263 || keyCode == 262) {
            double next = Math.clamp(settingValue() + (keyCode == 263 ? -step : step), minimum, maximum);
            value = (next - minimum) / (maximum - minimum);
            applyValue();
            updateMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
