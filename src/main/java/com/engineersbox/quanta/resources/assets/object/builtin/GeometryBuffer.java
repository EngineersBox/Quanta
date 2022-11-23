package com.engineersbox.quanta.resources.assets.object.builtin;

import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;
import org.apache.commons.geometry.euclidean.threed.mesh.TriangleMesh;
import org.apache.commons.geometry.io.core.GeometryFormat;
import org.apache.commons.geometry.io.core.output.GeometryOutput;
import org.apache.commons.geometry.io.core.output.StreamGeometryOutput;
import org.apache.commons.geometry.io.euclidean.threed.GeometryFormat3D;
import org.apache.commons.geometry.io.euclidean.threed.IO3D;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class GeometryBuffer {

    private final TriangleMesh mesh;

    public GeometryBuffer(final TriangleMesh mesh) {
        this.mesh = mesh;
    }

    public ByteBuffer getRaw() {
        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final GeometryOutput output = new StreamGeometryOutput(os);
            final GeometryFormat format = GeometryFormat3D.OBJ;
            IO3D.write(
                    this.mesh,
                    output,
                    format
            );
            final byte[] rawData = os.toByteArray();
            final ByteBuffer data = MemoryUtil.memAlloc(rawData.length);
            data.put(rawData);
            data.flip();
            return data;
        } catch (final IOException e) {
            return null;
        }
    }

    public Model getModel(final String id,
                          final TextureCache textureCache,
                          final MaterialCache materialCache) {
        final ByteBuffer data = getRaw();
        final Model model = ModelLoader.loadModel(
                id,
                data,
                textureCache,
                materialCache,
                false
        );
        MemoryUtil.memFree(data);
        return model;
    }

}
