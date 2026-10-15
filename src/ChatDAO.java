import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// guarda y recupera conversaciones y mensajes en SQLite (tutorias.db)
public class ChatDAO {

    public ChatDAO() {
        crearTablas();
    }

    private void crearTablas() {
        String tablaConversaciones =
                "CREATE TABLE IF NOT EXISTS conversaciones (" +
                "  id TEXT PRIMARY KEY," +
                "  estudiante_id TEXT NOT NULL," +
                "  tutor_id TEXT NOT NULL," +
                "  fecha TEXT NOT NULL," +
                "  UNIQUE (estudiante_id, tutor_id)" +
                ")";

        String tablaMensajes =
                "CREATE TABLE IF NOT EXISTS mensajes (" +
                "  id TEXT PRIMARY KEY," +
                "  conversacion_id TEXT NOT NULL," +
                "  remitente_id TEXT NOT NULL," +
                "  tipo TEXT NOT NULL," +
                "  contenido TEXT," +
                "  ruta_archivo TEXT," +
                "  fecha TEXT NOT NULL," +
                "  FOREIGN KEY (conversacion_id) REFERENCES conversaciones(id)" +
                ")";

        try (Statement st = DatabaseConnection.getConnection().createStatement()) {
            st.execute(tablaConversaciones);
            st.execute(tablaMensajes);
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear las tablas del chat", e);
        }
    }

    public void guardarConversacion(Conversacion c) {
        String sql = "INSERT INTO conversaciones (id, estudiante_id, tutor_id, fecha) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getId());
            ps.setString(2, c.getEstudiante().getIdUsuario());
            ps.setString(3, c.getTutor().getIdUsuario());
            ps.setString(4, c.getFechaCreacion().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar la conversación", e);
        }
    }

    public void guardarMensaje(Mensaje m, String conversacionId) {
        String sql = "INSERT INTO mensajes (id, conversacion_id, remitente_id, tipo, contenido, ruta_archivo, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, conversacionId);
            ps.setString(3, m.getRemitente().getIdUsuario());
            ps.setString(4, m.getTipo().name());
            ps.setString(5, m.getContenido());
            ps.setString(6, m.getRutaArchivo());
            ps.setString(7, m.getFecha().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el mensaje", e);
        }
    }

    public Optional<Conversacion> buscarPorParticipantes(String estudianteId, String tutorId,
                                                         UsuarioRepositorio repo) {
        List<Conversacion> lista = consultar("estudiante_id = ? AND tutor_id = ?", repo, estudianteId, tutorId);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public Optional<Conversacion> buscarPorId(String id, UsuarioRepositorio repo) {
        List<Conversacion> lista = consultar("id = ?", repo, id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public List<Conversacion> listarDe(String usuarioId, UsuarioRepositorio repo) {
        return consultar("estudiante_id = ? OR tutor_id = ?", repo, usuarioId, usuarioId);
    }

    private List<Conversacion> consultar(String where, UsuarioRepositorio repo, String... params) {
        List<Conversacion> resultado = new ArrayList<>();
        String sql = "SELECT id, estudiante_id, tutor_id, fecha FROM conversaciones WHERE "
                + where + " ORDER BY fecha ASC";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario estudiante = repo.obtenerPorId(rs.getString("estudiante_id"));
                    Usuario tutor = repo.obtenerPorId(rs.getString("tutor_id"));

                    if (estudiante instanceof Estudiante && tutor instanceof Tutor) {
                        resultado.add(new Conversacion(
                                rs.getString("id"),
                                (Estudiante) estudiante,
                                (Tutor) tutor,
                                LocalDateTime.parse(rs.getString("fecha"))
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar las conversaciones", e);
        }

        for (Conversacion c : resultado) {
            cargarMensajes(c);
        }
        return resultado;
    }

    private void cargarMensajes(Conversacion conversacion) {
        String sql = "SELECT id, remitente_id, tipo, contenido, ruta_archivo, fecha FROM mensajes "
                + "WHERE conversacion_id = ? ORDER BY fecha ASC, rowid ASC";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, conversacion.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario remitente = conversacion.participantePorId(rs.getString("remitente_id"));
                    if (remitente == null) {
                        continue;
                    }
                    conversacion.agregarMensaje(new Mensaje(
                            rs.getString("id"),
                            remitente,
                            TipoMensaje.valueOf(rs.getString("tipo")),
                            rs.getString("contenido"),
                            rs.getString("ruta_archivo"),
                            LocalDateTime.parse(rs.getString("fecha"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar los mensajes", e);
        }
    }
}
