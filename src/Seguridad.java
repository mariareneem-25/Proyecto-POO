import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Seguridad {

    private static final String dominioInstitucional = "@uvg.edu.gt";
    private static final int largoMinimo = 8;

    public static boolean esCorreoInstitucional(String correo) {
        if (correo == null) {
            return false;
        }

        String texto = correo.toLowerCase();

        if (!texto.endsWith(dominioInstitucional)) {
            return false;
        }

        String nombre = texto.substring(0, texto.length() - dominioInstitucional.length());
        return nombre.matches("[a-z0-9._-]+");
    }

    public static boolean contrasenaValida(String contrasena) {
        if (contrasena == null || contrasena.length() < largoMinimo || contrasena.contains(" ")) {
            return false;
        }

        boolean tieneLetra = false;
        boolean tieneNumero = false;

        for (char caracter : contrasena.toCharArray()) {
            if (Character.isLetter(caracter)) {
                tieneLetra = true;
            }
            if (Character.isDigit(caracter)) {
                tieneNumero = true;
            }
        }

        return tieneLetra && tieneNumero;
    }

    public static String requisitosContrasena() {
        return "La contraseña debe tener al menos " + largoMinimo + " caracteres, con letras y números, y sin espacios.";
    }

    public static void cambiarContrasena(Perfil_usuario usuario, String actual, String nueva, String confirmacion) {
        if (usuario == null || !usuario.isSesionActiva()) {
            throw new IllegalStateException("Debes iniciar sesión para cambiar la contraseña.");
        }

        if (actual == null || nueva == null || confirmacion == null) {
            throw new IllegalArgumentException("Debes llenar todos los campos.");
        }

        if (!coincideContrasenaActual(usuario.getIdUsuario(), actual)) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }

        if (!nueva.equals(confirmacion)) {
            throw new IllegalArgumentException("La confirmación no coincide con la nueva contraseña.");
        }

        if (nueva.equals(actual)) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual.");
        }

        if (!contrasenaValida(nueva)) {
            throw new IllegalArgumentException(requisitosContrasena());
        }

        if (!usuario.actualizarPerfil(usuario.getNombre(), nueva, usuario.getFotoPerfil())) {
            throw new IllegalStateException("No se pudo guardar la nueva contraseña.");
        }
    }

    private static boolean coincideContrasenaActual(int idUsuario, String contrasena) {
        String sql = "SELECT 1 FROM usuarios WHERE idUsuario = ? AND contrasena = ?";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setString(2, contrasena);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Error en la base de datos: " + e.getMessage());
        }
    }
}