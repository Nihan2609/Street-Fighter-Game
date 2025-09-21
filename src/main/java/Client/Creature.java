package Client;

import javafx.scene.canvas.GraphicsContext;

public class Creature extends Entity{
    //every subclass of creature has an x,y
    public Creature(float x, float y) {
        super(x, y);
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(GraphicsContext gc) {

    }
}
