package Photon;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Settings {
  public static final String APP_NAME = "Photon";
  public static final String STARTUP_TIPS = "Right Click or Press RightKey ->  Open Folder / Next Photo\n\n" +
                                            "Scroll                        ->  Zoom Photo\n\n" +
                                            "Drag                          ->  Move Photo\n\n" +
                                            "Press UpKey                   ->  Show in Finder\n\n" +
                                            "Press LeftKey                 ->  Previous Photo\n\n" +
                                            "Press Esc                     ->  Quit\n\n";
                                          //"Double Click \t->\tReset Photo to Fit Window Size\n\n";
  public static final Font TIPS_FONT = new Font("Monaco", 22);
  public static final boolean WINDOW_RESIZABLE = true;
  public static final Color BACKGROUND_COLOR = Color.BLACK;
  public static final boolean ADD_IMAGE_RECURSIVELY = true;
  public static final int MIN_PIXELS = 10;
}
