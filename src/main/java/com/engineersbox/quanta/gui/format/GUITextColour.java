package com.engineersbox.quanta.gui.format;

import imgui.ImColor;

public enum GUITextColour {
    ORANGE(ImColor.rgbToColor("#f59762")),
    YELLOW(ImColor.rgbToColor("#ffd866")),
    RED(ImColor.rgbToColor("#ff6188")),
    CYAN(ImColor.rgbToColor("#78dce8")),
    BLUE(ImColor.rgbToColor("#4763d6")),
    GRAY(ImColor.rgbToColor("#939293")),
    DARK_GRAY(ImColor.rgbToColor("#636263")),
    GREEN(ImColor.rgbToColor("#a9dc76")),
    MAGENTA(ImColor.rgbToColor("#ab9df2")),
    NORMAL(ImColor.rgbToColor("#fcfcfa"));

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

}
