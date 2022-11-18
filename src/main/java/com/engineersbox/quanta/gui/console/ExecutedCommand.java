package com.engineersbox.quanta.gui.console;

import com.engineersbox.quanta.gui.console.format.ColouredString;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

public record ExecutedCommand(Date date,
                              ColouredString[] command,
                              ColouredString[] result) {

    public String joinedCommand() {
        return Arrays.stream(command)
                .map(ColouredString::value)
                .collect(Collectors.joining());
    }

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
