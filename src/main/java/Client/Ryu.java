package Client;

import javafx.scene.image.Image;

public class Ryu extends Fighter {

    public Ryu(double x, double y) {
        super(x, y);
        loadSprites();
    }

    @Override
    public void loadSprites() {
        idleImg = new Image(getClass().getResource("/images/ryuSelect.png").toExternalForm());
        attackImg = new Image(getClass().getResource("/images/ryuSelect.png").toExternalForm()); // placeholder
        hitImg = new Image(getClass().getResource("/images/ryuSelect.png").toExternalForm()); // placeholder
    }
}
