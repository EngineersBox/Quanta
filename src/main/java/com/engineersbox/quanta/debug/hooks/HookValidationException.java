package com.engineersbox.quanta.debug.hooks;

import com.engineersbox.quanta.gui.format.ColouredString;
import com.engineersbox.quanta.gui.format.GUITextColour;

public class HookValidationException extends RuntimeException {

    private final transient ColouredString[] formattedMessage;

    public HookValidationException(final String message) {
        this(new ColouredString[]{new ColouredString(
                GUITextColour.NORMAL,
                message
        )});
    }

    public HookValidationException(final ColouredString[] formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    public ColouredString[] getFormattedMessage() {
        return this.formattedMessage;
    }

}
