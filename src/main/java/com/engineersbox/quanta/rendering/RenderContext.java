package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.buffers.GBuffer;
import com.engineersbox.quanta.rendering.buffers.HDRBuffer;
import com.engineersbox.quanta.rendering.buffers.SSAOBuffer;
import com.engineersbox.quanta.rendering.indirect.RenderBuffers;
import com.engineersbox.quanta.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public record RenderContext(Scene scene,
                            Window window,
                            RenderBuffers renderBuffers,
                            GBuffer gBuffer,
                            HDRBuffer hdrBuffer,
                            SSAOBuffer ssaoBuffer,
                            Map<Object, Object> attributes) {

    public RenderContext(final Scene scene,
                         final Window window,
                         final RenderBuffers renderBuffers,
                         final GBuffer gBuffer,
                         final HDRBuffer hdrBuffer,
                         SSAOBuffer ssaoBuffer) {
        this(
                scene,
                window,
                renderBuffers,
                gBuffer,
                hdrBuffer,
                ssaoBuffer,
                new HashMap<>()
        );
    }

}
