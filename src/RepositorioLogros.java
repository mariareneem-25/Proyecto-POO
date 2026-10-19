import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RepositorioLogros {

    private final Controlaracceso acceso = new Controlaracceso();

    public void agregarLogro(Perfil_usuario estudiante, String tema) {
        if (!acceso.puedeGestionarLogros(estudiante)) {
            throw new IllegalStateException("Solo un estudiante puede gestionar sus logros.");
        }

        if (tema == null || tema.trim().isEmpty()) {
            throw new IllegalArgumentException("El tema del logro es obligatorio.");
        }

        String sql = "INSERT INTO logros (idUsuario, tema, completado) VALUES (?, ?, 0)";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, estudiante.getIdUsuario());
            ps.setString(2, tema.trim());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el logro: " + e.getMessage());
        }
    }

    public void completarLogro(Perfil_usuario estudiante, String tema) {
        if (!acceso.puedeGestionarLogros(estudiante)) {
            throw new IllegalStateException("Solo un estudiante puede gestionar sus logros.");
        }

        if (tema == null || tema.trim().isEmpty()) {
            throw new IllegalArgumentException("El tema del logro es obligatorio.");
        }

        String sql = "UPDATE logros SET completado = 1 WHERE idUsuario = ? AND LOWER(tema) = LOWER(?)";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, estudiante.getIdUsuario());
            ps.setString(2, tema.trim());

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró un logro con ese tema.");
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el logro: " + e.getMessage());
        }
    }

    public List<Logro> obtenerLogros(Perfil_usuario solicitante, int idEstudiante) {
        if (!acceso.puedeVerInformacionDe(solicitante, idEstudiante)) {
            throw new IllegalStateException("No tienes permiso para ver los logros de este usuario.");
        }

        List<Logro> resultado = new ArrayList<>();
        String sql = "SELECT tema, completado FROM logros WHERE idUsuario = ? ORDER BY idLogro";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idEstudiante);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Logro logro = new Logro(rs.getString("tema"));
                    if (rs.getInt("completado") == 1) {
                        logro.marcarCompletado();
                    }
                    resultado.add(logro);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron obtener los logros: " + e.getMessage());
        }

        return resultado;
    }
}