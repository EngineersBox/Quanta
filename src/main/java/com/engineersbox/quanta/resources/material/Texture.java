package com.engineersbox.quanta.resources.material;

import com.engineersbox.quanta.resources.config.ConfigHandler;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;

public class Texture {

    private int textureId;
    private final String path;

    public Texture(final int width,
                   final int height,
                   final ByteBuffer data) {
        this.path = "";
        generateTexture(width, height, data);
    }

    public Texture(final String path) {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            this.path = path;
            final IntBuffer w = stack.mallocInt(1);
            final IntBuffer h = stack.mallocInt(1);
            final IntBuffer channels = stack.mallocInt(1);

            final ByteBuffer data = stbi_load(
                    path,
                    w, h,
                    channels,
                    4
            );
            if (data == null) {
                throw new IllegalStateException("Unable to find texture at " + path);
            }
            generateTexture(w.get(), h.get(), data);
            stbi_image_free(data);
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, this.textureId);
    }

    public void cleanup() {
        glDeleteTextures(this.textureId);
    }

    private void generateTexture(final int width,
                                 final int height,
                                 final ByteBuffer data) {
        this.textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        /* MIP MAPPING CONFIGURATIONS
         *
         * Filter Combination                     | Bilinear | Bilinear | Mipmapping
         * (MAG_FILTER / MIN_FILTER)              | (Near)   | (FAR)    |
         * ---------------------------------------+----------+----------+------------
         * GL_NEAREST / GL_NEAREST_MIPMAP_NEAREST | Off      | Off      | Standard
         * GL_NEAREST / GL_LINEAR_MIPMAP_NEAREST  | Off      | On       | Standard
         * GL_NEAREST / GL_NEAREST_MIPMAP_LINEAR  | Off      | Off      | Trilinear filtering
         * GL_NEAREST / GL_LINEAR_MIPMAP_LINEAR   | Off      | On       | Trilinear filtering
         * GL_NEAREST / GL_NEAREST                | Off      | Off      | None
         * GL_NEAREST / GL_LINEAR                 | Off      | On       | None
         * GL_LINEAR / GL_NEAREST_MIPMAP_NEAREST  | On       | Off      | Standard
         * GL_LINEAR / GL_LINEAR_MIPMAP_NEAREST   | On       | On       | Standard
         * GL_LINEAR / GL_NEAREST_MIPMAP_LINEAR   | On       | Off      | Trilinear filtering
         * GL_LINEAR / GL_LINEAR_MIPMAP_LINEAR    | On       | On       | Trilinear filtering
         * GL_LINEAR / GL_NEAREST                 | On       | Off      | None
         * GL_LINEAR / GL_LINEAR                  | On       | On       | None
         */
        final int minFilter = switch (ConfigHandler.CONFIG.render.texture.mipmaps) {
            case NONE -> GL_LINEAR;
            case BILINEAR -> GL_LINEAR_MIPMAP_NEAREST;
            case TRILINEAR -> GL_LINEAR_MIPMAP_LINEAR;
        };
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                data
        );
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    public String getPath() {
        return this.path;
    }

}
