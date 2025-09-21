package Client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class KeyManager {

    private ObservableList<KeyCode> keysPressed = FXCollections.observableArrayList();

    public KeyManager() {
    }

    public void handleKeyPressed(KeyEvent e) {
        KeyCode code = e.getCode();
        if (!keysPressed.contains(code)) {
            keysPressed.add(code);
        }
    }

    public void handleKeyReleased(KeyEvent e) {
        keysPressed.remove(e.getCode());
    }

    public boolean isKeyPressed(String keyName) {
        try {
            KeyCode code = KeyCode.valueOf(keyName.toUpperCase());
            return keysPressed.contains(code);
        } catch (IllegalArgumentException e) {
            // Handle cases where the key name is not a valid KeyCode enum
            return false;
        }
    }

    public boolean isPlayerOneUp() { return isKeyPressed("W"); }
    public boolean isPlayerOneDown() { return isKeyPressed("S"); }
    public boolean isPlayerOneLeft() { return isKeyPressed("A"); }
    public boolean isPlayerOneRight() { return isKeyPressed("D"); }

    public boolean isPlayerOneAttackG() { return isKeyPressed("G"); }
    public boolean isPlayerOneAttackH() { return isKeyPressed("H"); }
    public boolean isPlayerOneAttackB() { return isKeyPressed("B"); }
    public boolean isPlayerOneAttackN() { return isKeyPressed("N"); }

    public boolean isPlayerTwoUp() { return isKeyPressed("UP"); }
    public boolean isPlayerTwoDown() { return isKeyPressed("DOWN"); }
    public boolean isPlayerTwoLeft() { return isKeyPressed("LEFT"); }
    public boolean isPlayerTwoRight() { return isKeyPressed("RIGHT"); }

    public boolean isPlayerTwoAttack1() { return isKeyPressed("NUMPAD4"); }
    public boolean isPlayerTwoAttack2() { return isKeyPressed("NUMPAD5"); }
    public boolean isPlayerTwoAttack3() { return isKeyPressed("NUMPAD1"); }
    public boolean isPlayerTwoAttack4() { return isKeyPressed("NUMPAD2"); }
}