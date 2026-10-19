import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
/*clase que funciona ara guardar y recuperar los datos del uturo en la base de datos del sistema*/
public class TutorDAO {
/*llama a los tres métodos que recuperan los datos de tutor*/
    public void cargarPerfil(Tutor tutor) {
        int idTutor = obtenerId(tutor);

        try (Connection conexion = ConexionDB.conectar()) {
            cargarDatosPerfil(conexion, tutor, idTutor);
            cargarMaterias(conexion, tutor, idTutor);
            cargarHorarios(conexion, tutor, idTutor);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo cargar el perfil del tutor.", e
            );
        }
    }

    private void cargarDatosPerfil(
            Connection conexion,
            Tutor tutor,
            int idTutor) throws SQLException {

        String sql =
                "SELECT carrera, anualidad, presentacion "
                + "FROM perfil_tutor WHERE idTutor = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tutor.setCarrera(rs.getString("carrera"));
                    tutor.setAnualidadCarrera(rs.getInt("anualidad"));
                    tutor.setPresentacion(rs.getString("presentacion"));
                } else {
                    tutor.setCarrera("");
                    tutor.setAnualidadCarrera(0);
                    tutor.setPresentacion("");
                }
            }
        }
    }

    private void cargarMaterias(
            Connection conexion,
            Tutor tutor,
            int idTutor) throws SQLException {
/*busca las materias de cada tutor por su identificador*/
        String sql =
                "SELECT materia FROM tutor_materias "
                + "WHERE idTutor = ? ORDER BY materia";

        tutor.limpiarMaterias();

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tutor.agregarMateria(rs.getString("materia"));
                }
            }
        }
    }

    private void cargarHorarios(
            Connection conexion,
            Tutor tutor,
            int idTutor) throws SQLException {

        String sql =
                "SELECT dia, inicio, fin FROM tutor_horarios "
                + "WHERE idTutor = ? ORDER BY rowid";

        tutor.limpiarHorarios();

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Horario horario = new Horario(
                            DayOfWeek.valueOf(rs.getString("dia")),
                            LocalTime.parse(rs.getString("inicio")),
                            LocalTime.parse(rs.getString("fin"))
                    );

                    tutor.agregarHorario(horario);
                }
            }
        }
    }

    public void guardar(Tutor tutor, boolean enviar) {
        int idTutor = obtenerId(tutor);

        if (enviar && !tutor.perfilCompleto()) {
            throw new IllegalStateException(
                    "Antes de enviar debes completar: "
                    + String.join(", ", tutor.camposPendientes())
            );
        }

        try (Connection conexion = ConexionDB.conectar()) {
            conexion.setAutoCommit(false);

            try {
                validarPuedeEditar(conexion, idTutor);

                guardarDatosPerfil(conexion, tutor, idTutor);
                guardarMaterias(conexion, tutor, idTutor);
                guardarHorarios(conexion, tutor, idTutor);

                if (enviar) {
                    registrarSolicitud(conexion, idTutor);
                }

                conexion.commit();

            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo guardar el perfil o enviar la solicitud.",
                    e
            );
        }
    }

    private void validarPuedeEditar(
            Connection conexion,
            int idTutor) throws SQLException {

        String sql =
                "SELECT u.estadoCuenta, "
                + "COALESCE(s.estado, 'BORRADOR') AS solicitud "
                + "FROM usuarios u "
                + "LEFT JOIN solicitudes_tutor s "
                + "ON s.idTutor = u.idUsuario "
                + "WHERE u.idUsuario = ? "
                + "AND u.tipoUsuario = 'TUTOR'";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException(
                            "No se encontró la cuenta del tutor."
                    );
                }

                String estadoCuenta = rs.getString("estadoCuenta");
                String estadoSolicitud = rs.getString("solicitud");

                boolean cuentaPendiente =
                        "PENDIENTE".equals(estadoCuenta);

                boolean solicitudEditable =
                        "BORRADOR".equals(estadoSolicitud)
                        || "RECHAZADO".equals(estadoSolicitud);

                if (!cuentaPendiente || !solicitudEditable) {
                    throw new IllegalStateException(
                            "Solo puedes guardar o enviar un perfil "
                            + "en borrador o rechazado, "
                            + "con la cuenta pendiente."
                    );
                }
            }
        }
    }

    private void guardarDatosPerfil(
            Connection conexion,
            Tutor tutor,
            int idTutor) throws SQLException {

        String sql =
                "INSERT INTO perfil_tutor "
                + "(idTutor, carrera, anualidad, presentacion) "
                + "VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(idTutor) DO UPDATE SET "
                + "carrera = excluded.carrera, "
                + "anualidad = excluded.anualidad, "
                + "presentacion = excluded.presentacion";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTutor);
            ps.setString(2, tutor.getCarrera());
            ps.setInt(3, tutor.getAnualidadCarrera());
            ps.setString(4, tutor.getPresentacion());

            ps.executeUpdate();
        }
    }

    private void guardarMaterias(
            Connection conexion,
            Tutor tutor,
            int idTutor) throws SQLException {

        String eliminar =
                "DELETE FROM tutor_materias WHERE idTutor = ?";

        try (PreparedStatement ps =
                     conexion.prepareStatement(eliminar)) {

            ps.setInt(1, idTutor);
            ps.executeUpdate();
        }

        String insertar =
                "INSERT INTO tutor_materias "
                + "(idTutor, materia) VALUES (?, ?)";

        try (PreparedStatement ps =
                     conexion.prepareStatement(insertar)) {

            for (String materia : tutor.getMateriasQueDicta()) {
                ps.setInt(1, idTutor);
                ps.setString(2, materia);
                ps.executeUpdate();
            }
        }
    }

    private void guardarHorarios(
            Connection conexion,
            Tutor tutor,
            int idTutor) throws SQLException {

        String eliminar =
                "DELETE FROM tutor_horarios WHERE idTutor = ?";

        try (PreparedStatement ps =
                     conexion.prepareStatement(eliminar)) {

            ps.setInt(1, idTutor);
            ps.executeUpdate();
        }

        String insertar =
                "INSERT INTO tutor_horarios "
                + "(idTutor, dia, inicio, fin) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps =
                     conexion.prepareStatement(insertar)) {

            for (Horario horario : tutor.getHorariosDisponibles()) {
                ps.setInt(1, idTutor);
                ps.setString(2, horario.getDia().name());
                ps.setString(3, horario.getHoraInicio().toString());
                ps.setString(4, horario.getHoraFin().toString());

                ps.executeUpdate();
            }
        }
    }

    private void registrarSolicitud(
            Connection conexion,
            int idTutor) throws SQLException {

        String sql =
                "INSERT INTO solicitudes_tutor "
                + "(idTutor, estado, aprobadoPor) "
                + "VALUES (?, 'PENDIENTE', NULL) "
                + "ON CONFLICT(idTutor) DO UPDATE SET "
                + "estado = 'PENDIENTE', aprobadoPor = NULL";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTutor);
            ps.executeUpdate();
        }
    }

    private int obtenerId(Tutor tutor) {
        if (tutor == null) {
            throw new IllegalArgumentException(
                    "El tutor no puede ser nulo."
            );
        }

        try {
            int id = Integer.parseInt(tutor.getIdUsuario());

            if (id <= 0) {
                throw new IllegalArgumentException(
                        "El tutor debe estar registrado antes de guardar su perfil."
                );
            }

            return id;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El identificador del tutor debe ser numérico.",
                    e
            );
        }
    }
}