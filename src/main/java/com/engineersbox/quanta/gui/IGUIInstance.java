package com.engineersbox.quanta.gui;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.scene.Scene;

public interface IGUIInstance {

    void drawGUI();

    boolean handleGUIInput(final Scene scene, final Window window);

}
