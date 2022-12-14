package com.engineersbox.quanta.core;

import com.engineersbox.quanta.rendering.Renderer;
import com.engineersbox.quanta.scene.Scene;

public interface IAppLogic {

    void cleanup();

    void init(final EngineInitContext context);

    void input(final Window window,
               final Scene scene,
               final long diffTimeMillis,
               final boolean inputConsumed);

    void update(final Window window,
                final Scene scene,
                final long diffTimeMillis);

}
