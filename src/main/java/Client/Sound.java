package Client;

import java.io.IOException;
import javax.sound.sampled.*;

public class Sound implements LineListener {

    private volatile boolean playCompleted;

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


    @Override
    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.START) {
            System.out.println("Playback started.");
        } else if (event.getType() == LineEvent.Type.STOP) {
            playCompleted = true;
            System.out.println("Playback completed.");
        }
    }

    public static void punch() {
        Sound player = new Sound();
        java.net.URL audioResource = Sound.class.getResource("/sounds/2BH.wav");
        if (audioResource != null) {
            player.play(audioResource);
        } else {
            System.err.println("Audio resource not found!");
        }
    }


}
