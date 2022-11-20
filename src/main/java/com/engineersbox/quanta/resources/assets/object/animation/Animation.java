package com.engineersbox.quanta.resources.assets.object.animation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Animation(String name,
                        double duration,
                        List<AnimatedFrame> frames) {
}
