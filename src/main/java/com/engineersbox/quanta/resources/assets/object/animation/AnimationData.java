package com.engineersbox.quanta.resources.assets.object.animation;

import com.engineersbox.quanta.resources.loader.ModelLoader;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Matrix4f;

import java.util.Arrays;

public class AnimationData {

    public static final Matrix4f[] DEFAULT_BONES_MATRICES = new Matrix4f[ModelLoader.MAX_BONES];

    static {
        final Matrix4f zeroMatrix = new Matrix4f().zero();
        Arrays.fill(AnimationData.DEFAULT_BONES_MATRICES, zeroMatrix);
    }

    private Animation currentAnimation;
    private int currentFrameIdx;

    public AnimationData(final Animation currentAnimation) {
        this(currentAnimation, 0);
    }

    @JsonCreator
    public AnimationData(@JsonProperty("current_animation") final Animation currentAnimation,
                         @JsonProperty("current_frame_index") final int currentFrameIdx) {
        this.currentAnimation = currentAnimation;
        this.currentFrameIdx = currentFrameIdx;
    }

    @JsonProperty("current_animation")
    public Animation getCurrentAnimation() {
        return this.currentAnimation;
    }

    public AnimatedFrame getCurrentFrame() {
        return this.currentAnimation.frames().get(this.currentFrameIdx);
    }

    @JsonProperty("current_frame_index")
    public int getCurrentFrameIdx() {
        return this.currentFrameIdx;
    }

    public void nextFrame() {
        final int nextFrame = this.currentFrameIdx + 1;
        this.currentFrameIdx = nextFrame > this.currentAnimation.frames().size() - 1
                ? 0
                : nextFrame;
    }

    public void setCurrentAnimation(final Animation currentAnimation) {
        this.currentFrameIdx = 0;
        this.currentAnimation = currentAnimation;
    }
}
