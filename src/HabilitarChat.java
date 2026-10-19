import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HabilitarChat {

    private final Controlaracceso acceso = new Controlaracceso();

    public List<Solicitud> listarChatsDisponibles(Perfil_usuario usuario) {
        if (!acceso.puedeUsarChat(usuario)) {
            throw new IllegalStateException("Tu tipo de cuenta no puede usar el chat privado.");
        }

        List<Solicitud> chats = new ArrayList<>();
        String sql = Solicitud.CONSULTA
                + "WHERE s.estado = 'APROBADO' AND (s.idEstudiante = ? OR s.idTutor = ?) ORDER BY s.idSolicitud";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, usuario.getIdUsuario());
            ps.setInt(2, usuario.getIdUsuario());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    chats.add(Solicitud.desdeFila(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron obtener los chats: " + e.getMessage());
        }

        return chats;
    }

    public void enviarMensaje(Perfil_usuario usuario, int idSolicitud, String texto) {
        validarAcceso(usuario, idSolicitud);

        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío.");
        }

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String sql = "INSERT INTO mensajes (idSolicitud, idEmisor, texto, fecha) VALUES (?, ?, ?, ?)";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idSolicitud);
            ps.setInt(2, usuario.getIdUsuario());
            ps.setString(3, texto.trim());
            ps.setString(4, fecha);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo enviar el mensaje: " + e.getMessage());
        }
    }

    public List<Mensaje> obtenerMensajes(Perfil_usuario usuario, int idSolicitud) {
        validarAcceso(usuario, idSolicitud);

        List<Mensaje> mensajes = new ArrayList<>();
        String sql = "SELECT m.idMensaje, m.idSolicitud, m.idEmisor, u.nombre, m.texto, m.fecha "
                + "FROM mensajes m JOIN usuarios u ON u.idUsuario = m.idEmisor "
                + "WHERE m.idSolicitud = ? ORDER BY m.idMensaje";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idSolicitud);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mensajes.add(new Mensaje(
                            rs.getInt("idMensaje"),
                            rs.getInt("idSolicitud"),
                            rs.getInt("idEmisor"),
                            rs.getString("nombre"),
                            rs.getString("texto"),
                            rs.getString("fecha")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron obtener los mensajes: " + e.getMessage());
        }

        return mensajes;
    }

    private void validarAcceso(Perfil_usuario usuario, int idSolicitud) {
        if (!acceso.puedeAccederChat(usuario, idSolicitud)) {
            throw new IllegalStateException("No puedes usar este chat: la solicitud no ha sido aceptada o no eres parte de ella.");
        }
    }
}