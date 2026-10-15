package com.tozyx.blocksidesound.client.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

/** Vanilla scrolling, focus navigation and narration for one control per row. */
final class SettingsList extends ContainerObjectSelectionList<SettingsList.Row> {
    SettingsList(Minecraft minecraft, int width, int height, int top) {
        super(minecraft, width, height, top, 26);
        centerListVertically = false;
    }

    void add(AbstractWidget widget) {
        addEntry(new Row(widget));
    }

    @Override
    public int getRowWidth() {
        return Math.min(360, getWidth() - 48);
    }

    @Override
    protected int getScrollbarPosition() {
        return getWidth() / 2 + getRowWidth() / 2 + 6;
    }

    static final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final AbstractWidget widget;

        Row(AbstractWidget widget) {
            this.widget = widget;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            widget.setPosition(left, top + 2);
            widget.setWidth(width);
            widget.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(widget);
        }
    }
}
