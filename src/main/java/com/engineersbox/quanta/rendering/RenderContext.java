package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.buffers.GBuffer;
import com.engineersbox.quanta.rendering.buffers.HDRBuffer;
import com.engineersbox.quanta.rendering.buffers.SSAOBuffer;
import com.engineersbox.quanta.rendering.indirect.AnimationRenderBuffers;
import com.engineersbox.quanta.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public record RenderContext(Scene scene,
                            Window window,
                            AnimationRenderBuffers animationRenderBuffers,
                            GBuffer gBuffer,
                            HDRBuffer hdrBuffer,
                            SSAOBuffer ssaoBuffer,
                            Map<Object, Object> attributes) {

    public RenderContext(final Scene scene,
                         final Window window,
                         final AnimationRenderBuffers animationRenderBuffers,
                         final GBuffer gBuffer,
                         final HDRBuffer hdrBuffer,
                         SSAOBuffer ssaoBuffer) {
        this(
                scene,
                window,
                animationRenderBuffers,
                gBuffer,
                hdrBuffer,
                ssaoBuffer,
                new HashMap<>()
        );
    }

}
