package com.engineersbox.quanta.gui.format;

import com.engineersbox.quanta.resources.config.ConfigHandler;
import imgui.ImColor;

public enum GUITextColour {
    ORANGE(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.orange)),
    YELLOW(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.yellow)),
    RED(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.red)),
    CYAN(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.cyan)),
    BLUE(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.blue)),
    GRAY(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.gray)),
    DARK_GRAY(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.darkGray)),
    GREEN(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.green)),
    MAGENTA(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.magenta)),
    NORMAL(ImColor.rgbToColor(ConfigHandler.CONFIG.gui.colours.normal));

    private final int colour;

    GUITextColour(final int colour) {
        this.colour = colour;
    }

    public int getColour() {
        return this.colour;
    }

    public ColouredString with(final String value) {
        return new ColouredString(this, value);
    }

    public ColouredString withFormat(final String format, final Object ...values) {
        return with(String.format(
                format,
                values
        ));
    }

}
