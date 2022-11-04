package com.engineersbox.quanta.core;

import com.engineersbox.quanta.graph.Render;
import com.engineersbox.quanta.scene.Scene;

public interface IAppLogic {

    void cleanup();

    void init(final Window window,
              final Scene scene,
              final Render render);

    void input(final Window window,
               final Scene scene,
               final long diffTimeMillis);

    void update(final Window window,
                final Scene scene,
                final long diffTimeMillis);

}
