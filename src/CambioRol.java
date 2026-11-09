import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// cambia una cuenta de estudiante a tutor y de tutor a estudiante
// al cambiar de rol se cierran las tutorías, reservas y solicitudes que dependían del rol anterior
// el tutor que vuelve a ser estudiante pierde su información de tutor

public class CambioRol {

    private static final String MOTIVO_CAMBIO = "La cuenta cambió de rol";

    public static void aTutor(int idUsuario) {
        new CalendarioDAO(); // asegura que existan las tablas del calendario

        ejecutar("UPDATE solicitudes SET estado = 'RECHAZADO', motivoRechazo = ? "
                + "WHERE idEstudiante = ? AND estado IN ('PENDIENTE', 'APROBADO')", MOTIVO_CAMBIO, idUsuario);
        ejecutar("UPDATE reservas SET estado = 'CANCELADA' WHERE idEstudiante = ? AND estado = 'ACTIVA'", idUsuario);
        ejecutar("UPDATE solicitudes_calendario SET estado = 'RECHAZADA' "
                + "WHERE idEstudiante = ? AND estado = 'PENDIENTE'", idUsuario);

        // el formulario de tutor empieza desde cero
        ejecutar("DELETE FROM solicitudes_tutor WHERE idTutor = ?", idUsuario);
        ejecutar("UPDATE usuarios SET tipoUsuario = 'TUTOR' WHERE idUsuario = ?", idUsuario);
    }

    public static void aEstudiante(int idUsuario) {
        new CalendarioDAO();

        ejecutar("UPDATE solicitudes SET estado = 'RECHAZADO', motivoRechazo = ? "
                + "WHERE idTutor = ? AND estado IN ('PENDIENTE', 'APROBADO')", MOTIVO_CAMBIO, idUsuario);
        ejecutar("UPDATE reservas SET estado = 'CANCELADA' WHERE idTutor = ? AND estado = 'ACTIVA'", idUsuario);
        ejecutar("UPDATE solicitudes_calendario SET estado = 'RECHAZADA' "
                + "WHERE idTutor = ? AND estado = 'PENDIENTE'", idUsuario);

        // se borra la información de tutor (solicitud, descripción y banner)
        ejecutar("DELETE FROM solicitudes_tutor WHERE idTutor = ?", idUsuario);
        ejecutar("UPDATE usuarios SET tipoUsuario = 'ESTUDIANTE', descripcion = '', banner = '' "
                + "WHERE idUsuario = ?", idUsuario);
    }

    private static void ejecutar(String sql, Object... parametros) {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cambiar la cuenta: " + e.getMessage());
        }
    }
}
