package org.jacobzhang.photon.util;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import org.jacobzhang.photon.constant.Constant;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeSet;

/**
 * @author JacobChengZhang
 */
public class CommonUtil {

    public static boolean stringIsNotEmpty(String string) {
        return string != null && string.length() > 0;
    }

    public static int nextRandomInt(int limit) {
        return Constant.RANDOM.nextInt(limit);
    }

    public static File[] getFiles(File dir) {
        assert (dir != null);
        TreeSet<File> files = new TreeSet<>();
        try {
            Files.walk(Paths.get(dir.toURI())).filter(Files::isRegularFile)
                .filter(CommonUtil::customFileCheck).forEach(f -> files.add(f.toFile()));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return files.toArray(new File[0]);
    }

    public static double clamp(double value, double min, double max) {
        assert (min <= max);
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
            Runtime.getRuntime().exec(new String[] { "open", "-R", file.getPath(), });
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

        double minX = CommonUtil.clamp(viewport.getMinX() - delta.getX(), 0, maxX);
        double minY = CommonUtil.clamp(viewport.getMinY() - delta.getY(), 0, maxY);

        imageView
            .setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
    }

    /**
     * convert mouse coordinates in the imageView to coordinates in the actual image
     */
    public static Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates) {
        double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
        double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

        Rectangle2D viewport = imageView.getViewport();
        return new Point2D(viewport.getMinX() + xProportion * viewport.getWidth(),
            viewport.getMinY() + yProportion * viewport.getHeight());
    }

    public static void reset(ImageView imageView, double width, double height) {
        imageView.setViewport(new Rectangle2D(0, 0, width, height));
    }

    private static boolean customFileCheck(Path path) {
        assert (path != null);
        return isImageExtension(path) && isFileVisible(path);
    }

    private static boolean isFileVisible(Path path) {
        if (path.getFileName() != null) {
            String filename = path.getFileName().toString();
            if (stringIsNotEmpty(filename)) {
                return filename.charAt(0) != '.';
            }
        }
        return false;
    }

    private static boolean isImageExtension(Path path) {
        String filepath = path.toString();
        return filepath.length() >= Constant.FILE_EXTENSION_DEFAULT_LENGTH
                && Constant.FILE_EXTENSIONS
                .contains(filepath.substring(filepath.length()
                        - Constant.FILE_EXTENSION_DEFAULT_LENGTH));
    }
}
