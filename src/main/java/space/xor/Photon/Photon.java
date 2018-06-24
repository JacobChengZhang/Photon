package space.xor.Photon;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
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
  private LinkedList<Integer> previousFiles = new LinkedList<>();

  private volatile boolean slidePlaying = false;
  private Runnable runnable = () -> {
    try {
      while (slidePlaying) {
        Thread.sleep(Settings.SLIDE_INTERVAL);
        showImage(true);
      }
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    }
  };

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle(Utils.genTitle(ordered, slidePlaying));
    primaryStage.setResizable(Settings.WINDOW_RESIZABLE);
    scene = new Scene(createScene());
    scene.setOnKeyPressed(k -> {
      switch (k.getCode()) {
        case D: {
          getDirectory(false);
          showImage(true);
          break;
        }
        case F: {
          getDirectory(true);
          showImage(true);
          break;
        }
        case UP: {
          Utils.revealImageInFinder(currentFile);
          break;
        }
        case LEFT: {
          rewind();
          break;
        }
        case RIGHT: {
          showImage(true);
          break;
        }
        case SLASH: {
          ordered = !ordered;
          primaryStage.setTitle(Utils.genTitle(ordered, slidePlaying));
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
              thread.setDaemon(true);
              thread.start();
            }
            primaryStage.setTitle(Utils.genTitle(ordered, slidePlaying));
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
      Point2D mousePress = Utils.imageViewToImage(imgView, new Point2D(e.getX(), e.getY()));
      mouseDown.set(mousePress);
    });

    imgView.setOnMouseDragged(e -> {
      Point2D dragPoint = Utils.imageViewToImage(imgView, new Point2D(e.getX(), e.getY()));
      Utils.shift(imgView, dragPoint.subtract(mouseDown.get()));
      mouseDown.set(Utils.imageViewToImage(imgView, new Point2D(e.getX(), e.getY())));
    });

    imgView.setOnScroll(e -> {
      double delta = e.getDeltaY();
      Rectangle2D viewport = imgView.getViewport();

      double scale = Utils.clamp(Math.pow(1.01, delta),

              // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
              Math.min(Settings.MIN_PIXELS / viewport.getWidth(), Settings.MIN_PIXELS / viewport.getHeight()),

              // don't scale so that we're bigger than image dimensions:
              Math.max(imgWidth / viewport.getWidth(), imgHeight / viewport.getHeight())

      );

      Point2D mouse = Utils.imageViewToImage(imgView, new Point2D(e.getX(), e.getY()));

      double newWidth = viewport.getWidth() * scale;
      double newHeight = viewport.getHeight() * scale;

      // To keep the visual point under the mouse from moving, we need
      // (x - newViewportMinX) / (x - currentViewportMinX) = scale
      // where x is the mouse X coordinate in the image

      // solving this for newViewportMinX gives

      // newViewportMinX = x - (x - currentViewportMinX) * scale

      // we then clamp this value so the image never scrolls out
      // of the imageview:

      double newMinX = Utils.clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale,
              0, imgWidth - newWidth);
      double newMinY = Utils.clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale,
              0, imgHeight - newHeight);

      imgView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
    });

    root.setOnMouseClicked(e -> {
      if (e.getButton().name().equals("SECONDARY")) {
        showImage(true);
      }
//            if (e.getClickCount() == 1 && e.getButton().name().equals("SECONDARY")) {
//                changeImagechangeImage(e);
//            }
//            else if (e.getClickCount() == 2 && e.getButton().name().equals("PRIMARY")) {
//                reset(imgView, imgWidth, imgHeight);
//            }
    });
    ;

    root.getChildren().add(imgView);
  }

  private void showImage(boolean forward) {
    if (fileList == null) {
      return;
    }

    if (pos < 0) {
      if (forward) {
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
      if (previousFiles.size() > Settings.REWIND_CAPABILITY) {
        previousFiles.removeFirst();
      }

      currentFile = fileList[pos];
    }

    openImage();
  }

  private void clearFileList() {
    pos = 0;
    previousFiles.clear();
    slidePlaying = false;
    fileList = null;
    currentFile = null;
  }

  private void getDirectory(boolean byFile) {
    if (fileList != null) {
      clearFileList();
    }

    if (byFile) {
      FileChooser fc = new FileChooser();
      File file = fc.showOpenDialog(null);
      if (file == null) {
        return;
      } else {
        fileList = Utils.getFiles(file.getParentFile());
        int i = 0;
        for (; i < fileList.length; i++) {
          if (fileList[i] == file) {
            pos = i;
          }
        }
        if (i == fileList.length) {
          pos = 0;
        }

        System.out.println(i);
      }
    } else {
      DirectoryChooser dc = new DirectoryChooser();
      File file = dc.showDialog(null);
      if (file == null) {
        return;
      } else {
        fileList = Utils.getFiles(file);
      }
    }

    if (fileList.length == 0) {
      fileList = null;
      startupTips.setVisible(true);
      return;
    } else {
      startupTips.setVisible(false);
    }
  }

  private void openImage() {
    Image image = null;
    try {
      image = new Image(new FileInputStream(currentFile));
    } catch (Exception ex) {
      return;
    }

    imgWidth = image.getWidth();
    imgHeight = image.getHeight();
    imgView.setImage(image);
    Utils.reset(imgView, imgWidth, imgHeight);
  }

  private void rewind() {
    if (pos <= -previousFiles.size()) {
      return;
    }

    if (pos >= 0) {
      pos = -2;
    } else {
      pos--;
    }
    showImage(false);
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

  public static void main(String[] args) {
    launch(args);
  }
}
