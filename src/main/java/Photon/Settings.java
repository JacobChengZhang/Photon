package Photon;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Settings {
  public static final String APP_NAME = "Photon";
  public static final String STARTUP_TIPS = "Right Click or Press '\u2192'   ->  Open Folder / Next Photo\n\n" +
                                            "Press '\u2191'                  ->  Show in Finder\n\n" +
                                            "Press '\u2190'                  ->  Previous Photo\n\n" +
                                            "Press '/'                  ->  Toggle In-Order / Random\n\n" +
                                            "Press 'P'                  ->  Toggle Slide Mode\n\n" +
                                            "Scroll                     ->  Zoom Photo\n\n" +
                                            "Drag                       ->  Move Photo\n\n" +
                                            "Press Esc                  ->  Quit\n\n";
                                          //"Double Click \t->\tReset Photo to Fit Window Size\n\n";
  public static final Font TIPS_FONT = new Font("Monaco", 22);
  public static final boolean WINDOW_RESIZABLE = true;
  public static final Color BACKGROUND_COLOR = Color.BLACK;
  public static final boolean ADD_IMAGE_RECURSIVELY = true;
  public static final int MIN_PIXELS = 10;

  public static final int RECALL_CAPABILITY = 77;

  public static final int SLIDE_INTERVAL = 4000;
}
