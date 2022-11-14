package com.engineersbox.quanta.resources.assets.audio;

import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;

public class SoundSource {

    private final int sourceId;

    public SoundSource(final boolean loop, final boolean relative) {
        this.sourceId = alGenSources();
        alSourcei(this.sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        alSourcei(this.sourceId, AL_SOURCE_RELATIVE, relative ? AL_TRUE : AL_FALSE);
    }

    public void cleanup() {
        stop();
        alDeleteSources(this.sourceId);
    }

    public boolean isPlaying() {
        return alGetSourcei(this.sourceId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void pause() {
        alSourcePause(this.sourceId);
    }

    public void play() {
        alSourcePlay(this.sourceId);
    }

    public void setBuffer(final int bufferId) {
        stop();
        alSourcei(this.sourceId, AL_BUFFER, bufferId);
    }

    public void setGain(final float gain) {
        alSourcef(this.sourceId, AL_GAIN, gain);
    }

    public void setPosition(final Vector3f position) {
        alSource3f(this.sourceId, AL_POSITION, position.x, position.y, position.z);
    }

    public void stop() {
        alSourceStop(this.sourceId);
    }
}
