package com.engineersbox.quanta.gui.console;

import imgui.ImColor;

public enum ConsoleColour {
    ORANGE(ImColor.rgbToColor("#f59762")),
    YELLOW(ImColor.rgbToColor("#ffd866")),
    RED(ImColor.rgbToColor("#ff6188")),
    CYAN(ImColor.rgbToColor("#78dce8")),
    BLUE(ImColor.rgbToColor("#4763d6")),
    GRAY(ImColor.rgbToColor("#939293")),
    GREEN(ImColor.rgbToColor("#a9dc76")),
    MAGENTA(ImColor.rgbToColor("#ab9df2")),
    NORMAL(ImColor.rgbToColor("#fcfcfa"));

    private int colour;

    ConsoleColour(final int colour) {
        this.colour = colour;
    }

    public int getColour() {
        return this.colour;
    }

}
