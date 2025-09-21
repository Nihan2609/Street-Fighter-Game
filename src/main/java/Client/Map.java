package Client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Map {
    private Image bg;

    public Map(String path) {
        bg = new Image(getClass().getResource(path).toExternalForm());
    }

    public void render(GraphicsContext gc) {
        gc.drawImage(bg, 0, 0, 960, 560);
    }
}
