package mivalgamer.app.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import mivalgamer.app.Autentificacion;
import mivalgamer.app.Usuario;

public class LoginController {

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    private Autentificacion autentificacion;

    // Este método permite recibir una instancia de Autentificacion desde la clase principal
    public void setAutentificacion(Autentificacion autentificacion) {
        this.autentificacion = autentificacion;
    }

    @FXML
    private void handleIniciarSesion() {
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        try {
            Usuario usuario = autentificacion.iniciarSesion(email, password);
            if (usuario != null) {
                showAlert("Inicio de Sesión", "Bienvenido, " + usuario.getNombre() + "!");
            } else {
                showAlert("Error", "Email o Contraseña incorrectos.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Ha ocurrido un error: " + e.getMessage());
        }
    }

    @FXML
    private void handleVolver() {
        System.out.println("Botón de 'Volver' presionado. Implementa la funcionalidad según sea necesario.");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}
