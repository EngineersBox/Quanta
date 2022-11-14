package com.engineersbox.quanta.resources.assets.object.animation;

import java.util.List;

public record Animation(String name,
                        double duration,
                        List<AnimatedFrame> frames) {
}
