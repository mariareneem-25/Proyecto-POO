import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// guarda y consulta las solicitudes de tutor en la tabla solicitudes_tutor
// las columnas nuevas se crean en ConexionDB.crearTablas()
// las reglas de quién puede aprobar o rechazar están en las consultas (solo el revisor elegido)

public class SolicitudTutorDAO {

    // devuelve null si el tutor todavía no ha guardado nada
    public SolicitudTutor obtener(int idTutor) {
        String sql = "SELECT * FROM solicitudes_tutor WHERE idTutor = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idTutor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return desdeResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer la solicitud de tutor: " + e.getMessage());
        }
        return null;
    }

    // guarda el progreso sin enviarlo (el tutor puede seguir editando al volver a iniciar sesión)
    public void guardarBorrador(SolicitudTutor s) {
        String sql = "INSERT INTO solicitudes_tutor "
                + "(idTutor, estado, nombreCompleto, descripcion, materias, fotoPerfil, banner) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(idTutor) DO UPDATE SET "
                + "estado = excluded.estado, nombreCompleto = excluded.nombreCompleto, "
                + "descripcion = excluded.descripcion, materias = excluded.materias, "
                + "fotoPerfil = excluded.fotoPerfil, banner = excluded.banner, "
                + "idRevisor = NULL, motivoRechazo = NULL";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, s.getIdTutor());
            ps.setString(2, EstadoSolicitud.BORRADOR.name());
            ps.setString(3, s.getNombreCompleto());
            ps.setString(4, s.getDescripcion());
            ps.setString(5, s.getMaterias());
            ps.setString(6, s.getFotoPerfil());
            ps.setString(7, s.getBanner());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la solicitud: " + e.getMessage());
        }
    }

    // pasa el borrador a PENDIENTE y lo deja visible solo para el revisor elegido
    public boolean enviar(int idTutor, int idRevisor) {
        String sql = "UPDATE solicitudes_tutor SET estado = ?, idRevisor = ?, motivoRechazo = NULL "
                + "WHERE idTutor = ? AND estado = ?";
        return actualizar(sql, EstadoSolicitud.PENDIENTE.name(), idRevisor, idTutor,
                EstadoSolicitud.BORRADOR.name());
    }

    // solicitudes pendientes que fueron enviadas a este docente o director
    public List<SolicitudTutor> pendientesPara(int idRevisor) {
        List<SolicitudTutor> lista = new ArrayList<>();
        String sql = "SELECT * FROM solicitudes_tutor WHERE estado = ? AND idRevisor = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, EstadoSolicitud.PENDIENTE.name());
            ps.setInt(2, idRevisor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(desdeResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron leer las solicitudes: " + e.getMessage());
        }
        return lista;
    }

    // solo funciona si la solicitud está pendiente y fue enviada a este revisor
    // al aprobar, la información del tutor pasa también a su perfil
    public boolean aprobar(SolicitudTutor s, int idRevisor) {
        String sql = "UPDATE solicitudes_tutor SET estado = ?, aprobadoPor = ?, motivoRechazo = NULL "
                + "WHERE idTutor = ? AND estado = ? AND idRevisor = ?";
        boolean aprobada = actualizar(sql, EstadoSolicitud.APROBADO.name(), idRevisor, s.getIdTutor(),
                EstadoSolicitud.PENDIENTE.name(), idRevisor);

        if (aprobada) {
            copiarAlPerfil(s);
        }
        return aprobada;
    }

    public boolean rechazar(int idTutor, int idRevisor, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo del rechazo es obligatorio");
        }

        String sql = "UPDATE solicitudes_tutor SET estado = ?, aprobadoPor = ?, motivoRechazo = ? "
                + "WHERE idTutor = ? AND estado = ? AND idRevisor = ?";
        return actualizar(sql, EstadoSolicitud.RECHAZADO.name(), idRevisor, motivo.trim(), idTutor,
                EstadoSolicitud.PENDIENTE.name(), idRevisor);
    }

    // "crear nueva solicitud": la rechazada vuelve a borrador conservando lo que ya había llenado
    public boolean reabrir(int idTutor) {
        String sql = "UPDATE solicitudes_tutor SET estado = ?, idRevisor = NULL, motivoRechazo = NULL "
                + "WHERE idTutor = ? AND estado = ?";
        return actualizar(sql, EstadoSolicitud.BORRADOR.name(), idTutor, EstadoSolicitud.RECHAZADO.name());
    }

    // ---------------------------------------------------------------

    private void copiarAlPerfil(SolicitudTutor s) {
        String sql = "UPDATE usuarios SET nombre = ?, fotoPerfil = ?, descripcion = ?, banner = ? "
                + "WHERE idUsuario = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, s.getNombreCompleto());
            ps.setString(2, s.getFotoPerfil());
            ps.setString(3, s.getDescripcion());
            ps.setString(4, s.getBanner());
            ps.setInt(5, s.getIdTutor());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el perfil del tutor: " + e.getMessage());
        }
    }

    private boolean actualizar(String sql, Object... parametros) {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error en la base de datos: " + e.getMessage());
        }
    }

    private SolicitudTutor desdeResultSet(ResultSet rs) throws SQLException {
        int revisor = rs.getInt("idRevisor");
        Integer idRevisor = rs.wasNull() ? null : revisor;

        return new SolicitudTutor(
                rs.getInt("idTutor"),
                rs.getString("nombreCompleto"),
                rs.getString("descripcion"),
                rs.getString("materias"),
                rs.getString("fotoPerfil"),
                rs.getString("banner"),
                EstadoSolicitud.valueOf(rs.getString("estado")),
                idRevisor,
                rs.getString("motivoRechazo")
        );
    }
}
