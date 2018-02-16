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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

public class Photon extends Application{
    private Scene scene = null;
    private StackPane root = null;
    private ImageView imgView = null;
    private Text startupTips = null;
    private File[] fileList = null;
    private File file = null;
    private double imgWidth = 0;
    private double imgHeight = 0;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(Settings.APP_NAME);
        primaryStage.setResizable(Settings.WINDOW_RESIZABLE);
        scene = new Scene(createScene());
        scene.setOnKeyPressed(k -> {
                    if (k.getCode() == KeyCode.R) {
                        revealImageInFinder();
                    }
                });
        primaryStage.setScene(scene);
        primaryStage.show();
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
        startupTips.setFont(new Font("Helvetica", 25));
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
                btnClicked(e);
            }
//            if (e.getClickCount() == 1 && e.getButton().name().equals("SECONDARY")) {
//                btnClicked(e);
//            }
//            else if (e.getClickCount() == 2 && e.getButton().name().equals("PRIMARY")) {
//                reset(imgView, imgWidth, imgHeight);
//            }
        });;

        root.getChildren().add(imgView);
    }

    private void btnClicked(MouseEvent me) {
        if (fileList == null) {
            // get files at firstA
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

        file = fileList[new Random().nextInt(fileList.length)];

        Image image = null;
        try {
            image = new Image(new FileInputStream(file));
        } catch (Exception ex) {
            return;
        }

        imgWidth = image.getWidth();
        imgHeight = image.getHeight();
        imgView.setImage(image);
        reset(imgView, imgWidth, imgHeight);
    }

    private void revealImageInFinder() {
        if (file == null || !file.exists()) {
            return;
        }

        try {
            Runtime.getRuntime().exec("open -R " + file.getPath());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private File[] getFiles(File dir) {
        if (Settings.ADD_IMAGE_RECURSIVELY) {
            ArrayList<File> files = new ArrayList<>();
            try {
                Files.walk(Paths.get(dir.toURI()))
                        .filter(Files::isRegularFile)
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

    private void reset(ImageView imageView, double width, double height) {
        imageView.setViewport(new Rectangle2D(0, 0, width, height));
    }

    /**
     * shift the viewport of the imageView by the specified delta, clamping so
     * the viewport does not move off the actual image:
     */
    private void shift(ImageView imageView, Point2D delta) {
        Rectangle2D viewport = imageView.getViewport();

        double width = imageView.getImage().getWidth() ;
        double height = imageView.getImage().getHeight() ;

        double maxX = width - viewport.getWidth();
        double maxY = height - viewport.getHeight();

        double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
        double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);

        imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        } else if (value > max){
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

    private Parent createScene() {
        createPane();
        return root;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
