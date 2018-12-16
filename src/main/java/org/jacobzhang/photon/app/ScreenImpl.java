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
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jacobzhang.photon.constant.Constant;
import org.jacobzhang.photon.model.Photon;
import org.jacobzhang.photon.model.PhotonImpl;
import org.jacobzhang.photon.util.CommonUtil;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author JacobChengZhang
 */
public class ScreenImpl extends Application implements Screen {
    private Photon         photon      = null;
    private Stage          stage       = null;
    private Scene          scene       = null;
    private StackPane      root        = null;
    private ImageView      imageView   = null;
    private Text           startPage   = null;
    private double         imageWidth  = 0;
    private double         imageHeight = 0;

    private final Runnable playSlide   = () -> {
                                           try {
                                               while (photon.isSlidePlaying()) {
                                                   Thread.sleep(Constant.SLIDE_INTERVAL);
                                                   photon.next();
                                               }
                                           } catch (InterruptedException ie) {
                                               ie.printStackTrace();
                                           }
                                       };

    private void initStage(Stage primaryStage) {
        this.stage = primaryStage;
        this.stage.setResizable(Constant.IS_WINDOW_RESIZABLE);
        this.stage.setOnCloseRequest(e -> Platform.exit());
    }

    @Override
    public void start(Stage primaryStage) {
        initStage(primaryStage);

        this.photon = new PhotonImpl(this);
        photon.init();
    }

    @Override
    public void setScene(Scene scene) {
        this.scene = scene;
    }

    @Override
    public Stage getStage() {
        return this.stage;
    }

    @Override
    public Parent createScene() {
        setRootPane();

        setStartPage();

        setImageView();

        return root;
    }

    @Override
    public void playSlide() {
        Constant.FIXED_THREAD_POOL.execute(playSlide);
    }

    @Override
    public void toggleHelp() {
        if (startPage.isVisible()) {
            startPage.setVisible(false);
            imageView.setVisible(true);
        } else {
            startPage.setVisible(true);
            imageView.setVisible(false);
        }
    }

    @Override
    public void updateTitle(boolean inOrder, boolean slidePlaying) {
        stage.setTitle(Constant.APP_NAME + "    " + (inOrder ? "#in-order" : "#random")
                       + (slidePlaying ? " #slide-mode" : ""));
    }

    @Override
    public void showImage(File file) {
        assert (file != null);

        final Image image;
        try {
            image = new Image(new FileInputStream(file));
        } catch (Exception ex) {
            return;
        }

        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        imageView.setImage(image);
        CommonUtil.reset(imageView, imageWidth, imageHeight);
    }

    private void setRootPane() {
        root = new StackPane();
        root.setAlignment(Pos.CENTER);

        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        root.setPrefSize(graphicsDevice.getDisplayMode().getWidth(), graphicsDevice
            .getDisplayMode().getHeight());

        root.setBackground(Constant.BACKGROUND);

        root.setOnMouseClicked(e -> {
            if (e.getButton().name().equals(Constant.RIGHT_CLICK)) {
                photon.next();
            }
            //            if (e.getClickCount() == 1 && e.getButton().name().equals("SECONDARY")) {
            //                changeImagechangeImage(e);
            //            } else if (e.getClickCount() == 2 && e.getButton().name().equals("PRIMARY")) {
            //                reset(imageView, imageWidth, imageHeight);
            //            }
        });
    }

    private void setStartPage() {
        startPage = new Text(Constant.STARTUP_TIPS);
        startPage.setFill(Constant.TIPS_FILL);
        startPage.setFont(Constant.TIPS_FONT);
        root.getChildren().add(startPage);
    }

    private void setImageView() {
        imageView = new ImageView();
        imageView.setPreserveRatio(Constant.IS_IMAGE_VIEW_KEEP_RATIO);
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

        root.getChildren().add(imageView);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
