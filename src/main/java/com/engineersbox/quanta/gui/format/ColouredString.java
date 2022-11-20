package com.engineersbox.quanta.gui.format;

import imgui.ImGui;
import imgui.flag.ImGuiCol;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record ColouredString(GUITextColour colour,
                             String value) {
    public static final String NEWLINE_CHARACTER = System.getProperty("line.separator");

    public static void renderFormattedString(final Object ...colouredStringObjects) {
        if (colouredStringObjects == null) {
            return;
        }
        final List<ColouredString> colouredStrings = Arrays.stream(colouredStringObjects)
                .filter(Objects::nonNull)
                .flatMap((final Object object) -> {
                    if (object instanceof ColouredString colouredString) {
                        return Stream.of(colouredString);
                    } else if (object instanceof ColouredString[] colouredStringArray) {
                        return Arrays.stream(colouredStringArray);
                    }
                    return Stream.of();
                }).toList();
        boolean onSameLine = false;
        boolean pushed = false;
        for (final ColouredString colouredString : colouredStrings) {
            if (onSameLine) {
                ImGui.sameLine(0, 0);
            }
            if (colouredString.colour() != null) {
                if (pushed) {
                    ImGui.popStyleColor();
                    pushed = false;
                }
                ImGui.pushStyleColor(
                        ImGuiCol.Text,
                        colouredString.colour().getColour()
                );
                pushed = true;
            }
            if (colouredString.value() != null) {
                ImGui.text(colouredString.value());
                onSameLine = !colouredString.value().endsWith(NEWLINE_CHARACTER);
            } else {
                onSameLine = true;
            }
        }
        if (pushed) {
            ImGui.popStyleColor();
        }
    }
}
