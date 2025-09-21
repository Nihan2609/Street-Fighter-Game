package Client;

import javafx.scene.image.Image;

public class Animation {

    private final int rate;
    public int index;
    private long lastTime;
    private long timer;
    private final Image[] frames;
    private boolean playedOnce;

    public Animation(int rate, Image[] frames) {
        this.rate = rate;
        this.frames = frames;
        this.index = 0;
        this.lastTime = System.currentTimeMillis();
        this.timer = 0;
        this.playedOnce = false;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        timer += now - lastTime;
        lastTime = now;

        if (timer > rate) {
            index++;
            timer = 0;

            if (index >= frames.length) {
                index = 0;
                playedOnce = true;
            }
        }
    }

    public int getFrame() {
        return index;
    }

    public Image getCurrentFrame() {
        return frames[index];
    }

    public boolean hasPlayedOnce() {
        return playedOnce;
    }

    public void reset() {
        this.index = 0;
        this.timer = 0;
        this.playedOnce = false;
    }

    public void setPlayed() {
        this.playedOnce = false;
    }
}
