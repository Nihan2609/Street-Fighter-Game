package Client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public abstract class Fighter {
    protected double x, y;
    protected double vx, vy;
    protected int health = 100;
    protected boolean facingRight = true;
    protected boolean attacking = false;
    protected boolean alive = true;

    protected Image idleImg, attackImg, hitImg;

    public Fighter(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public abstract void loadSprites();

    public void moveLeft() {
        vx = -3;
        facingRight = false;
    }

    public void moveRight() {
        vx = 3;
        facingRight = true;
    }

    public void stop() {
        vx = 0;
    }

    public void jump() {
        if (vy == 0) vy = -10;
    }

    public void attack() {
        attacking = true;
    }

    public void takeDamage(int dmg) {
        if (!alive) return;
        health -= dmg;
        if (health <= 0) {
            alive = false;
            health = 0;
        }
    }

    public void update() {
        x += vx;
        y += vy;
        if (y < 350) {
            vy += 0.5; // gravity
        } else {
            y = 350;
            vy = 0;
        }
        if (attacking) {
            // simple cooldown
            attacking = false;
        }
    }

    public void render(GraphicsContext gc) {
        Image sprite = idleImg;
        if (!alive) sprite = hitImg;
        else if (attacking) sprite = attackImg;

        if (facingRight) {
            gc.drawImage(sprite, x, y, 80, 100);
        } else {
            gc.save();
            gc.translate(x + 80, y);
            gc.scale(-1, 1);
            gc.drawImage(sprite, 0, 0, 80, 100);
            gc.restore();
        }
    }

    public boolean isAttacking() { return attacking; }
    public boolean isAlive() { return alive; }
    public int getHealth() { return health; }
    public double getX() { return x; }
}
