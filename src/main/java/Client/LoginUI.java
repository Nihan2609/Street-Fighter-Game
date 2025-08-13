package Client;

import db.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;


public class LoginUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");

        Button loginBtn = new Button("Login");
        Button signupBtn = new Button("Sign Up");
        Button leaderboardBtn = new Button("View Leaderboard");

        // Build a form grid
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.add(new Label("Username"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(new Label("Password"), 0, 1);
        form.add(passwordField, 1, 1);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        // Action buttons
        HBox actions = new HBox(10, loginBtn, signupBtn);
        actions.setAlignment(Pos.CENTER);

        // Title
        Label title = new Label("Street Fighter");
        title.getStyleClass().add("login-title");

        // Card container
        VBox card = new VBox(16, title, form, actions, leaderboardBtn);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(320);
        card.getStyleClass().add("login-card");

        // Root with background image and subtle overlay
        StackPane root = new StackPane();
        root.getStyleClass().add("login-root");

        ImageView bgView = new ImageView(new Image(getClass().getResource("/images/LogBack.jpeg").toExternalForm()));
        bgView.setPreserveRatio(true);
        bgView.setSmooth(true);
        bgView.fitWidthProperty().bind(root.widthProperty());
        bgView.fitHeightProperty().bind(root.heightProperty());

        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.rgb(0, 0, 0, 0.45));
        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());

        root.getChildren().addAll(bgView, overlay, card);
        StackPane.setAlignment(card, Pos.CENTER);

        // Layout padding
        StackPane.setMargin(card, new Insets(24));

        // UX tweaks
        loginBtn.setDefaultButton(true);
        loginBtn.disableProperty().bind(
                usernameField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        // --- Login Handler ---
        loginBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please fill in both fields.").showAndWait();
                return;
            }
            if (DatabaseManager.loginPlayer(user, pass)) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Login Success!");
                a.showAndWait();
                showGameModeDialog(user);
                primaryStage.close();
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid login!").showAndWait();
            }
        });

        // --- Signup Handler ---
        signupBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please fill in both fields.").showAndWait();
                return;
            }
            if (DatabaseManager.registerPlayer(user, pass)) {
                new Alert(Alert.AlertType.INFORMATION, "Signup Success! You can now login.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Username already exists!").showAndWait();
            }
        });

        // --- Leaderboard Button ---
        leaderboardBtn.setOnAction(e -> LeaderboardUI.display());
        leaderboardBtn.getStyleClass().add("link-button");

        Scene scene = new Scene(root, 420, 520);
        scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());

        primaryStage.setTitle("Street Fighter Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void showGameModeDialog(String username) {
        Stage dialog = new Stage();
        dialog.setTitle("Select Game Mode");

        Button playPcBtn = new Button("Play vs PC");
        Button playMultiBtn = new Button("Play Multiplayer");

        playPcBtn.setMaxWidth(Double.MAX_VALUE);
        playMultiBtn.setMaxWidth(Double.MAX_VALUE);

        VBox dialogLayout = new VBox(15,
                new Label("Choose a game mode:"),
                playPcBtn,
                playMultiBtn
        );
        dialogLayout.setAlignment(Pos.CENTER);
        dialogLayout.setPadding(new Insets(20));

        playPcBtn.setOnAction(ev -> {
            dialog.close();
            SinglePlayerGame.username = username;
            SinglePlayerGame.openSinglePlayer(username);

        });

        playMultiBtn.setOnAction(ev -> {
            dialog.close();
            GameClient.openMultiplayer(username);
        });


        Scene scene = new Scene(dialogLayout, 250, 180);
        scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    public static void main(String[] args) {
        launch(args);


    }
}
