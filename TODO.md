# TODO

* Scene management/builder with ImGui
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
* Move HDR implementation out of lighting pass and into separate HDR pass
  * Move HDR function out of `lighting.frag` and into `hdr.frag`
  * Figure out how to consolidate multi-pass results from lighting pass into single buffer for HDR pass (possibly `glBlitFramebuffer(...)`?)