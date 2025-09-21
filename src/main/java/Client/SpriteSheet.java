package Client;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

public class SpriteSheet {

    private final Image sheet;

    // Constructor now accepts String path
    public SpriteSheet(String imagePath) {
        this.sheet = ImageLoader.loadImage(imagePath);
        if (this.sheet == null) {
            throw new RuntimeException("Could not load sprite sheet: " + imagePath);
        }
    }

    // Alternative constructor if you already have an Image
    public SpriteSheet(Image sheet) {
        this.sheet = sheet;
    }

    public Image crop(int width, int height, int x, int y) {
        PixelReader reader = sheet.getPixelReader();
        if (reader == null) {
            throw new IllegalStateException("PixelReader could not be created for this image!");
        }
        return new WritableImage(reader, x, y, width, height);
    }

    public Image getSheet() {
        return sheet;
    }
}