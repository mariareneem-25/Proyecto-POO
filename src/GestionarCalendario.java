import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class GestionarCalendario {

    private final Controlaracceso acceso = new Controlaracceso();
    private final HabilitarChat chat = new HabilitarChat();

    public int proponerFecha(Perfil_usuario usuario, int idSolicitud, String textoFecha) {
        if (!acceso.puedeAccederChat(usuario, idSolicitud)) {
            throw new IllegalStateException("Solo puedes proponer fechas en un chat habilitado de tus tutorías.");
        }

        LocalDateTime fecha = convertirFecha(textoFecha);

        if (!fecha.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha propuesta debe ser futura.");
        }

        if (hayConflicto(idSolicitud, fecha)) {
            throw new IllegalStateException("Alguno de los dos ya tiene una tutoría reservada en esa fecha y hora.");
        }

        String sql = "INSERT INTO propuestasFecha (idSolicitud, idProponente, fechaHora, estado) VALUES (?, ?, ?, ?)";
        int idPropuesta = 0;

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idSolicitud);
            ps.setInt(2, usuario.getIdUsuario());
            ps.setString(3, fecha.format(PropuestaFecha.FORMATO));
            ps.setString(4, EstadoSolicitud.PENDIENTE.name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    idPropuesta = rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la propuesta: " + e.getMessage());
        }

        chat.enviarMensaje(usuario, idSolicitud,
                "Propongo la fecha " + fecha.format(PropuestaFecha.FORMATO) + " (propuesta #" + idPropuesta + ")");

        return idPropuesta;
    }

    public List<PropuestaFecha> listarPropuestasPendientes(Perfil_usuario usuario, int idSolicitud) {
        if (!acceso.puedeAccederChat(usuario, idSolicitud)) {
            throw new IllegalStateException("No puedes ver las propuestas de este chat.");
        }

        return buscar("WHERE p.idSolicitud = ? AND p.estado = 'PENDIENTE' ORDER BY p.fechaHora", idSolicitud);
    }

    public void responderPropuesta(Perfil_usuario usuario, int idPropuesta, boolean aceptar) {
        List<PropuestaFecha> encontradas = buscar("WHERE p.idPropuesta = ?", idPropuesta);

        if (encontradas.isEmpty()) {
            throw new IllegalArgumentException("No existe la propuesta indicada.");
        }

        PropuestaFecha propuesta = encontradas.get(0);

        if (!acceso.puedeAccederChat(usuario, propuesta.getIdSolicitud())) {
            throw new IllegalStateException("No puedes responder propuestas de este chat.");
        }

        if (propuesta.getIdProponente() == usuario.getIdUsuario()) {
            throw new IllegalStateException("No puedes responder tu propia propuesta, debe responderla la otra persona.");
        }

        if (propuesta.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("Esta propuesta ya fue respondida.");
        }

        if (aceptar && hayConflicto(propuesta.getIdSolicitud(), propuesta.getFechaHora())) {
            throw new IllegalStateException("Alguno de los dos ya tiene una tutoría reservada en esa fecha y hora.");
        }

        EstadoSolicitud nuevoEstado = aceptar ? EstadoSolicitud.APROBADO : EstadoSolicitud.RECHAZADO;
        actualizarEstado(idPropuesta, nuevoEstado);

        String fecha = propuesta.getFechaHora().format(PropuestaFecha.FORMATO);
        if (aceptar) {
            chat.enviarMensaje(usuario, propuesta.getIdSolicitud(), "Acepté la fecha " + fecha + ". Queda reservada.");
        } else {
            chat.enviarMensaje(usuario, propuesta.getIdSolicitud(), "No puedo en la fecha " + fecha + ".");
        }
    }

    public List<PropuestaFecha> obtenerReservadas(Perfil_usuario solicitante, int idUsuario) {
        if (!acceso.puedeUsarCalendario(solicitante) || !acceso.puedeVerInformacionDe(solicitante, idUsuario)) {
            throw new IllegalStateException("No tienes permiso para ver este calendario.");
        }

        return buscar("WHERE p.estado = 'APROBADO' AND (s.idEstudiante = ? OR s.idTutor = ?) ORDER BY p.fechaHora",
                idUsuario, idUsuario);
    }

    public List<PropuestaFecha> verCalendario(Perfil_usuario usuario) {
        return obtenerReservadas(usuario, usuario.getIdUsuario());
    }

    private LocalDateTime convertirFecha(String textoFecha) {
        if (textoFecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }

        try {
            return LocalDateTime.parse(textoFecha.trim(), PropuestaFecha.FORMATO);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha inválido. Usa aaaa-MM-dd HH:mm, por ejemplo 2026-11-05 14:30.");
        }
    }

    private boolean hayConflicto(int idSolicitud, LocalDateTime fecha) {
        String sql = "SELECT 1 FROM propuestasFecha p "
                + "JOIN solicitudes s ON s.idSolicitud = p.idSolicitud "
                + "JOIN solicitudes actual ON actual.idSolicitud = ? "
                + "WHERE p.estado = 'APROBADO' AND p.fechaHora = ? "
                + "AND (s.idEstudiante = actual.idEstudiante OR s.idTutor = actual.idTutor "
                + "OR s.idEstudiante = actual.idTutor OR s.idTutor = actual.idEstudiante)";

        return ConexionDB.existe(sql, idSolicitud, fecha.format(PropuestaFecha.FORMATO));
    }

    private void actualizarEstado(int idPropuesta, EstadoSolicitud estado) {
        String sql = "UPDATE propuestasFecha SET estado = ? WHERE idPropuesta = ?";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, estado.name());
            ps.setInt(2, idPropuesta);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar la propuesta: " + e.getMessage());
        }
    }

    private List<PropuestaFecha> buscar(String filtro, int... parametros) {
        List<PropuestaFecha> resultado = new ArrayList<>();

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(PropuestaFecha.CONSULTA + filtro)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setInt(i + 1, parametros[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(PropuestaFecha.desdeFila(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron obtener las fechas: " + e.getMessage());
        }

        return resultado;
    }
}