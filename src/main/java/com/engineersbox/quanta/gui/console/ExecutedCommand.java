package com.engineersbox.quanta.gui.console;

import java.util.Date;

public record ExecutedCommand(Date date,
                              ColouredString[] command,
                              ColouredString[] result) {

    public static ExecutedCommand from(final ColouredString[] command,
                                       final ColouredString[] result) {
        return new ExecutedCommand(
                new Date(),
                command,
                result
        );
    }

    public static ExecutedCommand from(final ColouredString command,
                                       final ColouredString[] result) {
        return ExecutedCommand.from(
                new ColouredString[]{command},
                result
        );
    }

    public static ExecutedCommand from(final ColouredString[] command,
                                       final ColouredString result) {
        return ExecutedCommand.from(
                command,
                new ColouredString[]{result}
        );
    }

    public static ExecutedCommand from(final ColouredString command,
                                       final ColouredString result) {
        return ExecutedCommand.from(
                command,
                new ColouredString[]{result}
        );
    }

}
