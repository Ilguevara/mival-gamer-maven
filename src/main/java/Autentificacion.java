
// Archivo: proyecto/Autentificacion.java

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class Autentificacion {
    private static final Logger LOGGER = Logger.getLogger(Autentificacion.class.getName());
    private final Connection connection;

    // Regex para validar correo con dominios .com, .net, .org y .es (puedes añadir más)
    // Dentro de la clase Autentificacion
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(gmail\\.com|hotmail\\.com|outlook\\.com|yahoo\\.es|dominio\\.net|ejemplo\\.org)$",
            Pattern.CASE_INSENSITIVE
    );

    // Dentro de la clase Autentificacion
    public boolean validarFormatoEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public Autentificacion(Connection connection) {
        this.connection = connection;
    }

    /**
     * Registra un nuevo usuario en la BD.
     * Lanza IllegalArgumentException si el email no cumple el patrón.
     */
    public Usuario registrarUsuario(String nombre, String email, String password) throws SQLException {
        if (!validarEmail(email)) {
            throw new IllegalArgumentException(
                    "Dominio inválido. Por favor, ingrese un correo con dominio válido.\n" +
                            "Ejemplos válidos: usuario@gmail.com, usuario@hotmail.com, usuario@outlook.com");
        }

        String idUsuario = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String hashedPassword = hashPassword(password);

        String sql = "INSERT INTO usuario (id_usuario, nombre, email, password) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, idUsuario);
            stmt.setString(2, nombre);
            stmt.setString(3, email);
            stmt.setString(4, hashedPassword);
            stmt.executeUpdate();
        }

        registrarAutenticacion(idUsuario);
        return new Usuario(connection, idUsuario, nombre, email);
    }

    /**
     * Inicia sesión comprobando email y contraseña.
     * Devuelve el objeto Usuario o null si falla.
     */
    public Usuario iniciarSesion(String email, String password) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && verifyPassword(password, rs.getString("password"))) {
                actualizarUltimoLogin(rs.getString("id_usuario"));
                return new Usuario(
                        connection,
                        rs.getString("id_usuario"),
                        rs.getString("nombre"),
                        rs.getString("email")
                );
            }
            return null;
        }
    }

    /**
     * Actualiza la contraseña de un usuario ya existente.
     */
    // Método cambiarContrasena en la clase Autentificacion
    public void cambiarContrasena(String idUsuario, String nuevaPassword) throws SQLException {
        // Validar fortaleza de la contraseña
        if (!esContraseñaSegura(nuevaPassword)) {
            throw new IllegalArgumentException("""
                    La contraseña debe cumplir con:
                    - Mínimo 8 caracteres
                    - Al menos una mayúscula
                    - Al menos una minúscula
                    - Al menos un número
                    """);
        }

        // Verificar que la nueva contraseña sea diferente a la actual
        String sqlVerificar = "SELECT password FROM usuario WHERE id_usuario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sqlVerificar)) {
            stmt.setString(1, idUsuario);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String contraseñaActual = rs.getString("password");
                if (verifyPassword(nuevaPassword, contraseñaActual)) {
                    throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual");
                }
            }
        }

        // Actualizar la contraseña
        String hashed = hashPassword(nuevaPassword);
        String sqlActualizar = "UPDATE usuario SET password = ? WHERE id_usuario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sqlActualizar)) {
            stmt.setString(1, hashed);
            stmt.setString(2, idUsuario);
            int count = stmt.executeUpdate();
            if (count == 0) {
                throw new SQLException("No se encontró usuario con id: " + idUsuario);
            }
        }
    }

    // Método auxiliar para validar fortaleza de contraseña
    boolean esContraseñaSegura(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*");
    }

    // — Métodos auxiliares privados —

    private boolean validarEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private void registrarAutenticacion(String idUsuario) throws SQLException {
        String sql = "INSERT INTO autenticacion (id_usuario) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, idUsuario);
            stmt.executeUpdate();
        }
    }

    private void actualizarUltimoLogin(String idUsuario) throws SQLException {
        String sql = "UPDATE autenticacion SET ultimo_login = NOW() WHERE id_usuario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, idUsuario);
            stmt.executeUpdate();
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al encriptar contraseña", e);
        }
    }

    private boolean verifyPassword(String inputPassword, String storedHash) {
        return hashPassword(inputPassword).equals(storedHash);
    }

    public boolean verificarCredenciales(String email, String password) {
        String sql = "SELECT password FROM usuario WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashed = rs.getString("password");
                return verifyPassword(password, hashed);
            }
        } catch (SQLException e) {
            LOGGER.warning("Error al verificar credenciales: " + e.getMessage());
        }
        return false;
    }
}

//Hola

