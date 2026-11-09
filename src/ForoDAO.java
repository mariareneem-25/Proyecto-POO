import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// encapsula la lectura y escritura del foro académico hacia la base de datos SQLite
// la clase Foro delega aquí cada vez que necesita persistir o recuperar información
// esto es para no mezclar lógica de negocio con lógica de acceso a datos

public class ForoDAO {

    public void guardarPublicacion(Publicacion p) {
        String sql = "INSERT INTO publicaciones (id, autor_id, anonimo, titulo, cuerpo, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getAutorReal().getIdUsuario());
            ps.setInt(3, p.esAnonimo() ? 1 : 0);
            ps.setString(4, p.getTitulo());
            ps.setString(5, p.getContenido());
            ps.setString(6, p.getFecha().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar la publicación en la base de datos", e);
        }

        for (Multimedia m : p.getArchivosAdjuntos()) {
            guardarMultimedia(m, p.getId());
        }
    }

    public void guardarComentario(Comentario c, String publicacionId, String comentarioPadreId) {
        String sql = "INSERT INTO comentarios "
                + "(id, publicacion_id, comentario_padre_id, autor_id, anonimo, cuerpo, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getId());
            ps.setString(2, publicacionId);
            ps.setString(3, comentarioPadreId);
            ps.setString(4, c.getAutorReal().getIdUsuario());
            ps.setInt(5, c.esAnonimo() ? 1 : 0);
            ps.setString(6, c.getContenido());
            ps.setString(7, c.getFecha().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el comentario en la base de datos", e);
        }
    }

    public void guardarMultimedia(Multimedia m, String publicacionId) {
        String sql = "INSERT INTO multimedia (id, publicacion_id, tipo, ruta_archivo) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, publicacionId);
            ps.setString(3, m.getTipo().name());
            ps.setString(4, m.getRutaArchivo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el archivo multimedia en la base de datos", e);
        }
    }

    // recupera todas las publicaciones guardadas, junto con sus archivos multimedia y sus comentarios 
    // incluyendo comentarios anidados

    public List<Publicacion> cargarPublicaciones(UsuarioRepositorio usuarioRepositorio) {
        List<Publicacion> resultado = new ArrayList<>();
        String sql = "SELECT id, autor_id, anonimo, titulo, cuerpo, fecha FROM publicaciones";

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Usuario autor = usuarioRepositorio.obtenerPorId(rs.getString("autor_id"));

                Publicacion publicacion = new Publicacion(
                        rs.getString("id"),
                        autor,
                        rs.getString("titulo"),
                        rs.getString("cuerpo"),
                        rs.getInt("anonimo") == 1,
                        LocalDateTime.parse(rs.getString("fecha"))
                );

                cargarMultimediaDe(publicacion);
                cargarComentariosDe(publicacion, usuarioRepositorio);
                resultado.add(publicacion);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar las publicaciones desde la base de datos", e);
        }

        return resultado;
    }

    private void cargarMultimediaDe(Publicacion publicacion) {
        String sql = "SELECT id, tipo, ruta_archivo FROM multimedia WHERE publicacion_id = ?";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, publicacion.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Multimedia m = new Multimedia(
                            rs.getString("id"),
                            TipoMultimedia.valueOf(rs.getString("tipo")),
                            rs.getString("ruta_archivo")
                    );
                    publicacion.getArchivosAdjuntos().add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar los archivos multimedia", e);
        }
    }

    // reconstruye los comentarios de una publicación y los enlaza entre sí
    // comentarios que responden a otros comentarios
    // esto se hace a partir de la columna comentario_padre_id.

    private void cargarComentariosDe(Publicacion publicacion, UsuarioRepositorio usuarioRepositorio) {
        String sql = "SELECT id, comentario_padre_id, autor_id, anonimo, cuerpo, fecha "
                + "FROM comentarios WHERE publicacion_id = ? ORDER BY fecha ASC";

        Map<String, Comentario> comentariosPorId = new HashMap<>();
        List<String[]> relacionesPadreHijo = new ArrayList<>();

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, publicacion.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String padreId = rs.getString("comentario_padre_id");
                    Usuario autor = usuarioRepositorio.obtenerPorId(rs.getString("autor_id"));

                    Comentario comentario = new Comentario(
                            id,
                            autor,
                            rs.getString("cuerpo"),
                            rs.getInt("anonimo") == 1,
                            LocalDateTime.parse(rs.getString("fecha"))
                    );

                    comentariosPorId.put(id, comentario);
                    relacionesPadreHijo.add(new String[]{id, padreId});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar los comentarios", e);
        }

        for (String[] relacion : relacionesPadreHijo) {
            String id = relacion[0];
            String padreId = relacion[1];
            Comentario actual = comentariosPorId.get(id);

            if (padreId == null) {
                publicacion.getRespuestas().add(actual);
            } else {
                Comentario padre = comentariosPorId.get(padreId);
                if (padre != null) {
                    padre.agregarRespuesta(actual);
                }
            }
        }
    }
}
