package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.rendering.deferred.GBuffer;
import com.engineersbox.quanta.rendering.indirect.RenderBuffers;
import com.engineersbox.quanta.scene.Scene;

public record RenderContext(Scene scene,
                            RenderBuffers renderBuffers,
                            GBuffer gBudder,
                            ShadowRenderer shadowRenderer) {
}
