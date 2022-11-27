# TODO

* Scene management/builder with ImGui
  * Add/remove entities from the scene
  * Edit properties of entities
    * Position
    * Scale
    * Material values
    * Culling
* Partial transparency blending support
* Radiosity
* Volumetric lighting
* Water
* Refactor material system to BRDF
* Scene entity selection
  * Outline with post-processing shader
* Fix shadow acne and bias
* Fix shadow sampling and smoothing
* Replace usages of `RuntimeException` with custom exceptions
* Implement frustum culling
* Implement draw distance control
* Refactor command registration for console widget
* Shader graph
* Fix console widget texture bindings/active texture not stencilling properly
* Dynamically create global constants shader for inclusion in other shaders, allowing for programmatic definition of elements that would otherwise we constants
  * Use `#extension GL_ARB_shading_language_include: require` to initially expose this functionality in a shader
  * Including other shaders can be done after the extension is declared as `#include <name>`
  * Targets for inclusion are declared as named strings via the `glNamedStringARG(...)` function
* Fix SSAO shader output all black, view space coordinates not calculated correctly?