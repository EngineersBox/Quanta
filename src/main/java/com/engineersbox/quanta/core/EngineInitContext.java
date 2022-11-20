package com.engineersbox.quanta.core;

import com.engineersbox.quanta.debug.OpenGLInfo;
import com.engineersbox.quanta.debug.PipelineStatistics;
import com.engineersbox.quanta.rendering.Renderer;
import com.engineersbox.quanta.scene.Scene;

public record EngineInitContext(Window window,
                                Scene scene,
                                Renderer renderer,
                                PipelineStatistics pipelineStatistics,
                                OpenGLInfo openGLInfo) {
}
