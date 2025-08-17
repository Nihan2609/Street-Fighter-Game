package Client;

import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Fighter1{
    private ImageView body;
    private double dx = 0;
    private double dy = 0;
    private final double gravity = 1;
    private final double JUMP_FORCE = -15;
    private final double FLOOR_Y = 220;

    public Fighter1(String imagePath, double x, double y, double width, double height, Group root) {
        Image image = new Image(getClass().getResourceAsStream(imagePath));
        body = new ImageView(image);
        body.setX(x);
        body.setY(y);
        body.setFitWidth(width);
        body.setFitHeight(height);
        body.setPreserveRatio(true);
        body.setSmooth(true);
        body.setCache(true);
        root.getChildren().add(body);
    }

    public void moveLeft() { dx = -5; body.setX(body.getX() + dx); }
    public void moveRight() { dx = 5; body.setX(body.getX() + dx); }
    public void jump() { if (onGround()) dy = JUMP_FORCE; }

    public void applyGravity() {
        if (!onGround() || dy < 0) {
            dy += gravity;
            body.setY(body.getY() + dy);
            if (body.getY() > FLOOR_Y) { body.setY(FLOOR_Y); dy = 0; }
        } else { dy = 0; body.setY(FLOOR_Y); }
    }

    public boolean onGround() { return body.getY() >= FLOOR_Y; }

    public double getX() { return body.getX(); }
    public void setX(double x) { body.setX(x); }
    public double getY() { return body.getY(); }
    public void setY(double y) { body.setY(y); }
    public ImageView getBody() { return body; }
}
