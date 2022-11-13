package com.engineersbox.quanta.resources.assets.shader;

import com.engineersbox.quanta.utils.FileUtils;
import org.lwjgl.opengl.GL30;

import java.util.List;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {

    private final int programId;

    public ShaderProgram(final ShaderModuleData... shaderModuleData) {
        this(List.of(shaderModuleData));
    }

    public ShaderProgram(final List<ShaderModuleData> shaderModuleData) {
        this.programId = glCreateProgram();
        if (this.programId == 0) {
            throw new RuntimeException("Unable to create a new shader program");
        }
        final List<Integer> moduleIds = shaderModuleData.stream()
                .map((final ShaderModuleData data) -> createShader(FileUtils.readFile(data.file()), data.type()))
                .toList();
        link(moduleIds);
    }

    protected int createShader(final String code,
                               final int type) {
        final int shaderId = glCreateShader(type);
        if (shaderId == 0) {
            throw new RuntimeException(String.format(
                    "[SHADER PROGRAM] Error while creating shader of type %d",
                    type
            ));
        }
        glShaderSource(shaderId, code);
        glCompileShader(shaderId);
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException(String.format(
                    "[SHADER PROGRAM] Error while compiling shader: %s",
                    glGetShaderInfoLog(shaderId, 1024)
            ));
        }
        glAttachShader(this.programId, shaderId);
        return shaderId;
    }

    private void link(final List<Integer> moduleIds) {
        glLinkProgram(this.programId);
        if (glGetProgrami(this.programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException(String.format(
                    "[SHADER PROGRAM] Error while linking shader: %s",
                    glGetProgramInfoLog(this.programId, 1024)
            ));
        }
        moduleIds.forEach((final Integer id) -> glDetachShader(this.programId, id));
        moduleIds.forEach(GL30::glDeleteShader);
    }

    public void bind() {
        glUseProgram(this.programId);
    }

    public static void unbind() {
        glUseProgram(0);
    }

    public void validate() {
        glValidateProgram(this.programId);
        if (glGetProgrami(this.programId, GL_VALIDATE_STATUS) == 0) {
            throw new RuntimeException(String.format(
                    "[SHADER PROGRAM] Error while validating shader program: %s",
                    glGetProgramInfoLog(this.programId, 1024)
            ));
        }
    }

    public int getProgramId() {
        return this.programId;
    }

    public void cleanup() {
        ShaderProgram.unbind();
        if (this.programId != 0) {
            glDeleteProgram(this.programId);
        }
    }

}
