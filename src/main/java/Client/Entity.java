package Client;

import javafx.scene.canvas.GraphicsContext;

public abstract class Entity {
    protected float x,y;

    //constructor
    public Entity(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public abstract void tick();

    public abstract void render(GraphicsContext gc);

    public abstract String getName();
}