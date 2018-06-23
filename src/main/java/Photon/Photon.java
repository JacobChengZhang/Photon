package Photon;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Photon extends Application {
  private Scene scene = null;
  private StackPane root = null;
  private ImageView imgView = null;
  private Text startupTips = null;
  private File[] fileList = null;
  private File currentFile = null;
  private double imgWidth = 0;
  private double imgHeight = 0;

  private boolean ordered = true;
  private int pos = 0;
  private Set fileExtensions = new HashSet() {
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
  private LinkedList<Integer> previousFiles = new LinkedList<>();

  private volatile boolean slidePlaying = false;
  private Runnable runnable = () -> {
    try {
      while (slidePlaying) {
        Thread.sleep(Settings.SLIDE_INTERVAL);
        btnClicked(true);
      }
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    }
  };

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle(genTitle());
    primaryStage.setResizable(Settings.WINDOW_RESIZABLE);
    scene = new Scene(createScene());
    scene.setOnKeyPressed(k -> {
      switch (k.getCode()) {
        case UP: {
          revealImageInFinder();
          break;
        }
        case LEFT: {
          recall();
          break;
        }
        case RIGHT: {
          btnClicked(true);
          break;
        }
        case SLASH: {
          ordered = !ordered;
          primaryStage.setTitle(genTitle());
          break;
        }
        case P: {
          if (fileList != null) {
            /*
            Notice that, if you toggle this several times within the sleep interval and stop at the "on" state,
            you can achieve a higher speed on slide playing.
            So, it's not a bug. It is actually a feature.
             */
            if (slidePlaying) {
              slidePlaying = false;
            } else {
              slidePlaying = true;
              Thread thread = new Thread(runnable);
              thread.start();
            }
            primaryStage.setTitle(genTitle());
          }
          break;
        }
        case ESCAPE: {
          System.exit(0);
        }
        default: {
          break;
        }
      }
    });
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  private Parent createScene() {
    createPane();
    return root;
  }

  private void createPane() {
    root = new StackPane();
    root.setAlignment(Pos.CENTER);

    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    int tmpWidth = gd.getDisplayMode().getWidth();
    int tmpHeight = gd.getDisplayMode().getHeight();

    root.setPrefSize(tmpWidth, tmpHeight);
    root.setBackground(new Background(new BackgroundFill(Settings.BACKGROUND_COLOR, null, null)));

    startupTips = new Text(Settings.STARTUP_TIPS);
    startupTips.setFill(Color.WHITE);
    startupTips.setFont(Settings.TIPS_FONT);
    root.getChildren().add(startupTips);

    imgView = new ImageView();
    imgView.setPreserveRatio(true);
    imgView.fitWidthProperty().bind(root.widthProperty());
    imgView.fitHeightProperty().bind(root.heightProperty());

    ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

    imgView.setOnMousePressed(e -> {
      Point2D mousePress = imageViewToImage(imgView, new Point2D(e.getX(), e.getY()));
      mouseDown.set(mousePress);
    });

    imgView.setOnMouseDragged(e -> {
      Point2D dragPoint = imageViewToImage(imgView, new Point2D(e.getX(), e.getY()));
      shift(imgView, dragPoint.subtract(mouseDown.get()));
      mouseDown.set(imageViewToImage(imgView, new Point2D(e.getX(), e.getY())));
    });

    imgView.setOnScroll(e -> {
      double delta = e.getDeltaY();
      Rectangle2D viewport = imgView.getViewport();

      double scale = clamp(Math.pow(1.01, delta),

              // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
              Math.min(Settings.MIN_PIXELS / viewport.getWidth(), Settings.MIN_PIXELS / viewport.getHeight()),

              // don't scale so that we're bigger than image dimensions:
              Math.max(imgWidth / viewport.getWidth(), imgHeight / viewport.getHeight())

      );

      Point2D mouse = imageViewToImage(imgView, new Point2D(e.getX(), e.getY()));

      double newWidth = viewport.getWidth() * scale;
      double newHeight = viewport.getHeight() * scale;

      // To keep the visual point under the mouse from moving, we need
      // (x - newViewportMinX) / (x - currentViewportMinX) = scale
      // where x is the mouse X coordinate in the image

      // solving this for newViewportMinX gives

      // newViewportMinX = x - (x - currentViewportMinX) * scale

      // we then clamp this value so the image never scrolls out
      // of the imageview:

      double newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale,
              0, imgWidth - newWidth);
      double newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale,
              0, imgHeight - newHeight);

      imgView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
    });

    root.setOnMouseClicked(e -> {
      if (e.getButton().name().equals("SECONDARY")) {
        btnClicked(true);
      }
//            if (e.getClickCount() == 1 && e.getButton().name().equals("SECONDARY")) {
//                btnClicked(e);
//            }
//            else if (e.getClickCount() == 2 && e.getButton().name().equals("PRIMARY")) {
//                reset(imgView, imgWidth, imgHeight);
//            }
    });
    ;

    root.getChildren().add(imgView);
  }

  private String genTitle() {
    return new StringBuilder()
            .append(Settings.APP_NAME)
            .append("    ")
            .append(ordered ? "#in-order" : "#random")
            .append(slidePlaying ? " #slide-mode" : "")
            .toString();
  }

  private void btnClicked(boolean moveForward) {
    if (fileList == null) {
      // get files at first
      DirectoryChooser directoryChooser = new DirectoryChooser();

      File directory = directoryChooser.showDialog(null);
      if (directory == null) {
        return;
      } else {
        fileList = getFiles(directory);
      }

      if (fileList.length == 0) {
        fileList = null;
        startupTips.setVisible(true);
        return;
      } else {
        startupTips.setVisible(false);
      }
    }

    if (pos < 0) {
      if (moveForward) {
        pos++;
        if (pos == 0) {
          pos =  previousFiles.get(previousFiles.size() - 1);
          genNextPos();
          currentFile = fileList[pos];
        } else {
          currentFile = fileList[previousFiles.get(previousFiles.size() + pos)];
        }
      } else {
        currentFile = fileList[previousFiles.get(previousFiles.size() + pos)];
      }
    } else {
      if (pos == 0 && previousFiles.size() == 0) {
        pos = -1;
      }
      genNextPos();
      previousFiles.add(pos);
      if (previousFiles.size() > Settings.RECALL_CAPABILITY) {
        previousFiles.removeFirst();
      }

      currentFile = fileList[pos];
    }

    Image image = null;
    try {
      image = new Image(new FileInputStream(currentFile));
    } catch (Exception ex) {
      return;
    }

    imgWidth = image.getWidth();
    imgHeight = image.getHeight();
    imgView.setImage(image);
    reset(imgView, imgWidth, imgHeight);
  }

  private void recall() {
    if (pos <= -previousFiles.size()) {
      return;
    }

    if (pos >= 0) {
      pos = -2;
    } else {
      pos--;
    }
    btnClicked(false);
  }

  private void genNextPos() {
    if (ordered) {
      pos++;
      if (pos == fileList.length) {
        pos = 0;
      }
    } else {
      pos = new Random().nextInt(fileList.length);
    }
  }

  private void revealImageInFinder() {
    if (currentFile == null || !currentFile.exists()) {
      return;
    }

    try {
      Runtime.getRuntime().exec(new String[]{
              "open",
              "-R",
              currentFile.getPath(),
      });
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private File[] getFiles(File dir) {
    if (Settings.ADD_IMAGE_RECURSIVELY) {
      //ArrayList<File> files = new ArrayList<>();
      TreeSet<File> files = new TreeSet<>();
      try {
        Files.walk(Paths.get(dir.toURI()))
                .filter(Files::isRegularFile)
                .filter(this::isPhoto)
                .forEach(f -> files.add(f.toFile()));
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
      return files.toArray(new File[files.size()]);
    } else {
      return dir.listFiles(pathname -> {
        if (pathname.isDirectory()) {
          return false;
        } else {
          return true;
        }
      });
    }
  }

  private boolean isPhoto(Path path) {
    String p = path.toString();
    if (p.length() < 4) {
      return false;
    } else {
      return fileExtensions.contains(p.substring(p.length() - 4));
    }

  }

  private void reset(ImageView imageView, double width, double height) {
    imageView.setViewport(new Rectangle2D(0, 0, width, height));
  }

  /**
   * shift the viewport of the imageView by the specified delta, clamping so
   * the viewport does not move off the actual image:
   */
  private void shift(ImageView imageView, Point2D delta) {
    Rectangle2D viewport = imageView.getViewport();

    double width = imageView.getImage().getWidth();
    double height = imageView.getImage().getHeight();

    double maxX = width - viewport.getWidth();
    double maxY = height - viewport.getHeight();

    double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
    double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);

    imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
  }

  private double clamp(double value, double min, double max) {
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    } else {
      return value;
    }
  }

  /**
   * convert mouse coordinates in the imageView to coordinates in the actual image
   */
  private Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates) {
    double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
    double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

    Rectangle2D viewport = imageView.getViewport();
    return new Point2D(
            viewport.getMinX() + xProportion * viewport.getWidth(),
            viewport.getMinY() + yProportion * viewport.getHeight());
  }

  public static void main(String[] args) {
    launch(args);
  }
}
