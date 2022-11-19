# TODO

* Scene management with ImGui
  * Add/remove entities from the scene
  * Edit properties of entities
    * Position
    * Scale
    * Material values
    * Culling
* Transparency support
* Radiosity
* Volumetric lighting
* Water
* Scene entity selection
  * Outline with post-processing shader
* Generify rendering pipeline
  * Add `@Renderer` to class extending `AbstractRenderingHandler`
  * Specify rendering stage to attach to and ordering as parameters of `@Renderer`
  * Resolve renderer classes with reflection, order and categorise them
  * Invoke intialisers for each rendering handler
  * Invoke `void render(final Scene scene, final RenderingState state);` method on classes in order
* Fix shadow acne and bias
* Fix shadow sampling and smoothing
* Debug view for OpenGL and pipeline statistics with ImGui
* Replace usages of `RuntimeException` with custom exceptions