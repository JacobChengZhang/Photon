package org.jacobzhang.photon;

import javafx.application.Application;
import javafx.application.Platform;
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
  private Stage stage = null;
  private Scene scene = null;
  private StackPane root = null;
  private ImageView imageView = null;
  private Text startPage = null;
  private File[] fileList = null;
  private double imageWidth = 0;
  private double imageHeight = 0;

  private int pos = 0;
  private boolean inOrder = true;
  private LinkedList<Integer> history = new LinkedList<>();

  private volatile boolean slidePlaying = false;
  private final Runnable playSlide = () -> {
    try {
      while (slidePlaying) {
        Thread.sleep(Settings.SLIDE_INTERVAL);
        next();
      }
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    }
  };

  @Override
  public void start(Stage primaryStage) {
    stage = primaryStage;
    stage.setResizable(Settings.WINDOW_RESIZABLE);
    stage.setOnCloseRequest(e -> Platform.exit());
    updateTitle();
    scene = new Scene(createScene());
    scene.setOnKeyPressed(k -> {
      switch (k.getCode()) {
        case D: {
          openDirectory(false);
          break;
        }
        case F: {
          openDirectory(true);
          break;
        }
        case UP: {
          Utils.revealImageInFinder(getCurrentFile());
          break;
        }
        case LEFT: {
          prev();
          break;
        }
        case RIGHT: {
          next();
          break;
        }
        case SLASH: {
          inOrder = !inOrder;
          updateTitle();
          break;
        }
        case P: {
          toggleSlideMode();
          break;
        }
        case ESCAPE: {
          Platform.exit();
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
    // create Pane
    root = new StackPane();
    root.setAlignment(Pos.CENTER);

    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    int tmpWidth = gd.getDisplayMode().getWidth();
    int tmpHeight = gd.getDisplayMode().getHeight();

    root.setPrefSize(tmpWidth, tmpHeight);
    root.setBackground(new Background(new BackgroundFill(Settings.BACKGROUND_COLOR, null, null)));

    startPage = new Text(Settings.STARTUP_TIPS);
    startPage.setFill(Color.WHITE);
    startPage.setFont(Settings.TIPS_FONT);
    root.getChildren().add(startPage);

    imageView = new ImageView();
    imageView.setPreserveRatio(true);
    imageView.fitWidthProperty().bind(root.widthProperty());
    imageView.fitHeightProperty().bind(root.heightProperty());

    ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

    imageView.setOnMousePressed(e -> {
      Point2D mousePress = Utils.imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
      mouseDown.set(mousePress);
    });

    imageView.setOnMouseDragged(e -> {
      Point2D dragPoint = Utils.imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
      Utils.shift(imageView, dragPoint.subtract(mouseDown.get()));
      mouseDown.set(Utils.imageViewToImage(imageView, new Point2D(e.getX(), e.getY())));
    });

    imageView.setOnScroll(e -> {
      double delta = e.getDeltaY();
      Rectangle2D viewport = imageView.getViewport();

      double scale = Utils.clamp(Math.pow(1.01, delta),

              // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
              Math.min(Settings.MIN_PIXELS / viewport.getWidth(), Settings.MIN_PIXELS / viewport.getHeight()),

              // don't scale so that we're bigger than image dimensions:
              Math.max(imageWidth / viewport.getWidth(), imageHeight / viewport.getHeight())

      );

      Point2D mouse = Utils.imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));

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
              0, imageWidth - newWidth);
      double newMinY = Utils.clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale,
              0, imageHeight - newHeight);

      imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
    });

    root.setOnMouseClicked(e -> {
      if (e.getButton().name().equals("SECONDARY")) {
        next();
      }
//            if (e.getClickCount() == 1 && e.getButton().name().equals("SECONDARY")) {
//                changeImagechangeImage(e);
//            }
//            else if (e.getClickCount() == 2 && e.getButton().name().equals("PRIMARY")) {
//                reset(imageView, imageWidth, imageHeight);
//            }
    });

    root.getChildren().add(imageView);
    return root;
  }

  private void updateTitle() {
    stage.setTitle(Settings.APP_NAME +
            "    " +
            (inOrder ? "#in-order" : "#random") +
            (slidePlaying ? " #slide-mode" : ""));
  }

  private File getCurrentFile() {
    if (pos >= 0) {
      return fileList[pos];
    } else {
      return fileList[history.get(history.size() + pos)];
    }
  }

  private void openDirectory(boolean byFile) {
    if (getDirectory(byFile)) {
      showImage();
      history.add(pos);
      startPage.setVisible(false);
    }
  }

  private boolean getDirectory(boolean byFile) {
    if (byFile) {
      FileChooser fc = new FileChooser();
      File file = fc.showOpenDialog(null);
      if (file == null) {
        return false;
      } else {
        if (fileList != null) {
          clearFilesAndHistory();
        }

        fileList = Utils.getFiles(file.getParentFile());

        int i = 0;
        for (; i < fileList.length; i++) {
          if (fileList[i].equals(file)) {
            pos = i;
            break;
          }
        }
        if (i == fileList.length) {
          pos = 0;
        }
      }
    } else {
      DirectoryChooser dc = new DirectoryChooser();
      File file = dc.showDialog(null);
      if (file == null) {
        return false;
      } else {
        if (fileList != null) {
          clearFilesAndHistory();
        }

        fileList = Utils.getFiles(file);
      }
    }

    if (fileList.length == 0) {
      fileList = null;
      return false;
    } else {
      return true;
    }
  }

  private void clearFilesAndHistory() {
    fileList = null;
    history = new LinkedList<>();
    pos = 0;
    slidePlaying = false;
  }

  private void toggleSlideMode() {
    if (fileList != null) {
            /*
            Notice that, if you toggle this several times within the sleep interval and stop at the "on" state,
            you can achieve a higher speed on toggleSlideMode playing.
            So, it's not a bug. It is actually a feature.
             */
      if (slidePlaying) {
        slidePlaying = false;
      } else {
        slidePlaying = true;
        Utils.ftp.execute(playSlide);
      }
      updateTitle();
    }
  }

  private void next() {
    if (fileList == null) {
      return;
    }

    if (pos < 0) {
      pos++;
      if (pos == 0) {
        pos = history.get(history.size() - 1);
        genNextPos();
      }
    } else {
      genNextPos();
      history.add(pos);
      if (history.size() > Settings.HISTORY_CAPABILITY) {
        history.removeFirst();
      }
    }
    showImage();
  }

  private void prev() {
    if (pos <= -history.size() || history.size() == 1) {
      return;
    }

    if (pos >= 0) {
      pos = -2;
    } else {
      pos--;
    }
    showImage();
  }

  private void genNextPos() {
    if (inOrder) {
      pos++;
      if (pos == fileList.length) {
        pos = 0;
      }
    } else {
      pos = Utils.nextRandomInt(fileList.length);
    }
  }

  private void showImage() {
    if (fileList == null) {
      return;
    }

    final Image image;
    try {
      image = new Image(new FileInputStream(getCurrentFile()));
    } catch (Exception ex) {
      return;
    }

    imageWidth = image.getWidth();
    imageHeight = image.getHeight();
    imageView.setImage(image);
    Utils.reset(imageView, imageWidth, imageHeight);
  }

  public static void main(String[] args) {
    launch(args);
  }
}
