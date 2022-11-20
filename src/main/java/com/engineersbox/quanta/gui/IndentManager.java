package com.engineersbox.quanta.gui;

import imgui.ImGui;

import java.util.ArrayDeque;

public class IndentManager extends ArrayDeque<Float> {

    public IndentManager() {
        super();
    }

    @Override
    public void push(final Float indent) {
        super.push(indent);
        ImGui.indent(indent);
    }

    @Override
    public Float pop() {
        final float indent = super.pop();
        ImGui.unindent(indent);
        return indent;
    }

    public Float popN(final int n) {
        float totalIndent = 0;
        for (int i = 0; i < n; i++) {
            totalIndent += pop();
        }
        return totalIndent;
    }
}
