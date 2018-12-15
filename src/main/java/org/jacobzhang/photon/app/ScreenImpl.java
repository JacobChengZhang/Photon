package org.jacobzhang.photon.app;

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
import org.jacobzhang.photon.constant.Constant;
import org.jacobzhang.photon.model.Photon;
import org.jacobzhang.photon.model.PhotonImpl;
import org.jacobzhang.photon.util.CommonUtil;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;

/**
 * @author JacobChengZhang
 */
public class ScreenImpl extends Application implements Screen {
    private Photon              photon       = null;
    private Stage               stage        = null;
    private Scene               scene        = null;
    private StackPane           root         = null;
    private ImageView           imageView    = null;
    private Text                startPage    = null;
    private File[]              fileList     = null;
    private double              imageWidth   = 0;
    private double              imageHeight  = 0;

    private int                 pos          = 0;
    private boolean             inOrder      = true;
    private LinkedList<Integer> history      = new LinkedList<>();

    private volatile boolean    slidePlaying = false;
    private final Runnable      playSlide    = () -> {
                                                 try {
                                                     while (slidePlaying) {
                                                         Thread.sleep(Constant.SLIDE_INTERVAL);
                                                         next();
                                                     }
                                                 } catch (InterruptedException ie) {
                                                     ie.printStackTrace();
                                                 }
                                             };

    @Override
    public void start(Stage primaryStage) {
        initStage(primaryStage);

        this.photon = new PhotonImpl(this);
        photon.init();
    }

    private void initStage(Stage primaryStage) {
        this.stage = primaryStage;
        this.stage.setResizable(Constant.WINDOW_RESIZABLE);
        this.stage.setOnCloseRequest(e -> Platform.exit());
    }

    @Override
    public void setScene(Scene scene) {
        this.scene = scene;
    }

    @Override
    public Scene getScene() {
        return this.scene;
    }

    @Override
    public Stage getStage() {
        return this.stage;
    }

    @Override
    public Parent createScene() {
        // create Pane
        root = new StackPane();
        root.setAlignment(Pos.CENTER);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        int tmpWidth = gd.getDisplayMode().getWidth();
        int tmpHeight = gd.getDisplayMode().getHeight();

        root.setPrefSize(tmpWidth, tmpHeight);
        root.setBackground(new Background(new BackgroundFill(Constant.BACKGROUND_COLOR, null, null)));

        startPage = new Text(Constant.STARTUP_TIPS);
        startPage.setFill(Color.WHITE);
        startPage.setFont(Constant.TIPS_FONT);
        root.getChildren().add(startPage);

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(root.widthProperty());
        imageView.fitHeightProperty().bind(root.heightProperty());

        ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

        imageView.setOnMousePressed(e -> {
            Point2D mousePress = CommonUtil.imageViewToImage(imageView,
                new Point2D(e.getX(), e.getY()));
            mouseDown.set(mousePress);
        });

        imageView.setOnMouseDragged(e -> {
            Point2D dragPoint = CommonUtil.imageViewToImage(imageView,
                new Point2D(e.getX(), e.getY()));
            CommonUtil.shift(imageView, dragPoint.subtract(mouseDown.get()));
            mouseDown.set(CommonUtil.imageViewToImage(imageView, new Point2D(e.getX(), e.getY())));
        });

        imageView
            .setOnScroll(e -> {
                double delta = e.getDeltaY();
                Rectangle2D viewport = imageView.getViewport();

                double scale = CommonUtil.clamp(
                    Math.pow(1.01, delta),

                    // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
                    Math.min(Constant.MIN_PIXELS / viewport.getWidth(), Constant.MIN_PIXELS
                                                                        / viewport.getHeight()),

                    // don't scale so that we're bigger than image dimensions:
                    Math.max(imageWidth / viewport.getWidth(), imageHeight / viewport.getHeight())

                );

                Point2D mouse = CommonUtil.imageViewToImage(imageView,
                    new Point2D(e.getX(), e.getY()));

                double newWidth = viewport.getWidth() * scale;
                double newHeight = viewport.getHeight() * scale;

                // To keep the visual point under the mouse from moving, we need
                // (x - newViewportMinX) / (x - currentViewportMinX) = scale
                // where x is the mouse X coordinate in the image

                // solving this for newViewportMinX gives

                // newViewportMinX = x - (x - currentViewportMinX) * scale

                // we then clamp this value so the image never scrolls out
                // of the imageview:

                double newMinX = CommonUtil.clamp(mouse.getX()
                                                  - (mouse.getX() - viewport.getMinX()) * scale, 0,
                    imageWidth - newWidth);
                double newMinY = CommonUtil.clamp(mouse.getY()
                                                  - (mouse.getY() - viewport.getMinY()) * scale, 0,
                    imageHeight - newHeight);

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

    @Override
    public void toggleHelp() {
        if (imageView.isVisible()) {
            imageView.setVisible(false);
            startPage.setVisible(true);
        } else {
            imageView.setVisible(true);
            startPage.setVisible(false);
        }
    }

    @Override
    public void updateTitle() {
        stage.setTitle(Constant.APP_NAME + "    " + (inOrder ? "#in-order" : "#random")
                       + (slidePlaying ? " #slide-mode" : ""));
    }

    @Override
    public File getCurrentFile() {
        if (pos >= 0) {
            return fileList[pos];
        } else {
            return fileList[history.get(history.size() + pos)];
        }
    }

    @Override
    public void openDirectory(boolean byFile) {
        if (getDirectory(byFile)) {
            showImage();
            history.add(pos);
            startPage.setVisible(false);
        }
    }

    @Override
    public void toggleRandom() {
        inOrder = !inOrder;
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

                fileList = CommonUtil.getFiles(file.getParentFile());

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

                fileList = CommonUtil.getFiles(file);
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

    @Override
    public void toggleSlideMode() {
        if (fileList != null) {
            /**
             * Notice that, if you toggle this several times within the sleep interval and stop at the "on" state,
             * you can achieve a higher speed on toggleSlideMode playing.
             * So, it's not a bug. It is actually a feature.
             */
            if (slidePlaying) {
                slidePlaying = false;
            } else {
                slidePlaying = true;
                Constant.FIXED_THREAD_POOL.execute(playSlide);
            }
            updateTitle();
        }
    }

    @Override
    public void next() {
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
            if (history.size() > Constant.HISTORY_CAPABILITY) {
                history.removeFirst();
            }
        }
        showImage();
    }

    @Override
    public void prev() {
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
            pos = CommonUtil.nextRandomInt(fileList.length);
        }
    }

    @Override
    public void showImage() {
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
        CommonUtil.reset(imageView, imageWidth, imageHeight);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
