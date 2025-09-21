package Client;

import javafx.scene.image.Image;

public class Ken extends Fighter {

    public Ken(double x, double y) {
        super(x, y);
        loadSprites();
    }

    @Override
    public void loadSprites() {
        idleImg = new Image(getClass().getResource("/images/kenSelect.png").toExternalForm());
        attackImg = new Image(getClass().getResource("/images/kenSelect.png").toExternalForm()); // placeholder
        hitImg = new Image(getClass().getResource("/images/kenSelect.png").toExternalForm()); // placeholder
    }
}
