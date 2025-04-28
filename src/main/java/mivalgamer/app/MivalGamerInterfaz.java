package mivalgamer.app;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import mivalgamer.app.Controllers.LoginController;
import  mivalgamer.app.Controllers.RegisterController;

public class MivalGamerInterfaz extends Application {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/tu_base_de_datos";
    private static final String DB_USER = "tu_usuario";
    private static final String DB_PASSWORD = "tu_contraseña";

    private Connection connection;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Crear una conexión con la base de datos
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Conexión a la base de datos establecida.");
            mostrarPantallaLogin(primaryStage);
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos: " + e.getMessage());
        }
    }

    private void mostrarPantallaLogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyecto/views/Login.fxml"));
            Parent root = loader.load();

            // Pasar la conexión al controlador
            LoginController loginController = loader.getController();
            loginController.setAutentificacion(new Autentificacion(connection));

            stage.setTitle("MiVal Gamer - Inicio de Sesión");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("No se pudo cargar la pantalla de inicio de sesión.");
            e.printStackTrace();
        }
    }

    private void mostrarPantallaRegistro(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyecto/views/Register.fxml"));
            Parent root = loader.load();

            // Pasar la conexión al controlador
            RegisterController registerController = loader.getController();
            registerController.setAutentificacion(new Autentificacion(connection));

            stage.setTitle("MiVal Gamer - Registro");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("No se pudo cargar la pantalla de registro.");
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Conexión con la base de datos cerrada.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}