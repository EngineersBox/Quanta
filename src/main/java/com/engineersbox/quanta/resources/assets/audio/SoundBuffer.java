package com.engineersbox.quanta.resources.assets.audio;

import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SoundBuffer {

    private final int bufferId;

    private final ShortBuffer pcm;

    public SoundBuffer(final String filePath) {
        this.bufferId = alGenBuffers();
        try (final STBVorbisInfo info = STBVorbisInfo.malloc()) {
            this.pcm = readVorbis(filePath, info);
            // Copy to buffer
            alBufferData(this.bufferId, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, this.pcm, info.sample_rate());
        }
    }

    public void cleanup() {
        alDeleteBuffers(this.bufferId);
        if (this.pcm != null) {
            MemoryUtil.memFree(this.pcm);
        }
    }

    public int getBufferId() {
        return this.bufferId;
    }

    private ShortBuffer readVorbis(final String filePath, final STBVorbisInfo info) {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer error = stack.mallocInt(1);
            final long decoder = stb_vorbis_open_filename(filePath, error, null);
            if (decoder == NULL) {
                throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0));
            }
            stb_vorbis_get_info(decoder, info);
            final int channels = info.channels();
            final int lengthSamples = stb_vorbis_stream_length_in_samples(decoder);
            final ShortBuffer result = MemoryUtil.memAllocShort(lengthSamples * channels);
            result.limit(stb_vorbis_get_samples_short_interleaved(decoder, channels, result) * channels);
            stb_vorbis_close(decoder);
            return result;
        }
    }

}
