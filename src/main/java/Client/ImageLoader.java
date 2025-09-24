package Client;

import javafx.scene.image.Image;
import java.io.InputStream;

public class ImageLoader {

    public static Image loadImage(String path) {
        try {
            InputStream is = ImageLoader.class.getResourceAsStream(path);
            if (is == null) {
                System.err.println("Could not find resource: " + path);
                return null;
            }

            Image image = new Image(is);
            is.close();

            if (image.isError()) {
                System.err.println("Error loading image: " + path);
                return null;
            }

            return image;
        } catch (Exception e) {
            System.err.println("Exception loading image " + path + ": " + e.getMessage());
            return null;
        }
    }
}