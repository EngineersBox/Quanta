package com.engineersbox.quanta.resources.config;

public class Config {
  public final Config.Engine engine;
  public final Config.Game game;
  public final Config.Gui gui;
  public final Config.Mouse mouse;
  public final Config.Render render;
  public final Config.Sound sound;
  public final Config.Video video;
  // NOTE: incomplete #62 implementation
  public enum MipMapDistance {
    NEAR,
    FAR;
  }
  // NOTE: incomplete #62 implementation
  public enum MipMapType {
    NONE,
    BILINEAR,
    TRILINEAR;
  }

  public static class Engine {
    public final Engine.Features features;
    public final Engine.GlOptions glOptions;

    public static class Features {
      public final boolean showAxis;
      public final boolean showFPS;

      public Features(
          com.typesafe.config.Config c,
          java.lang.String parentPath,
          $TsCfgValidator $tsCfgValidator) {
        this.showAxis = c.hasPathOrNull("showAxis") && c.getBoolean("showAxis");
        this.showFPS = c.hasPathOrNull("showFPS") && c.getBoolean("showFPS");
      }
    }

    public static class GlOptions {
      public final int aaSamples;
      public final boolean antialiasing;
      public final boolean compatProfile;
      public final boolean debug;
      public final boolean debugLogs;
      public final boolean geometryFaceCulling;
      public final boolean shadowFaceCulling;
      public final boolean wireframe;

      public GlOptions(
          com.typesafe.config.Config c,
          java.lang.String parentPath,
          $TsCfgValidator $tsCfgValidator) {
        this.aaSamples = c.hasPathOrNull("aaSamples") ? c.getInt("aaSamples") : 4;
        this.antialiasing = !c.hasPathOrNull("antialiasing") || c.getBoolean("antialiasing");
        this.compatProfile = !c.hasPathOrNull("compatProfile") || c.getBoolean("compatProfile");
        this.debug = c.hasPathOrNull("debug") && c.getBoolean("debug");
        this.debugLogs = c.hasPathOrNull("debugLogs") && c.getBoolean("debugLogs");
        this.geometryFaceCulling =
            c.hasPathOrNull("geometryFaceCulling") && c.getBoolean("geometryFaceCulling");
        this.shadowFaceCulling =
            !c.hasPathOrNull("shadowFaceCulling") || c.getBoolean("shadowFaceCulling");
        this.wireframe = c.hasPathOrNull("wireframe") && c.getBoolean("wireframe");
      }
    }

    public Engine(
        com.typesafe.config.Config c,
        java.lang.String parentPath,
        $TsCfgValidator $tsCfgValidator) {
      this.features =
          c.hasPathOrNull("features")
              ? new Engine.Features(
                  c.getConfig("features"), parentPath + "features.", $tsCfgValidator)
              : new Engine.Features(
                  com.typesafe.config.ConfigFactory.parseString("features{}"),
                  parentPath + "features.",
                  $tsCfgValidator);
      this.glOptions =
          c.hasPathOrNull("glOptions")
              ? new Engine.GlOptions(
                  c.getConfig("glOptions"), parentPath + "glOptions.", $tsCfgValidator)
              : new Engine.GlOptions(
                  com.typesafe.config.ConfigFactory.parseString("glOptions{}"),
                  parentPath + "glOptions.",
                  $tsCfgValidator);
    }
  }

  public static class Game {
    public final double movementSpeed;

    public Game(
        com.typesafe.config.Config c,
        java.lang.String parentPath,
        $TsCfgValidator $tsCfgValidator) {
      this.movementSpeed = c.hasPathOrNull("movementSpeed") ? c.getDouble("movementSpeed") : 0.005;
    }
  }

  public static class Gui {
    public final Gui.Colours colours;

    public static class Colours {
      public final java.lang.String blue;
      public final java.lang.String cyan;
      public final java.lang.String darkGray;
      public final java.lang.String gray;
      public final java.lang.String green;
      public final java.lang.String magenta;
      public final java.lang.String normal;
      public final java.lang.String orange;
      public final java.lang.String red;
      public final java.lang.String yellow;

      public Colours(
          com.typesafe.config.Config c,
          java.lang.String parentPath,
          $TsCfgValidator $tsCfgValidator) {
        this.blue = c.hasPathOrNull("blue") ? c.getString("blue") : "#4763d6";
        this.cyan = c.hasPathOrNull("cyan") ? c.getString("cyan") : "#78dce8";
        this.darkGray = c.hasPathOrNull("darkGray") ? c.getString("darkGray") : "#636263";
        this.gray = c.hasPathOrNull("gray") ? c.getString("gray") : "#939293";
        this.green = c.hasPathOrNull("green") ? c.getString("green") : "#a9dc76";
        this.magenta = c.hasPathOrNull("magenta") ? c.getString("magenta") : "#ab9df2";
        this.normal = c.hasPathOrNull("normal") ? c.getString("normal") : "#fcfcfa";
        this.orange = c.hasPathOrNull("orange") ? c.getString("orange") : "#f59762";
        this.red = c.hasPathOrNull("red") ? c.getString("red") : "#ff6188";
        this.yellow = c.hasPathOrNull("yellow") ? c.getString("yellow") : "#ffd866";
      }
    }

    public Gui(
        com.typesafe.config.Config c,
        java.lang.String parentPath,
        $TsCfgValidator $tsCfgValidator) {
      this.colours =
          c.hasPathOrNull("colours")
              ? new Gui.Colours(c.getConfig("colours"), parentPath + "colours.", $tsCfgValidator)
              : new Gui.Colours(
                  com.typesafe.config.ConfigFactory.parseString("colours{}"),
                  parentPath + "colours.",
                  $tsCfgValidator);
    }
  }

  public static class Mouse {
    public final double sensitivity;

    public Mouse(
        com.typesafe.config.Config c,
        java.lang.String parentPath,
        $TsCfgValidator $tsCfgValidator) {
      this.sensitivity = $_reqDbl(parentPath, c, "sensitivity", $tsCfgValidator);
    }

    private static double $_reqDbl(
        java.lang.String parentPath,
        com.typesafe.config.Config c,
        java.lang.String path,
        $TsCfgValidator $tsCfgValidator) {
      if (c == null) return 0;
      try {
        return c.getDouble(path);
      } catch (com.typesafe.config.ConfigException e) {
        $tsCfgValidator.addBadPath(parentPath + path, e);
        return 0;
      }
    }
  }

  public static class Render {
    public final Render.Camera camera;
    public final Render.Shadows shadows;
    public final boolean ssao;
    public final Render.Texture texture;

    public static class Camera {
      public final double fov;
      public final boolean frustrumCulling;
      public final double zFar;
      public final double zNear;

      public Camera(
          com.typesafe.config.Config c,
          java.lang.String parentPath,
          $TsCfgValidator $tsCfgValidator) {
        this.fov = c.hasPathOrNull("fov") ? c.getDouble("fov") : 60.0;
        this.frustrumCulling =
            !c.hasPathOrNull("frustrumCulling") || c.getBoolean("frustrumCulling");
        this.zFar = c.hasPathOrNull("zFar") ? c.getDouble("zFar") : 1000.0;
        this.zNear = c.hasPathOrNull("zNear") ? c.getDouble("zNear") : 0.01;
      }
    }

    public static class Shadows {
      public final int mapResolution;

      public Shadows(
          com.typesafe.config.Config c,
          java.lang.String parentPath,
          $TsCfgValidator $tsCfgValidator) {
        this.mapResolution = c.hasPathOrNull("mapResolution") ? c.getInt("mapResolution") : 4096;
      }
    }

    public static class Texture {
      public final int lodBias;
      public final MipMapDistance mipmapDistance;
      public final MipMapType mipmaps;

      public Texture(
          com.typesafe.config.Config c,
          java.lang.String parentPath,
          $TsCfgValidator $tsCfgValidator) {
        this.lodBias = c.hasPathOrNull("lodBias") ? c.getInt("lodBias") : 100;
        this.mipmapDistance = MipMapDistance.valueOf(c.getString("mipmapDistance"));
        this.mipmaps = MipMapType.valueOf(c.getString("mipmaps"));
      }
    }

    public Render(
        com.typesafe.config.Config c,
        java.lang.String parentPath,
        $TsCfgValidator $tsCfgValidator) {
      this.camera =
          c.hasPathOrNull("camera")
              ? new Render.Camera(c.getConfig("camera"), parentPath + "camera.", $tsCfgValidator)
              : new Render.Camera(
                  com.typesafe.config.ConfigFactory.parseString("camera{}"),
                  parentPath + "camera.",
                  $tsCfgValidator);
      this.shadows =
          c.hasPathOrNull("shadows")
              ? new Render.Shadows(c.getConfig("shadows"), parentPath + "shadows.", $tsCfgValidator)
              : new Render.Shadows(
                  com.typesafe.config.ConfigFactory.parseString("shadows{}"),
                  parentPath + "shadows.",
                  $tsCfgValidator);
      this.ssao = !c.hasPathOrNull("ssao") || c.getBoolean("ssao");
      this.texture =
          c.hasPathOrNull("texture")
              ? new Render.Texture(c.getConfig("texture"), parentPath + "texture.", $tsCfgValidator)
              : new Render.Texture(
                  com.typesafe.config.ConfigFactory.parseString("texture{}"),
                  parentPath + "texture.",
                  $tsCfgValidator);
    }
  }

  public static class Sound {
    public final double effects;
    public final double master;
    public final double music;

    public Sound(
        com.typesafe.config.Config c,
        java.lang.String parentPath,
        $TsCfgValidator $tsCfgValidator) {
      this.effects = c.hasPathOrNull("effects") ? c.getDouble("effects") : 1.0;
      this.master = c.hasPathOrNull("master") ? c.getDouble("master") : 1.0;
      this.music = c.hasPathOrNull("music") ? c.getDouble("music") : 1.0;
    }
  }

  public static class Video {
    public final int fps;
    public final boolean fullscreen;
    public final int height;
    public final int monitor;
    public final boolean showFps;
    public final int ups;
    public final boolean vsync;
    public final int width;

    public Video(
        com.typesafe.config.Config c,
        java.lang.String parentPath,
        $TsCfgValidator $tsCfgValidator) {
      this.fps = c.hasPathOrNull("fps") ? c.getInt("fps") : 60;
      this.fullscreen = c.hasPathOrNull("fullscreen") && c.getBoolean("fullscreen");
      this.height = c.hasPathOrNull("height") ? c.getInt("height") : 1080;
      this.monitor = c.hasPathOrNull("monitor") ? c.getInt("monitor") : 0;
      this.showFps = !c.hasPathOrNull("showFps") || c.getBoolean("showFps");
      this.ups = c.hasPathOrNull("ups") ? c.getInt("ups") : 30;
      this.vsync = !c.hasPathOrNull("vsync") || c.getBoolean("vsync");
      this.width = c.hasPathOrNull("width") ? c.getInt("width") : 1920;
    }
  }

  public Config(com.typesafe.config.Config c) {
    final $TsCfgValidator $tsCfgValidator = new $TsCfgValidator();
    final java.lang.String parentPath = "";
    this.engine =
        c.hasPathOrNull("engine")
            ? new Config.Engine(c.getConfig("engine"), parentPath + "engine.", $tsCfgValidator)
            : new Config.Engine(
                com.typesafe.config.ConfigFactory.parseString("engine{}"),
                parentPath + "engine.",
                $tsCfgValidator);
    this.game =
        c.hasPathOrNull("game")
            ? new Config.Game(c.getConfig("game"), parentPath + "game.", $tsCfgValidator)
            : new Config.Game(
                com.typesafe.config.ConfigFactory.parseString("game{}"),
                parentPath + "game.",
                $tsCfgValidator);
    this.gui =
        c.hasPathOrNull("gui")
            ? new Config.Gui(c.getConfig("gui"), parentPath + "gui.", $tsCfgValidator)
            : new Config.Gui(
                com.typesafe.config.ConfigFactory.parseString("gui{}"),
                parentPath + "gui.",
                $tsCfgValidator);
    this.mouse =
        c.hasPathOrNull("mouse")
            ? new Config.Mouse(c.getConfig("mouse"), parentPath + "mouse.", $tsCfgValidator)
            : new Config.Mouse(
                com.typesafe.config.ConfigFactory.parseString("mouse{}"),
                parentPath + "mouse.",
                $tsCfgValidator);
    this.render =
        c.hasPathOrNull("render")
            ? new Config.Render(c.getConfig("render"), parentPath + "render.", $tsCfgValidator)
            : new Config.Render(
                com.typesafe.config.ConfigFactory.parseString("render{}"),
                parentPath + "render.",
                $tsCfgValidator);
    this.sound =
        c.hasPathOrNull("sound")
            ? new Config.Sound(c.getConfig("sound"), parentPath + "sound.", $tsCfgValidator)
            : new Config.Sound(
                com.typesafe.config.ConfigFactory.parseString("sound{}"),
                parentPath + "sound.",
                $tsCfgValidator);
    this.video =
        c.hasPathOrNull("video")
            ? new Config.Video(c.getConfig("video"), parentPath + "video.", $tsCfgValidator)
            : new Config.Video(
                com.typesafe.config.ConfigFactory.parseString("video{}"),
                parentPath + "video.",
                $tsCfgValidator);
    $tsCfgValidator.validate();
  }

  private static final class $TsCfgValidator {
    private final java.util.List<java.lang.String> badPaths = new java.util.ArrayList<>();

    void addBadPath(java.lang.String path, com.typesafe.config.ConfigException e) {
      badPaths.add("'" + path + "': " + e.getClass().getName() + "(" + e.getMessage() + ")");
    }

    void validate() {
      if (!badPaths.isEmpty()) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder("Invalid configuration:");
        for (java.lang.String path : badPaths) {
          sb.append("\n    ").append(path);
        }
        throw new com.typesafe.config.ConfigException(sb.toString()) {};
      }
    }
  }
}