# TODO

* Scene management/builder with ImGui
  * Add/remove entities from the scene
  * Edit properties of entities
    * Position
    * Scale
    * Material values
    * Culling
* Partial transparency blending support (Potentially dual depth peeling)
* Radiosity
* Volumetric lighting
* Water
* Refactor material system to BRDF
* Scene entity selection via stencilling
* Fix shadow acne and bias
* Fix shadow sampling and smoothing
* Implement cube-mapped point light shadows
* Replace usages of `RuntimeException` with custom exceptions
* Implement frustum culling
* Implement draw distance control
* Refactor command registration for console widget
* Shader graph
* Fix console widget texture bindings/active texture not stencilling properly
* Fix error in `LightingRenderer` and associated shader: `GL_INVALID_OPERATION error generated. Wrong component type or count`
* Add JavaDocs to all implementations
* Add support for terrain save/load
* Test for `GL_ARB_shading_language_include` presence and add a default handler to manually pre-process includes if not available