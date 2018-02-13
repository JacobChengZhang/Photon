package Photon;

import javafx.scene.paint.Color;

public class Settings {
    public static final String APP_NAME = "Photon";
    public static final String STARTUP_TIPS =   "Right Click  \t->\tOpen Folder / Switch to Next Photo\n\n" +
                                                "Scroll       \t->\tZoom Photo\n\n" +
                                                "Drag         \t->\tMove Photo\n\n" +
                                                "Press 'R'    \t->\tShow in Finder/Explorer\n\n";
                                                //"Double Click \t->\tReset Photo to Fit Window Size\n\n";
    public static final boolean WINDOW_RESIZABLE = true;
    public static final Color BACKGROUND_COLOR = Color.BLACK;
    public static final boolean ADD_IMAGE_RECURSIVELY = true;
    public static final int MIN_PIXELS = 10;
}
