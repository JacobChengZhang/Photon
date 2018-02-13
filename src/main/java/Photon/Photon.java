package Photon;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Random;

public class Photon extends Application{
    private StackPane root = null;
    private ImageView imgView = null;
    private File[] fileList = null;
    private ScrollPane scrollPane = null;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(Settings.appName);
        primaryStage.setResizable(Settings.windowResizable);
        primaryStage.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                Settings.width = number.intValue();
            }
        });
        primaryStage.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                Settings.height = number2.intValue();
            }
        });
        primaryStage.setScene(new Scene(createScene()));
        primaryStage.show();
    }

    private void createPane() {
        root = new StackPane();
        root.setOnMouseClicked((me) -> btnClicked(me));
        root.setAlignment(Pos.CENTER);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Settings.width = gd.getDisplayMode().getWidth();
        Settings.height = gd.getDisplayMode().getHeight();

        root.setPrefSize(Settings.width, Settings.height);
        Rectangle rect = new Rectangle(Settings.width, Settings.height, Color.BLACK);
        root.getChildren().add(rect);

        imgView = new ImageView();
        imgView.setPreserveRatio(true);
        scrollPane = new ScrollPane();
        root.getChildren().add(imgView);
    }

    private void btnClicked(MouseEvent me) {
        if (fileList == null) {
            DirectoryChooser directoryChooser = new DirectoryChooser();

            File directory = directoryChooser.showDialog(null);

            fileList = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.isDirectory()) {
                        return false;
                    } else {
                        return true;
                    }
                }
            });

            if (fileList.length == 0) {
                fileList = null;
                return;
            }
        }

        File file = fileList[new Random().nextInt(fileList.length)];
        Image image = null;
        try {
            image = new Image(new FileInputStream(file));
        } catch (Exception ex) {
            return;
        }

        imgView.setFitWidth(Settings.width);
        imgView.setFitHeight(Settings.height);
        imgView.setImage(image);
    }

    private void reset(ImageView imageView, double width, double height) {
        imageView.setViewport(new Rectangle2D(0, 0, width, height));
    }

    // shift the viewport of the imageView by the specified delta, clamping so
    // the viewport does not move off the actual image:
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

    // convert mouse coordinates in the imageView to coordinates in the actual image:
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
