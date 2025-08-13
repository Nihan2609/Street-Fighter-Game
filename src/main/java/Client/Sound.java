package Client;

import java.io.IOException;
import javax.sound.sampled.*;

public class Sound implements LineListener {

    private volatile boolean playCompleted;
    private static Clip backgroundClip;

    /**
     * Play a sound once (non-looping).
     */
    void play(java.net.URL audioResourceUrl) {
        new Thread(() -> {
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioResourceUrl)) {
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                Clip audioClip = (Clip) AudioSystem.getLine(info);
                audioClip.addLineListener(this);
                audioClip.open(audioStream);
                playCompleted = false;
                audioClip.start();

                while (!playCompleted) {
                    Thread.sleep(50);
                }
                audioClip.close();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    /**
     * Play a sound in a continuous loop (background music).
     */
    void playLoop(java.net.URL audioResourceUrl) {
        new Thread(() -> {
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioResourceUrl)) {
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                backgroundClip = (Clip) AudioSystem.getLine(info); // store reference
                backgroundClip.open(audioStream);
                backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundClip.start();
                System.out.println("Background music started (looping).");

                // Keep thread alive until clip is closed
                while (backgroundClip.isOpen()) {
                    Thread.sleep(1000);
                }
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    @Override
    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.START) {
            System.out.println("Playback started.");
        } else if (event.getType() == LineEvent.Type.STOP) {
            playCompleted = true;
            System.out.println("Playback completed.");
        }
    }

    /**
     * Start background music when match begins.
     */
    public static void backMusic() {
        Sound background = new Sound();
        java.net.URL audioResource = Sound.class.getResource("/sounds/rain.wav"); // change to your m4a/wav file
        if (audioResource != null) {
            background.playLoop(audioResource);
        } else {
            System.err.println("Background music file not found!");
        }
    }

    /**
     * Stop background music.
     */
    public static void stopBackMusic() {
        if (backgroundClip != null && backgroundClip.isOpen()) {
            backgroundClip.stop();
            backgroundClip.close();
            backgroundClip = null;
            System.out.println("Background music stopped.");
        }
    }

    /**
     * Play punch sound once.
     */
    public static void punch() {
        Sound player = new Sound();
        java.net.URL audioResource = Sound.class.getResource("/sounds/2BH.wav");
        if (audioResource != null) {
            player.play(audioResource);
        } else {
            System.err.println("Punch sound file not found!");
        }
    }
}
