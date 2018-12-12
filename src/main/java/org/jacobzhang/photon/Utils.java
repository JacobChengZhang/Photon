package org.jacobzhang.photon;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Utils {
  private static final Set fileExtensions = new HashSet<String>() {
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

  private static final Random rnd = new Random();

  private static class DaemonThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      return thread;
    }
  }

  private static final DaemonThreadFactory dtf = new DaemonThreadFactory();

  public static final ExecutorService ftp = Executors.newFixedThreadPool(Settings.MAX_THREAD, dtf);

  public static int nextRandomInt(int limit) {
    return rnd.nextInt(limit);
  }

  public static boolean isPhoto(Path path) {
    String p = path.toString();
    if (p.length() < 4) {
      return false;
    } else {
      return fileExtensions.contains(p.substring(p.length() - 4));
    }

  }

  public static File[] getFiles(File dir) {
    TreeSet<File> files = new TreeSet<>();
    try {
      Files.walk(Paths.get(dir.toURI()))
              .filter(Files::isRegularFile)
              .filter(Utils::isPhoto)
              .forEach(f -> files.add(f.toFile()));
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    return files.toArray(new File[0]);
  }

  public static double clamp(double value, double min, double max) {
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    } else {
      return value;
    }
  }

  public static void revealImageInFinder(File file) {
    if (file == null || !file.exists()) {
      return;
    }

    try {
      Runtime.getRuntime().exec(new String[]{
              "open",
              "-R",
              file.getPath(),
      });
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * shift the viewport of the imageView by the specified delta, clamping so
   * the viewport does not move off the actual image:
   */
  public static void shift(ImageView imageView, Point2D delta) {
    Rectangle2D viewport = imageView.getViewport();

    double width = imageView.getImage().getWidth();
    double height = imageView.getImage().getHeight();

    double maxX = width - viewport.getWidth();
    double maxY = height - viewport.getHeight();

    double minX = Utils.clamp(viewport.getMinX() - delta.getX(), 0, maxX);
    double minY = Utils.clamp(viewport.getMinY() - delta.getY(), 0, maxY);

    imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
  }

  /**
   * convert mouse coordinates in the imageView to coordinates in the actual image
   */
  public static Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates) {
    double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
    double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

    Rectangle2D viewport = imageView.getViewport();
    return new Point2D(
            viewport.getMinX() + xProportion * viewport.getWidth(),
            viewport.getMinY() + yProportion * viewport.getHeight());
  }

  public static void reset(ImageView imageView, double width, double height) {
    imageView.setViewport(new Rectangle2D(0, 0, width, height));
  }
}
