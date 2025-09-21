package Client;

import javafx.scene.image.Image;

public class ImageLoader {
    public static Image loadImage(String path) {
        try {
            return new Image(ImageLoader.class.getResource(path).toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
