package com.engineersbox.yajge.scene.element.object.composite.virtualisation;

/*
    Mesh Simplification
    (C) by Sven Forstmann in 2014

    derived from: https://github.com/sp4cerat/Fast-Quadric-Mesh-Simplification
    and: https://github.com/timknip/mesh-decimate/blob/master/src/simplify.js

    License : MIT
    http://opensource.org/licenses/MIT

    Converted to java / jmonkeyengine by James Khan a.k.a jayfella

    Refactored to support decimation by rendered size by Jack Kilrain A.K.A EngineersBox in 2022
 */

import com.engineersbox.yajge.scene.element.object.composite.Mesh;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive.Ref;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive.SymetricMatrix;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive.Triangle;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive.Vertex;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class QuadricErrorSimplifier {

    private static final Logger LOGGER = LogManager.getLogger(QuadricErrorSimplifier.class);

    private final Vector<Triangle> triangles;
    private final Vector<Vertex> vertices;
    private final Vector<Ref> refs;
    private final Mesh inMesh;
    private Vector3f[] meshInVertices;
    private Vector3f[] meshInNormals;
    private int baseTriangleCount = 0;
    private final Vector3f p = new Vector3f();
    private final double thresholdBase;
    private final double powerOffset;
    private final int meshUpdateFrequency;
    private final int clusterVertexThreshold;

    public QuadricErrorSimplifier(final Mesh mesh) {
        this(
                mesh,
                0.000000001d,
                3d,
                5,
                128
        );
    }

    public QuadricErrorSimplifier(final Mesh mesh,
                                  final double thresholdBase,
                                  final double powerOffset,
                                  final int meshUpdateFrequency,
                                  final int clusterVertexThreshold) {
        this.inMesh = mesh;
        this.triangles = new Vector<>();
        this.vertices = new Vector<>();
        this.refs = new Vector<>();
        this.thresholdBase = thresholdBase;
        this.powerOffset = powerOffset;
        this.meshUpdateFrequency = meshUpdateFrequency;
        this.clusterVertexThreshold = clusterVertexThreshold;
    }

    private int inMeshContainsVert(final Vector3f vert) {
        for (int i = 0; i < this.meshInVertices.length; i++) {
            if (this.meshInVertices[i].equals(vert)) {
                return i;
            }
        }
        return -1;
    }

    private void processMesh() {
        this.triangles.clear();
        this.vertices.clear();
        this.refs.clear();
        this.meshInVertices = this.inMesh.getGroupedVertices().toArray(Vector3f[]::new);
        this.meshInNormals = this.inMesh.getGroupedNormals().toArray(Vector3f[]::new);
        final int[] meshIndices = this.inMesh.getIndices();
        Arrays.stream(this.meshInVertices)
                .map(Vertex::new)
                .forEach(this.vertices::add);

        int index = 0;
        this.baseTriangleCount = 0;
        for (int i = 0; i < meshIndices.length; i += 3) {
            final Triangle triangle = new Triangle(
                    meshIndices[index++],
                    meshIndices[index++],
                    meshIndices[index++]
            );
            this.triangles.add(triangle);
            final Ref ref1 = new Ref(this.baseTriangleCount, triangle.getVertices()[0]);
            final Ref ref2 = new Ref(this.baseTriangleCount, triangle.getVertices()[1]);
            final Ref ref3 = new Ref(this.baseTriangleCount, triangle.getVertices()[2]);
            this.refs.add(ref1);
            this.refs.add(ref2);
            this.refs.add(ref3);
            this.baseTriangleCount++;
        }
    }

    /**
     * Begins the simplification process.
     *
     * @param targetPercent the amount in percent to attempt to achieve. For example: 0.25f would result in creating a
     * mesh with 25% of triangles contained in the original.
     * @param agressiveness sharpness to increase the threshold. 5..8 are good numbers. more iterations yield higher
     * quality. Minimum 4 and maximum 20 are recommended.
     * @param complexNormals Whether or not to generate computationally expensive normals, or just use the face normal.
     */
    public Mesh simplify(final float targetPercent,
                         final double agressiveness,
                         final boolean complexNormals) {
        return simplify(
                (int) (this.baseTriangleCount * targetPercent),
                agressiveness,
                complexNormals
        );
    }

    /**
     * Begins the simplification process.
     *
     * @param targetCount the amount of triangles to attempt to achieve.
     * @param aggressiveness sharpness to increase the threshold. 5..8 are good numbers. more iterations yield higher
     * quality. Minimum 4 and maximum 20 are recommended.
     * @param complexNormals Whether or not to generate computationally expensive normals, or just use the face normal.
     */
    public Mesh simplify(final int targetCount,
                         final double aggressiveness,
                         final boolean complexNormals) {
        // re-read the mesh every time we simplify to start with the original data.
        processMesh();
        QuadricErrorSimplifier.LOGGER.info(
                "[MESH QES | START] Simplifying mesh with {} triangles to target count of {} ({} reduction)",
                targetCount,
                this.triangles.size(),
                (targetCount * 100) / this.triangles.size()
        );
        final long timeStart = System.currentTimeMillis();
        this.triangles.forEach(t -> t.setDeleted(false));
        int deletedTriangles = 0;
        final int triangleCount = this.triangles.size();
        this.p.set(0, 0, 0);
        for (int iteration = 0; iteration < 1000; iteration++) {
            QuadricErrorSimplifier.LOGGER.debug(
                    "[MESH QES] Iteration {} Triangles [Deleted: {}] [Count: {}] [Reduction: {}]",
                    iteration,
                    deletedTriangles,
                    triangleCount - deletedTriangles,
                    (deletedTriangles * 100) / triangleCount
            );
            // target number of triangles reached ? Then break
            if (triangleCount - deletedTriangles <= targetCount) {
                break;
            }
            // update mesh once in a while
            if (iteration % this.meshUpdateFrequency == 0) {
                updateMesh(iteration);
            }
            // clear dirty flag
            this.triangles.forEach((final Triangle triangle) -> triangle.setDirty(false));
            //
            // All triangles with edges below the threshold will be removed
            //
            // The following numbers works well for most models.
            // If it does not, try to adjust the 3 parameters
            //
            final double threshold = this.thresholdBase * Math.pow(iteration + this.powerOffset, aggressiveness);
            // remove vertices & mark deleted triangles
            deletedTriangles += removeAndMarkVertices(
                    threshold,
                    deletedTriangles,
                    triangleCount,
                    targetCount
            );
        }
        // clean up mesh
        compactMesh();
        // ready
        final long timeEnd = System.currentTimeMillis();
        QuadricErrorSimplifier.LOGGER.info(
                "[MESH QES | FINISH] Mesh triangles simplified in {}ms [Old: {}] [New: {}] [Reduction: {}]",
                timeEnd - timeStart,
                triangleCount,
                triangleCount - deletedTriangles,
                (deletedTriangles * 100) / triangleCount
        );
        return createSimplifiedMesh(complexNormals);
    }

    private int removeAndMarkVertices(final double threshold,
                                      int globalDeletedTriangles,
                                      final int triangleCount,
                                      final int targetCount) {
        final Vector<Boolean> deleted0 = new Vector<>();
        final Vector<Boolean> deleted1 = new Vector<>();
        int deletedTriangles = 0;
        final ReverseListIterator<Triangle> it = new ReverseListIterator<>(this.triangles);
        for (Triangle triangle = it.next(); it.hasNext(); triangle = it.next()) {
            if (triangle.getError()[3] > threshold || triangle.isDeleted() || triangle.isDirty()) {
                continue;
            }
            for (int j = 0; j < 3; j++) {
                if (triangle.getError()[j] >= threshold) {
                    continue;
                }
                final int index0 = triangle.getVertices()[j];
                final int index1 = triangle.getVertices()[(j + 1) % 3];
                final Vertex vertex0 = this.vertices.get(index0);
                final Vertex vertex1 = this.vertices.get(index1);
                // Border check
                if (vertex0.isBorder() || vertex1.isBorder()) {
                    continue;
                }
                // Compute vertex to collapse to
                this.p.set(0, 0, 0);
                calculateError(index0, index1, this.p);
                deleted0.setSize(vertex0.getTriangleCount()); // normals temporarily
                deleted1.setSize(vertex1.getTriangleCount()); // normals temporarily
                // deleted0.trimToSize();
                // deleted1.trimToSize();
                // don't remove if flipped
                if (flipped(this.p, index1, vertex0, deleted0) || flipped(this.p, index0, vertex1, deleted1)) {
                    continue;
                }
                // not flipped, so remove edge
                vertex0.getPosition().set(this.p);
                vertex0.getQ().addLocal(vertex1.getQ());
                final int triangleStart = this.refs.size();
                deletedTriangles += updateTriangles(index0, vertex0, deleted0);
                deletedTriangles += updateTriangles(index0, vertex1, deleted1);
                final int currentTriangleCount = this.refs.size() - triangleStart;
                vertex0.setTriangleStart(triangleStart);
                vertex0.setTriangleCount(currentTriangleCount);
                break;
            }
            if (triangleCount - deletedTriangles - globalDeletedTriangles <= targetCount) {
                break;
            }
        }
        return deletedTriangles;
    }

    // Check if a triangle flips when this edge is removed
    private boolean flipped(final Vector3f p,
                            final int i1,
                            final Vertex v0,
                            final Vector<Boolean> deleted) {
        for (int k = 0; k < v0.getTriangleCount(); k++) {
            final Ref ref = this.refs.get(v0.getTriangleStart() + k);
            final Triangle triangle = this.triangles.get(ref.getTriangleId());
            if (triangle.isDeleted()) {
                continue;
            }
            final int vertexIdx = ref.getTriangleVertex();
            final int id1 = triangle.getVertices()[(vertexIdx + 1) % 3];
            final int id2 = triangle.getVertices()[(vertexIdx + 2) % 3];
            if (id1 == i1 || id2 == i1) { // delete ?
                deleted.set(k, true);
                continue;
            }
            final Vector3f d1 = this.vertices.get(id1).getPosition().sub(p).normalize();
            final Vector3f d2 = this.vertices.get(id2).getPosition().sub(p).normalize();
            if (Math.abs(d1.dot(d2)) > 0.9999d) {
                return true;
            }
            final Vector3f normal = new Vector3f(d1)
                    .cross(d2)
                    .normalize();
            deleted.set(k, false);
            if (normal.dot(triangle.getNormal()) < 0.2d) {
                return true;
            }
        }
        return false;
    }


    // Update triangle connections and edge error after a edge is collapsed
    private int updateTriangles(final int i0,
                                final Vertex vertex,
                                final Vector<Boolean> deleted) {
        int trianglesRemoved = 0;
        this.p.set(0, 0, 0);
        for (int k = 0; k < vertex.getTriangleCount(); k++) {
            final Ref ref = this.refs.get(vertex.getTriangleStart() + k);
            final Triangle triangle = this.triangles.get(ref.getTriangleId());
            if (triangle.isDeleted()) {
                continue;
            }
            if (Boolean.TRUE.equals(deleted.get(k))) {
                triangle.setDeleted(true);
                trianglesRemoved++;
                continue;
            }
            triangle.getVertices()[ref.getTriangleVertex()] = i0;
            triangle.setDirty(true);
            triangle.getError()[0] = calculateError(
                    triangle.getVertices()[0],
                    triangle.getVertices()[1],
                    this.p
            );
            triangle.getError()[1] = calculateError(
                    triangle.getVertices()[1],
                    triangle.getVertices()[2],
                    this.p
            );
            triangle.getError()[2] = calculateError(
                    triangle.getVertices()[2],
                    triangle.getVertices()[0],
                    this.p
            );
            triangle.getError()[3] = Math.min(
                    triangle.getError()[0],
                    Math.min(
                            triangle.getError()[1],
                            triangle.getError()[2]
                    )
            );
            this.refs.add(ref);
        }
        return trianglesRemoved;
    }

    private void updateMesh(final int iteration) {
        if (iteration > 0) { // compact triangles
            int dst = 0;
            for (int i = 0; i < this.triangles.size(); i++) {
                if (!this.triangles.get(i).isDeleted()) {
                    this.triangles.set(dst++, this.triangles.get(i));
                }
            }
            this.triangles.setSize(dst);
        }
        if (iteration == 0) {
            initQuadricPlaneEdge();
        }
        // Init Reference ID list
        this.vertices.forEach((final Vertex vertex) -> {
            vertex.setTriangleStart(0);
            vertex.setTriangleCount(0);
        });
        this.triangles.forEach((final Triangle triangle) -> {
            this.vertices.get(triangle.getVertices()[0]).setTriangleCount(this.vertices.get(triangle.getVertices()[0]).getTriangleCount() + 1);
            this.vertices.get(triangle.getVertices()[1]).setTriangleCount(this.vertices.get(triangle.getVertices()[1]).getTriangleCount() + 1);
            this.vertices.get(triangle.getVertices()[2]).setTriangleCount(this.vertices.get(triangle.getVertices()[2]).getTriangleCount() + 1);
        });
        int triangleStart = 0;
        for (final Vertex vertex : this.vertices) {
            vertex.setTriangleStart(triangleStart);
            triangleStart += vertex.getTriangleCount();
            vertex.setTriangleCount(0);
        }
        // Write References
        this.refs.setSize(this.triangles.size() * 3);
        for (int i = 0; i < this.triangles.size(); i++) {
            final Triangle triangle = this.triangles.get(i);
            for (int j = 0; j < 3; j++) {
                final Vertex vertex = this.vertices.get(triangle.getVertices()[j]);
                this.refs.get(vertex.getTriangleStart() + vertex.getTriangleCount()).setTriangleId(i);
                this.refs.get(vertex.getTriangleStart() + vertex.getTriangleCount()).setTriangleVertex(j);
                vertex.setTriangleCount(vertex.getTriangleCount() + 1);
            }
        }
        // Identify boundary : vertices[].border=0,1
        if (iteration != 0) {
            return;
        }
        this.vertices.forEach((final Vertex vertex) -> vertex.setBorder(false));
        this.vertices.forEach(this::findBoundary);
    }

    //
    // Init Quadrics by Plane & Edge Errors
    //
    // required at the beginning ( iteration == 0 )
    // recomputing during the simplification is not required,
    // but mostly improves the result for closed meshes
    //
    private void initQuadricPlaneEdge() {
        this.vertices.stream()
                .map(Vertex::getQ)
                .forEach((final SymetricMatrix symetricMatrix) -> symetricMatrix.set(new SymetricMatrix(0.0d)));
        this.triangles.forEach((final Triangle triangle) -> {
            final Vector3f[] vertex = new Vector3f[]{
                    this.vertices.get(triangle.getVertices()[0]).getPosition(),
                    this.vertices.get(triangle.getVertices()[1]).getPosition(),
                    this.vertices.get(triangle.getVertices()[2]).getPosition(),
            };
            final Vector3f normal = vertex[1].sub(vertex[0])
                    .cross(vertex[2].sub(vertex[0]))
                    .normalize();
            triangle.getNormal().set(normal);
            for (int j = 0; j < 3; j++) {
                this.vertices.get(triangle.getVertices()[j]).getQ().set(
                        this.vertices.get(triangle.getVertices()[j]).getQ().add(
                                new SymetricMatrix(
                                        normal.x,
                                        normal.y,
                                        normal.z,
                                        -normal.dot(vertex[0])
                                )
                        )
                );
            }
        });
        this.p.set(0, 0, 0);
        this.triangles.forEach((final Triangle triangle) -> {
            for (int j = 0; j < 3; j++) {
                triangle.getError()[j] = calculateError(triangle.getVertices()[j], triangle.getVertices()[(j + 1) % 3], this.p);
            }
            triangle.getError()[3] = Math.min(triangle.getError()[0], Math.min(triangle.getError()[1], triangle.getError()[2]));
        });
    }

    private void findBoundary(final Vertex vertex) {
        final Vector<Integer> vertexCount = new Vector<>();
        final Vector<Integer> vertexIds = new Vector<>();
        for (int j = 0; j < vertex.getTriangleCount(); j++) {
            int k = this.refs.get(vertex.getTriangleStart() + j).getTriangleId();
            final Triangle triangle = this.triangles.get(k);
            for (k = 0; k < 3; k++) {
                final int vertexId = triangle.getVertices()[k];
                int ofs;
                for (ofs = 0; ofs < vertexCount.size() && vertexIds.get(ofs) != vertexId; ofs++);
                if (ofs == vertexCount.size()) {
                    vertexCount.add(1);
                    vertexIds.add(vertexId);
                } else {
                    vertexCount.set(ofs, vertexCount.get(ofs) + 1);
                }
            }
        }
        vertexCount.stream()
                .mapToInt(Integer::intValue)
                .filter((final int i) -> i == 1)
                .mapToObj(this.vertices::get)
                .forEach((final Vertex matchedVertex) -> matchedVertex.setBorder(true));
    }

    // Finally compact mesh before exiting
    private void compactMesh() {
        var anonymousDst = new Object() {
            int dst = 0;
        };
        this.vertices.forEach((final Vertex vertex) -> vertex.setTriangleCount(0));
        for (final Triangle triangle : this.triangles) {
            if (!triangle.isDeleted()) {
                this.triangles.set(anonymousDst.dst++, triangle);
                for (int j = 0; j < 3; j++) {
                    this.vertices.get(triangle.getVertices()[j]).setTriangleCount(1);
                }
            }
        }
        this.triangles.setSize(anonymousDst.dst);
        anonymousDst.dst = 0;
        this.vertices.stream()
                .filter((final Vertex vertex) -> vertex.getTriangleCount() != 0)
                .forEach((final Vertex vertex) -> {
                    vertex.setTriangleStart(anonymousDst.dst);
                    this.vertices.get(anonymousDst.dst).getPosition().set(vertex.getPosition());
                    anonymousDst.dst++;
                });
        for (final Triangle triangle : this.triangles) {
            for (int j = 0; j < 3; j++) {
                triangle.getVertices()[j] = this.vertices.get(triangle.getVertices()[j]).getTriangleStart();
            }
        }
        this.vertices.setSize(anonymousDst.dst);
    }

    // Error between vertex and Quadric
    private static double vertexError(final SymetricMatrix q,
                                      final double x,
                                      final double y,
                                      final double z) {
        return q.getValue(0) * x * x + 2
                * q.getValue(1) * x * y + 2
                * q.getValue(2) * x * z + 2
                * q.getValue(3) * x
                + q.getValue(4) * y * y + 2
                * q.getValue(5) * y * z + 2
                * q.getValue(6) * y
                + q.getValue(7) * z * z + 2
                * q.getValue(8) * z
                + q.getValue(9);
    }

    // Error for one edge
    private double calculateError(final int idV1,
                                  final int idV2,
                                  final Vector3f pResult) {
        // compute interpolated vertex
        final SymetricMatrix q = this.vertices.get(idV1).getQ().add(this.vertices.get(idV2).getQ());
        final boolean border = this.vertices.get(idV1).isBorder() & this.vertices.get(idV2).isBorder();
        final double error;
        final double det = q.det(0, 1, 2, 1, 4, 5, 2, 5, 7);
        if (det != 0 && !border) {
            // q_delta is invertible
            pResult.x = (float) (-1 / det * (q.det(1, 2, 3, 4, 5, 6, 5, 7, 8)));    // vx = A41/det(q_delta)
            pResult.y = (float) (1 / det * (q.det(0, 2, 3, 1, 5, 6, 2, 7, 8)));    // vy = A42/det(q_delta)
            pResult.z = (float) (-1 / det * (q.det(0, 1, 3, 1, 4, 6, 2, 5, 8)));    // vz = A43/det(q_delta)
            error = QuadricErrorSimplifier.vertexError(q, pResult.x, pResult.y, pResult.z);
        } else {
            // det = 0 -> try to find best result
            final Vector3f p1 = this.vertices.get(idV1).getPosition();
            final Vector3f p2 = this.vertices.get(idV2).getPosition();
            final Vector3f p3 = p1.add(p2).div(2.0f); // (p1+p2)/2;
            final double error1 = QuadricErrorSimplifier.vertexError(q, p1.x, p1.y, p1.z);
            final double error2 = QuadricErrorSimplifier.vertexError(q, p2.x, p2.y, p2.z);
            final double error3 = QuadricErrorSimplifier.vertexError(q, p3.x, p3.y, p3.z);
            error = Math.min(error1, Math.min(error2, error3));
            if (error1 == error) {
                pResult.set(p1);
            }
            if (error2 == error) {
                pResult.set(p2);
            }
            if (error3 == error) {
                pResult.set(p3);
            }
        }
        return error;
    }


    private Mesh createSimplifiedMesh(final boolean complexNormals) {
        final int reducedVertexCount = this.vertices.size();
        final List<Float> newVertices = new ArrayList<>();
        for (final Vertex vertex : this.vertices) {
            newVertices.add(vertex.getPosition().x);
            newVertices.add(vertex.getPosition().y);
            newVertices.add(vertex.getPosition().z);
        }
        final List<Integer> indexList = new ArrayList<>();
        final Vector3f[] normalsArray = complexNormals
                ? normalizeMesh(this.triangles, this.vertices)
                : new Vector3f[reducedVertexCount];
        this.triangles.forEach((final Triangle triangle) -> {
            indexList.add(triangle.getVertices()[0]);
            indexList.add(triangle.getVertices()[1]);
            indexList.add(triangle.getVertices()[2]);
            if (!complexNormals) {
                normalsArray[triangle.getVertices()[0]] = new Vector3f(triangle.getNormal());
                normalsArray[triangle.getVertices()[1]] = new Vector3f(triangle.getNormal());
                normalsArray[triangle.getVertices()[2]] = new Vector3f(triangle.getNormal());
            }
        });
        final List<Float> newNormals = new ArrayList<>();
        for (final Vector3f normal : normalsArray) {
            newNormals.add(normal.x);
            newNormals.add(normal.y);
            newNormals.add(normal.z);
        }
        QuadricErrorSimplifier.LOGGER.info(
                "[MESH QES] Simplified mesh [Vertices: {} -> {}] [Triangles: {} -> {}]",
                this.inMesh.getVertexCount(),
                newVertices.size(),
                this.inMesh.triangleCount(),
                indexList.size() / 3
        );
        return new Mesh(
                ArrayUtils.toPrimitive(newVertices.toArray(Float[]::new)),
                new float[0],
                ArrayUtils.toPrimitive(newNormals.toArray(Float[]::new)),
                ArrayUtils.toPrimitive(indexList.toArray(Integer[]::new))
        );
    }

    private Vector3f[] normalizeMesh(final Vector<Triangle> triangles,
                                     final List<Vertex> vertices) {
        final Vector3f[] newNormals = new Vector3f[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            newNormals[i] = new Vector3f();
        }
        for (final Triangle face : triangles) {
            final int ia = face.getVertices()[0];
            final int ib = face.getVertices()[1];
            final int ic = face.getVertices()[2];
            final Vector3f e1 = vertices.get(ia).getPosition().sub(vertices.get(ib).getPosition());
            final Vector3f e2 = vertices.get(ic).getPosition().sub(vertices.get(ib).getPosition());
            final Vector3f no = e2.cross(e1); //cross( e1, e2 );
            newNormals[ia].add(no);
            newNormals[ib].add(no);
            newNormals[ic].add(no);
        }
        Arrays.stream(newNormals).forEach(Vector3f::normalize);
        for (int i = 0; i < vertices.size(); i++) {
            final int index = inMeshContainsVert(vertices.get(i).getPosition());
            if (index != -1) {
                newNormals[i] = this.meshInNormals[index];
            }
        }
        return newNormals;
    }

    /**
     * The mesh that was given in the constructor.
     *
     * @return the original untouched mesh given in the constructor.
     */
    public Mesh getOriginalMesh() {
        return this.inMesh;
    }

}