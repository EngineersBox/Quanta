package com.engineersbox.quanta.gui.console;

import com.engineersbox.quanta.gui.format.ColouredString;

public record ValidationState(boolean state,
                              ColouredString[] message) {
}
