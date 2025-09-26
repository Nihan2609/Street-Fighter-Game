package Client;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AudioManager {
    private static MediaPlayer bgmPlayer;
    private static Map<String, Media> soundCache = new ConcurrentHashMap<>();
    private static Map<String, MediaPlayer> activePlayers = new ConcurrentHashMap<>();

    // Volume settings
    private static double bgmVolume = 0.1;
    private static double sfxVolume = 0.3;
    private static boolean soundEnabled = true;

    // Background Music Methods
    public static void playBGM(String filename) {
        stopBGM();

        if (!soundEnabled) return;

        try {
            String path = AudioManager.class.getResource("/music/" + filename).toString();
            Media media = new Media(path);
            bgmPlayer = new MediaPlayer(media);
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(bgmVolume);
            bgmPlayer.play();

            System.out.println("Playing BGM: " + filename);
        } catch (Exception e) {
            System.err.println("Could not play BGM: " + filename);
            e.printStackTrace();
        }
    }

    public static void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
    }

    public static void pauseBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.pause();
        }
    }

    public static void resumeBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.play();
        }
    }

    // Sound Effects Methods
    public static void playSFX(String soundName) {
        if (!soundEnabled) return;

        try {
            // Try to get cached media first
            Media media = soundCache.get(soundName);

            if (media == null) {
                // Load and cache the sound
                String path = AudioManager.class.getResource("/sounds/" + soundName + ".wav").toString();
                media = new Media(path);
                soundCache.put(soundName, media);
            }

            // Create new player for this sound instance
            MediaPlayer player = new MediaPlayer(media);
            player.setVolume(sfxVolume);

            // Clean up after sound finishes
            player.setOnEndOfMedia(() -> {
                player.dispose();
                activePlayers.remove(soundName + "_" + player.hashCode());
            });

            // Track active player
            activePlayers.put(soundName + "_" + player.hashCode(), player);

            player.play();

        } catch (Exception e) {
            System.err.println("Could not play SFX: " + soundName);
            // Don't print full stack trace for missing sounds as it's not critical
        }
    }

    // Specific sound effect methods for easy use
    public static void playPunchSound() {
        playSFX("punch");
    }

    public static void playKickSound() {
        playSFX("kick");
    }

    public static void playJumpSound() {
        playSFX("jump");
    }

    public static void playSelectSound() {
        playSFX("select");
    }

    public static void playConfirmSound() {
        playSFX("confirm");
    }

    public static void playP1WinSound() {
        playSFX("p1win");
    }

    public static void playP2WinSound() {
        playSFX("p2win");
    }

    public static void playHitSound() {
        playSFX("hit");
    }

    public static void playBlockSound() {
        playSFX("block");
    }

    public static void playUppercutSound() {
        playSFX("uppercut");
    }

    public static void playAirAttackSound() {
        playSFX("air_attack");
    }

    public static void playLandSound() {
        playSFX("land");
    }

    public static void playDeathSound() {
        playSFX("death");
    }

    // Volume and Settings Control
    public static void setBGMVolume(double volume) {
        bgmVolume = Math.max(0.0, Math.min(1.0, volume));
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(bgmVolume);
        }
    }

    public static void setSFXVolume(double volume) {
        sfxVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        }
    }

    public static void stopAllSounds() {
        stopBGM();

        // Stop all active sound effects
        for (MediaPlayer player : activePlayers.values()) {
            if (player != null) {
                player.stop();
                player.dispose();
            }
        }
        activePlayers.clear();
    }

    // Utility Methods
    public static double getBGMVolume() {
        return bgmVolume;
    }

    public static double getSFXVolume() {
        return sfxVolume;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static boolean isBGMPlaying() {
        return bgmPlayer != null &&
                bgmPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    // Preload frequently used sounds for better performance
    public static void preloadSounds() {
        String[] commonSounds = {
                "punch", "kick", "jump", "select", "confirm",
                "hit", "block", "uppercut", "air_attack", "land", "death"
        };

        for (String sound : commonSounds) {
            try {
                String path = AudioManager.class.getResource("/sounds/" + sound + ".wav").toString();
                Media media = new Media(path);
                soundCache.put(sound, media);
            } catch (Exception e) {
                System.out.println("Could not preload sound: " + sound);
            }
        }

        System.out.println("Preloaded " + soundCache.size() + " sound effects");
    }

    // Clear cache to free memory
    public static void clearSoundCache() {
        soundCache.clear();
        System.out.println("Sound cache cleared");
    }

    // Get status information
    public static String getAudioStatus() {
        return String.format(
                "Audio Status - BGM: %s, SFX Volume: %.1f, BGM Volume: %.1f, Active SFX: %d, Cached Sounds: %d",
                isBGMPlaying() ? "Playing" : "Stopped",
                sfxVolume, bgmVolume,
                activePlayers.size(),
                soundCache.size()
        );
    }
}