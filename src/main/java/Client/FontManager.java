package Client;

import javafx.scene.text.Font;
import java.io.InputStream;

public class FontManager {
    private static FontManager instance;
    private Font pressStart2P;
    private boolean initialized = false;

    // Font sizes
    public static final double SMALL = 12;
    public static final double MEDIUM = 16;
    public static final double LARGE = 20;
    public static final double XLARGE = 24;
    public static final double TITLE = 32;

    private FontManager() {}

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    public void initialize() {
        if (initialized) return;

        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/PressStart2P-Regular.ttf");
            if (fontStream != null) {
                pressStart2P = Font.loadFont(fontStream, MEDIUM);
                fontStream.close();

                if (pressStart2P != null) {
                    initialized = true;
                } else {
                    loadFallbackFont();
                }
            } else {
                loadFallbackFont();
            }
        } catch (Exception e) {
            e.printStackTrace();
            loadFallbackFont();
        }
    }

    private void loadFallbackFont() {
        pressStart2P = Font.font("Monospaced", MEDIUM);
        initialized = true;
    }

    public Font getPressStart2P() {
        if (!initialized) initialize();
        return pressStart2P;
    }

    public Font getPressStart2P(double size) {
        if (!initialized) initialize();
        return Font.font(pressStart2P.getFamily(), size);
    }


    public Font getSmall() {
        return getPressStart2P(SMALL);
    }

    public Font getMedium() {
        return getPressStart2P(MEDIUM);
    }

    public Font getLarge() {
        return getPressStart2P(LARGE);
    }

    public Font getXLarge() {
        return getPressStart2P(XLARGE);
    }

    public Font getTitle() {
        return getPressStart2P(TITLE);
    }

    public String getFontStyle(double size) {
        if (!initialized) initialize();
        return String.format(
                "-fx-font-family: '%s'; -fx-font-size: %.0fpx;",
                pressStart2P.getFamily(),
                size
        );
    }

    public String getStyleString(double size) {
        return getFontStyle(size);
    }

    public String getStyleString(double size, String color) {
        return getFontStyle(size) + " -fx-text-fill: " + color + ";";
    }
}