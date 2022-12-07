#version 330

#extension GL_EXT_geometry_shader4: enable

uniform vec4 l_pos;  // Light position (eye space)
uniform int robust;  // Robust generation needed?
uniform int zpass;  // Is it safe to do z-pass?

void main() {
    vec3 ns[3];  // Normals
    vec3 d[3];  // Directions toward light
    vec4 v[4];  // Temporary vertices
    vec4 or_pos[3] = {  // Triangle oriented toward light source
        gl_PositionIn[0],
        gl_PositionIn[2],
        gl_PositionIn[4]
    };
    // Compute normal at each vertex.
    ns[0] = cross(
        gl_PositionIn[2].xyz - gl_PositionIn[0].xyz,
        gl_PositionIn[4].xyz - gl_PositionIn[0].xyz
    );
    ns[1] = cross(
        gl_PositionIn[4].xyz - gl_PositionIn[2].xyz,
        gl_PositionIn[0].xyz - gl_PositionIn[2].xyz
    );
    ns[2] = cross(
        gl_PositionIn[0].xyz - gl_PositionIn[4].xyz,
        gl_PositionIn[2].xyz - gl_PositionIn[4].xyz
    );
    // Compute direction from vertices to light.
    d[0] = lightPos.xyz - lightPos.w * gl_PositionIn[0].xyz;
    d[1] = lightPos.xyz - lightPos.w * gl_PositionIn[2].xyz;
    d[2] = lightPos.xyz - lightPos.w * gl_PositionIn[4].xyz;
    // Check if the main triangle faces the light.
    bool faces_light = true;
    if (!(dot(ns[0], d[0]) > 0
        || dot(ns[1], d[1]) > 0
        || dot(ns[2], d[2]) > 0)) {
        // Not facing the light and not robust, ignore.
        if (robust == 0) {
            return;
        }
        // Flip vertex winding order in or_pos.
        or_pos[1] = gl_PositionIn[4];
        or_pos[2] = gl_PositionIn[2];
        faces_light = false;
    }
    // Render caps. This is only needed for z-fail.
    if (zpass == 0) {
        // Near cap: simply render triangle.
        gl_Position = gl_ProjectionMatrix * or_pos[0];
        EmitVertex();
        gl_Position = gl_ProjectionMatrix * or_pos[1];
        EmitVertex();
        gl_Position = gl_ProjectionMatrix * or_pos[2];
        EmitVertex();
        EndPrimitive();
        // Far cap: extrude positions to infinity.
        v[0] = vec4(l_pos.w * or_pos[0].xyz - l_pos.xyz, 0);
        v[1] = vec4(l_pos.w * or_pos[2].xyz - l_pos.xyz, 0);
        v[2] = vec4(l_pos.w * or_pos[1].xyz - l_pos.xyz, 0);
        gl_Position = gl_ProjectionMatrix * v[0];
        EmitVertex();
        gl_Position = gl_ProjectionMatrix * v[1];
        EmitVertex();
        gl_Position = gl_ProjectionMatrix * v[2];
        EmitVertex(); EndPrimitive();
    }
    // Loop over all edges and extrude if needed.
    for (int i = 0; i < 3; i++) {
        // Compute indices of neighbor triangle.
        int v0 = i * 2;
        int nb = (i * 2 + 1);
        int v1 = (i * 2 + 2) % 6;
        // Compute normals at vertices, the *exact*
        // same way as done above!
        ns[0] = cross(
            gl_PositionIn[nb].xyz - gl_PositionIn[v0].xyz,
            gl_PositionIn[v1].xyz - gl_PositionIn[v0].xyz
        );
        ns[1] = cross(
            gl_PositionIn[v1].xyz - gl_PositionIn[nb].xyz,
            gl_PositionIn[v0].xyz - gl_PositionIn[nb].xyz
        );
        ns[2] = cross(
            gl_PositionIn[v0].xyz - gl_PositionIn[v1].xyz,
            gl_PositionIn[nb].xyz - gl_PositionIn[v1].xyz
        );
        // Compute direction to light, again as above.
        d[0] = lightPos.xyz - lightPos.w * gl_PositionIn[v0].xyz;
        d[1] = lightPos.xyz - lightPos.w * gl_PositionIn[nb].xyz;
        d[2] = lightPos.xyz - lightPos.w * gl_PositionIn[v1].xyz;
        // Extrude the edge if it does not have a
        // neighbor, or if it's a possible silhouette.
        if (gl_PositionIn[nb].w < 1e-3
            || (faces_light != (dot(ns[0], d[0]) > 0
                || dot(ns[1], d[1]) > 0
                || dot(ns[2], d[2]) > 0))) {
            // Make sure sides are oriented correctly.
            int i0 = faces_light ? v0 : v1;
            int i1 = faces_light ? v1 : v0;
            v[0] = gl_PositionIn[i0];
            v[1] = vec4(l_pos.w * gl_PositionIn[i0].xyz - l_pos.xyz, 0);
            v[2] = gl_PositionIn[i1];
            v[3] = vec4(l_pos.w * gl_PositionIn[i1].xyz - l_pos.xyz, 0);
            // Emit a quad as a triangle strip.
            gl_Position = gl_ProjectionMatrix * v[0];
            EmitVertex();
            gl_Position = gl_ProjectionMatrix * v[1];
            EmitVertex();
            gl_Position = gl_ProjectionMatrix * v[2];
            EmitVertex();
            gl_Position = gl_ProjectionMatrix * v[3];
            EmitVertex();
            EndPrimitive();
        }
    }
}