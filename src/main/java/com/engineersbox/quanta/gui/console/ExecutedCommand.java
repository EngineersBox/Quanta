package com.engineersbox.quanta.gui.console;

import java.util.Date;

public record ExecutedCommand(Date date,
                              String command,
                              String result) {
}
