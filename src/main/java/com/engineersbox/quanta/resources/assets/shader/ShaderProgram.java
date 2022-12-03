package com.engineersbox.quanta.resources.assets.shader;

import com.engineersbox.quanta.utils.EnumSetUtils;
import com.engineersbox.quanta.utils.FileUtils;
import org.lwjgl.opengl.GL30;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {

    private final String name;
    private final int programId;
    private final Uniforms uniforms;
    private boolean bound;

    public ShaderProgram(final String name,
                         final ShaderModuleData... shaderModuleData) {
        this(name, List.of(shaderModuleData));
    }

    public ShaderProgram(final String name,
                         final List<ShaderModuleData> shaderModuleData) {
        this.name = name;
        this.bound = false;
        validateUniqueShaderTypes(shaderModuleData);
        this.programId = glCreateProgram();
        if (this.programId == 0) {
            throw new RuntimeException("Unable to create a new shader program");
        }
        final List<Integer> moduleIds = shaderModuleData.stream()
                .map((final ShaderModuleData data) -> createShader(
                        FileUtils.readFile(data.file()),
                        data.type()
                )).toList();
        link(moduleIds);
        this.uniforms = new Uniforms(this.programId);
    }

    private void validateUniqueShaderTypes(final List<ShaderModuleData> shaderModuleData) {
        final Map<ShaderType, Long> counts = shaderModuleData.stream()
                .map(ShaderModuleData::type)
                .collect(EnumSetUtils.counting(ShaderType.class));
        final Optional<Map.Entry<ShaderType, Long>> possibleMultipleTypeBinding = counts.entrySet()
                .stream()
                .filter((final Map.Entry<ShaderType, Long> entry) -> entry.getValue() > 1)
                .findFirst();
        if (possibleMultipleTypeBinding.isEmpty()) {
            return;
        }
        final Map.Entry<ShaderType, Long> multipleTypeBinding = possibleMultipleTypeBinding.get();
        throw new IllegalStateException(String.format(
                "Program was bound with %d instances of %s, only 1 is supported",
                multipleTypeBinding.getValue(),
                multipleTypeBinding.getKey().name()
        ));
    }

    protected int createShader(final String code,
                               final ShaderType type) {
        final int shaderId = glCreateShader(type.getType());
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
        if (this.bound) {
            throw new IllegalStateException("Shader already bound");
        }
        glUseProgram(this.programId);
        this.bound = true;
    }

    public void unbind() {
        if (!this.bound) {
            throw new IllegalStateException("Shader is not currently bound");
        }
        glUseProgram(0);
        this.bound = false;
    }

    public ShaderValidationState validate() {
        glValidateProgram(this.programId);
        if (glGetProgrami(this.programId, GL_VALIDATE_STATUS) == 0) {
            return new ShaderValidationState(
                    false,
                    glGetProgramInfoLog(this.programId, 1024)
            );
        }
        return new ShaderValidationState(true, null);
    }

    public int getProgramId() {
        return this.programId;
    }

    public Uniforms getUniforms() {
        return this.uniforms;
    }

    public String getName() {
        return this.name;
    }

    public void cleanup() {
        if (this.programId != 0) {
            glDeleteProgram(this.programId);
        }
    }

}
