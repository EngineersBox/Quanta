package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.deferred.GBuffer;
import com.engineersbox.quanta.rendering.hdr.HDRBuffer;
import com.engineersbox.quanta.rendering.indirect.RenderBuffers;
import com.engineersbox.quanta.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public record RenderContext(Scene scene,
                            Window window,
                            RenderBuffers renderBuffers,
                            GBuffer gBuffer,
                            HDRBuffer hdrBuffer,
                            Map<Object, Object> attributes) {

    public RenderContext(final Scene scene,
                         final Window window,
                         final RenderBuffers renderBuffers,
                         final GBuffer gBuffer,
                         final HDRBuffer hdrBuffer) {
        this(
                scene,
                window,
                renderBuffers,
                gBuffer,
                hdrBuffer,
                new HashMap<>()
        );
    }

}
