package com.engineersbox.quanta.gui.console.hooks;

import com.engineersbox.quanta.gui.console.format.ColouredString;

public class HookValidationException extends RuntimeException {

    private final transient ColouredString[] formattedMessage;

    public HookValidationException(final ColouredString[] formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    public ColouredString[] getFormattedMessage() {
        return this.formattedMessage;
    }

}
