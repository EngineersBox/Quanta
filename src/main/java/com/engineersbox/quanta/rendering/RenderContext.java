package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public record RenderContext(Scene scene,
                            Window window,
                            Map<Object, Object> attributes) {

    public RenderContext(final Scene scene,
                         final Window window) {
        this(
                scene,
                window,
                new HashMap<>()
        );
    }

}
