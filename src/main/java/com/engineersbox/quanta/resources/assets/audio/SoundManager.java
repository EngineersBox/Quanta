package com.engineersbox.quanta.resources.assets.audio;

import com.engineersbox.quanta.rendering.view.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.openal.AL10.alDistanceModel;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SoundManager {

    private final List<SoundBuffer> soundBufferList;
    private final Map<String, SoundSource> soundSourceMap;
    private final long context;
    private final long device;
    private SoundListener listener;

    public SoundManager() {
        this.soundBufferList = new ArrayList<>();
        this.soundSourceMap = new HashMap<>();

        this.device = alcOpenDevice((ByteBuffer) null);
        if (this.device == NULL) {
            throw new IllegalStateException("Failed to open the default OpenAL device.");
        }
        final ALCCapabilities deviceCaps = ALC.createCapabilities(this.device);
        this.context = alcCreateContext(this.device, (IntBuffer) null);
        if (this.context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context.");
        }
        alcMakeContextCurrent(this.context);
        AL.createCapabilities(deviceCaps);
    }

    public void addSoundBuffer(final SoundBuffer soundBuffer) {
        this.soundBufferList.add(soundBuffer);
    }

    public void addSoundSource(final String name, final SoundSource soundSource) {
        this.soundSourceMap.put(name, soundSource);
    }

    public void cleanup() {
        this.soundSourceMap.values().forEach(SoundSource::cleanup);
        this.soundSourceMap.clear();
        this.soundBufferList.forEach(SoundBuffer::cleanup);
        this.soundBufferList.clear();
        if (this.context != NULL) {
            alcDestroyContext(this.context);
        }
        if (this.device != NULL) {
            alcCloseDevice(this.device);
        }
    }

    public SoundListener getListener() {
        return this.listener;
    }

    public SoundSource getSoundSource(final String name) {
        return this.soundSourceMap.get(name);
    }

    public void playSoundSource(final String name) {
        final SoundSource soundSource = this.soundSourceMap.get(name);
        if (soundSource != null && !soundSource.isPlaying()) {
            soundSource.play();
        }
    }

    public void removeSoundSource(final String name) {
        this.soundSourceMap.remove(name);
    }

    public void setAttenuationModel(final int model) {
        alDistanceModel(model);
    }

    public void setListener(final SoundListener listener) {
        this.listener = listener;
    }

    public void updateListenerPosition(final Camera camera) {
        final Matrix4f viewMatrix = camera.getViewMatrix();
        this.listener.setPosition(camera.getPosition());
        final Vector3f at = new Vector3f();
        viewMatrix.positiveZ(at).negate();
        final Vector3f up = new Vector3f();
        viewMatrix.positiveY(up);
        this.listener.setOrientation(at, up);
    }

}
