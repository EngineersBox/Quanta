package com.engineersbox.quanta.debug;

import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.ARBPipelineStatisticsQuery.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glGetQueryObjectuiv;

public class PipelineStatistics {

    public enum Stat {
        VERTICES_SUBMITTED(0, GL_VERTICES_SUBMITTED_ARB),
        TRIANGLES_SUBMITTED(-1, 0, true, Stat.VERTICES_SUBMITTED, 0.3f),
        PRIMITIVES_SUBMITTED(1, GL_PRIMITIVES_SUBMITTED_ARB),
        VERTEX_SHADER_INVOCATIONS(2, GL_VERTEX_SHADER_INVOCATIONS_ARB),
        TESS_CONTROL_SHADER_PATCHES(3, GL_TESS_CONTROL_SHADER_PATCHES_ARB),
        TESS_EVALUATION_SHADER_INVOCATIONS(4, GL_TESS_EVALUATION_SHADER_INVOCATIONS_ARB),
        GEOMETRY_SHADER_INVOCATIONS(5, GL_GEOMETRY_SHADER_INVOCATIONS),
        GEOMETRY_SHADER_PRIMITIVES_EMITTED(6, GL_GEOMETRY_SHADER_PRIMITIVES_EMITTED_ARB),
        FRAGMENT_SHADER_INVOCATIONS(7, GL_FRAGMENT_SHADER_INVOCATIONS_ARB),
        COMPUTE_SHADER_INVOCATIONS(8, GL_COMPUTE_SHADER_INVOCATIONS_ARB),
        CLIPPING_INPUT_PRIMITIVES(9, GL_CLIPPING_INPUT_PRIMITIVES_ARB),
        CLIPPING_OUTPUT_PRIMITIVES(10, GL_CLIPPING_OUTPUT_PRIMITIVES_ARB);

        private final int index;
        private final int target;
        private final boolean composite;
        private final Stat ref;
        private final float factor;

        Stat(final int index,
             final int target) {
            this(index, target, false, null, 0);
        }

        Stat(final int index,
             final int target,
             final boolean composite,
             final Stat ref,
             final float factor) {
            this.index = index;
            this.target = target;
            this.composite = composite;
            this.ref = ref;
            this.factor = factor;
        }
    }

    public static final int MAX = 11;
    private static final String QUERY_EXTENSION_ARB = "GL_ARB_pipeline_statistics_query";

    private final boolean extensionAvailable;
    private final IntBuffer queryName;
    private IntBuffer queryResult;
    private boolean running;

    public PipelineStatistics() {
        this.extensionAvailable = GLVersion.isExtensionSupported(PipelineStatistics.QUERY_EXTENSION_ARB);
        this.queryName = MemoryUtil.memAllocInt(PipelineStatistics.MAX);
        this.running = false;
        this.queryResult = MemoryUtil.memAllocInt(PipelineStatistics.MAX);
        IntStream.range(0, PipelineStatistics.MAX).forEach((final int index) ->
                this.queryResult.put(index, 0)
        );
    }

    public boolean extensionAvailable() {
        return this.extensionAvailable;
    }

    public void init() {
        if (!this.extensionAvailable) {
            return;
        }
        glGenQueries(this.queryName);

        final int[] queryCounterBits = new int[PipelineStatistics.MAX];

        for (final Stat stat : Stat.values()) {
            if (stat.composite) {
                continue;
            }
            final int[] query = new int[1];
            glGetQueryiv(
                    stat.target,
                    GL_QUERY_COUNTER_BITS,
                    query
            );
            queryCounterBits[stat.index] = query[0];
        }

        boolean validated = true;
        for (final int queryCounterBit : queryCounterBits) {
            validated &= queryCounterBit >= 18;
        }

        if (!validated) {
            throw new RuntimeException("Unable to initialise statistics queries");
        }
    }

    public void begin() {
        if (this.running) {
            return;
//            throw new IllegalStateException("Statistics query already running");
        }
        for (final Stat stat : Stat.values()) {
            if (stat.composite) {
                continue;
            }
            glBeginQuery(
                    stat.target,
                    this.queryName.get(stat.index)
            );
        }
        this.running = true;
    }

    public void end() {
        if (!this.running) {
            return;
//            throw new IllegalStateException("Statistics query is not running");
        }
        for (final Stat stat : Stat.values()) {
            if (stat.composite) {
                continue;
            }
            glEndQuery(stat.target);
            final int[] value = new int[1];
            glGetQueryObjectuiv(
                    this.queryName.get(stat.index),
                    GL_QUERY_RESULT,
                    value
            );
            this.queryResult.put(stat.index, value[0]);
        }
        this.running = false;
    }

    public int getResult(final Stat statistic) {
        final int result;
        if (statistic.composite) {
            result = (int) (this.queryResult.get(statistic.ref.index) * statistic.factor);
        } else {
            result = this.queryResult.get(statistic.index);
        }
        return result;
    }

}
