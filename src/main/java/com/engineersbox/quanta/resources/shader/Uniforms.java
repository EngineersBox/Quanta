package com.engineersbox.quanta.resources.shader;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class Uniforms {

    private final int programId;
    private final Map<String, Integer> uniforms;

    public Uniforms(final int programId) {
        this.programId = programId;
        this.uniforms = new HashMap<>();
    }

    public void createUniform(final String name) {
        final int location = glGetUniformLocation(this.programId, name);
        if (location < 0) {
            throw new RuntimeException(String.format(
                    "Cannot find uniform \"%s\" in shader %d",
                    name,
                    this.programId
            ));
        }
        this.uniforms.put(name, location);
    }

    public void setUniform(final String name,
                           final Matrix4f value) {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final Integer location = this.uniforms.get(name);
            if (location == null) {
                throw new RuntimeException(String.format(
                        "No such uniform \"%s\"",
                        name
                ));
            }
            glUniformMatrix4fv(
                    location,
                    false,
                    value.get(stack.mallocFloat(16))
            );
        }
    }

    public void setUniform(final String name,
                           final int value) {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final Integer location = this.uniforms.get(name);
            if (location == null) {
                throw new RuntimeException(String.format(
                        "No such uniform \"%s\"",
                        name
                ));
            }
            final IntBuffer intBuffer = stack.mallocInt(1);
            intBuffer.put(value);
            glUniform1iv(location, intBuffer);
        }
    }

}