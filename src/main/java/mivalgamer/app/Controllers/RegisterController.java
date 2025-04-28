package mivalgamer.app.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import mivalgamer.app.Autentificacion;
import mivalgamer.app.Usuario;

public class RegisterController {

    @FXML
    private TextField txtNombre;

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
    private void handleRegistrar() {
        String nombre = txtNombre.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        try {
            Usuario usuario = autentificacion.registrarUsuario(nombre, email, password);
            showAlert("Registro Exitoso", "Usuario registrado: " + usuario.getNombre() + " (" + usuario.getEmail() + ")");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Ha ocurrido un error al registrar el usuario.");
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

// Prueba comentario