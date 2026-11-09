import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import java.util.Locale;
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

        String texto = correo.trim().toLowerCase(Locale.ROOT);

        if (!texto.endsWith(dominioInstitucional)) {
            return false;
        }

        String nombre = texto.substring(0, texto.length() - dominioInstitucional.length());
        return nombre.matches("[a-z0-9._-]+");
    }

    private static final int ITERACIONES = 210000;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public static String hashContrasena(String contrasena) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        return derivar(contrasena, sal, ITERACIONES);
    }

    private static String derivar(String contrasena, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), sal, iteraciones, 256);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "pbkdf2$" + iteraciones + "$" + Base64.getEncoder().encodeToString(sal)
                    + "$" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger la contraseña", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verificarContrasena(String ingresada, String guardada) {
        if (ingresada == null || guardada == null) return false;
        if (!guardada.startsWith("pbkdf2$")) return false;
        try {
            String[] partes = guardada.split("\\$");
            if (partes.length != 4) return false;
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < 10000 || iteraciones > 1000000) return false;
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            String calculado = derivar(ingresada, sal, iteraciones);
            byte[] actual = Base64.getDecoder().decode(calculado.split("\\$")[3]);
            return MessageDigest.isEqual(esperado, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Actualiza cuentas creadas por versiones anteriores sin invalidar sus contraseñas.
    public static void migrarContrasenasAntiguas() {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement leer = conexion.prepareStatement("SELECT idUsuario, contrasena FROM usuarios");
             ResultSet rs = leer.executeQuery();
             PreparedStatement actualizar = conexion.prepareStatement("UPDATE usuarios SET contrasena = ? WHERE idUsuario = ?")) {
            while (rs.next()) {
                String almacenada = rs.getString("contrasena");
                if (almacenada != null && !almacenada.startsWith("pbkdf2$")) {
                    actualizar.setString(1, hashContrasena(almacenada));
                    actualizar.setInt(2, rs.getInt("idUsuario"));
                    actualizar.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron migrar las contraseñas antiguas", e);
        }
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
        String sql = "SELECT contrasena FROM usuarios WHERE idUsuario = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && verificarContrasena(contrasena, rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error en la base de datos: " + e.getMessage());
        }
    }
}
