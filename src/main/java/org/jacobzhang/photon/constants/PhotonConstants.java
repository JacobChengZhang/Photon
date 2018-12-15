package org.jacobzhang.photon.constants;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author JacobChengZhang
 */
public class PhotonConstants {
    /**
     * description
     */
    public static final String               APP_NAME               = "photon";
    public static final String               STARTUP_TIPS           = "Press 'D'                  ->  Open Directory\n\n"
                                                                      + "Press 'F'                  ->  Open File (and its directory)\n\n"
                                                                      + "Press '\u2192' or Right Click   ->  Next Photo\n\n"
                                                                      + "Press '\u2191'                  ->  Show in Finder\n\n"
                                                                      + "Press '\u2190'                  ->  Previous Photo\n\n"
                                                                      + "Press '/'                  ->  Toggle In-Order / Random\n\n"
                                                                      + "Press 'P'                  ->  Toggle Browse / Slide\n\n"
                                                                      + "Scroll                     ->  Zoom Photo\n\n"
                                                                      + "Drag                       ->  Move Photo\n\n"
                                                                      + "Press Esc                  ->  Quit\n\n";
    public static final Font                 TIPS_FONT              = new Font("Monaco", 22);

    /**
     * GUI
     */
    public static final boolean              WINDOW_RESIZABLE       = true;
    public static final Color                BACKGROUND_COLOR       = Color.BLACK;
    public static final int                  MIN_PIXELS             = 10;

    /**
     * Miscellaneous
     */
    public static final int                  HISTORY_CAPABILITY     = 128;
    public static final int                  SLIDE_INTERVAL         = 4000;
    public static final Random               RANDOM                 = new Random();
    public static final int                  FILE_EXTENSIONS_LENGTH = 4;
    public static final Set                  FILE_EXTENSIONS        = new HashSet<String>() {
                                                                        {
                                                                            add(".jpg");
                                                                            add(".png");
                                                                            add(".gif");
                                                                            add("jpeg");
                                                                            add(".bmp");
                                                                            //add(".tif");
                                                                            //add("tiff");
                                                                            //add(".ico");
                                                                        }
                                                                    };

    /**
     * Multithreading
     */
    private static final int                 MAX_THREAD             = 5;
    private static final DaemonThreadFactory DAEMON_THREAD_FACTORY  = new DaemonThreadFactory();
    public static final ExecutorService      FIXED_THREAD_POOL      = Executors.newFixedThreadPool(
                                                                        MAX_THREAD,
                                                                        DAEMON_THREAD_FACTORY);

    private static class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    }
}
