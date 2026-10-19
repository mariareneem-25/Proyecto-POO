import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Gestionarsolicitudes {

    private final Controlaracceso acceso = new Controlaracceso();
    private final RepositorioLogros repositorioLogros = new RepositorioLogros();
    private final GestionarCalendario calendario = new GestionarCalendario();

    public List<String> listarTutores(Perfil_usuario estudiante) {
        if (!acceso.puedeEnviarSolicitud(estudiante)) {
            throw new IllegalStateException("Solo un estudiante puede ver la lista de tutores.");
        }

        List<String> tutores = new ArrayList<>();
        String sql = "SELECT idUsuario, nombre FROM usuarios WHERE tipoUsuario = 'TUTOR' AND estadoCuenta = 'ACTIVA'";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tutores.add(rs.getInt("idUsuario") + " - " + rs.getString("nombre"));
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo obtener la lista de tutores: " + e.getMessage());
        }

        return tutores;
    }

    public int crearSolicitud(Perfil_usuario estudiante, int idTutor, String materia, String mensaje) {
        if (!acceso.puedeEnviarSolicitud(estudiante)) {
            throw new IllegalStateException("Solo un estudiante puede enviar solicitudes de tutoría.");
        }

        if (materia == null || materia.trim().isEmpty()) {
            throw new IllegalArgumentException("La materia es obligatoria.");
        }

        if (mensaje == null || mensaje.trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje para el tutor es obligatorio.");
        }

        if (!ConexionDB.existe("SELECT 1 FROM usuarios WHERE idUsuario = ? AND tipoUsuario = 'TUTOR' "
                + "AND estadoCuenta = 'ACTIVA'", idTutor)) {
            throw new IllegalArgumentException("El tutor indicado no existe.");
        }

        if (ConexionDB.existe("SELECT 1 FROM solicitudes WHERE idEstudiante = ? AND idTutor = ? "
                + "AND estado IN ('PENDIENTE', 'APROBADO')", estudiante.getIdUsuario(), idTutor)) {
            throw new IllegalStateException("Ya tienes una solicitud pendiente o aceptada con este tutor.");
        }

        String sql = "INSERT INTO solicitudes (idEstudiante, idTutor, materia, mensaje, estado) VALUES (?, ?, ?, ?, ?)";
        int idSolicitud = 0;

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, estudiante.getIdUsuario());
            ps.setInt(2, idTutor);
            ps.setString(3, materia.trim());
            ps.setString(4, mensaje.trim());
            ps.setString(5, EstadoSolicitud.PENDIENTE.name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    idSolicitud = rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la solicitud: " + e.getMessage());
        }

        return idSolicitud;
    }

    public List<Solicitud> listarPendientes(Perfil_usuario tutor) {
        if (!acceso.puedeGestionarSolicitudes(tutor)) {
            throw new IllegalStateException("Solo un tutor puede ver las solicitudes recibidas.");
        }

        return buscar("WHERE s.idTutor = ? AND s.estado = 'PENDIENTE' ORDER BY s.idSolicitud", tutor.getIdUsuario());
    }

    public List<Solicitud> listarMisSolicitudes(Perfil_usuario estudiante) {
        if (!acceso.puedeEnviarSolicitud(estudiante)) {
            throw new IllegalStateException("Solo un estudiante puede ver sus solicitudes enviadas.");
        }

        return buscar("WHERE s.idEstudiante = ? ORDER BY s.idSolicitud", estudiante.getIdUsuario());
    }

    public String consultarSolicitud(Perfil_usuario tutor, int idSolicitud) {
        if (!acceso.puedeGestionarSolicitud(tutor, idSolicitud)) {
            throw new IllegalStateException("No tienes permiso para consultar esta solicitud o no existe.");
        }

        Solicitud solicitud = obtenerPorId(idSolicitud);

        if (solicitud.getEstado() == EstadoSolicitud.RECHAZADO) {
            throw new IllegalStateException("La solicitud fue rechazada, ya no puedes ver la información del estudiante.");
        }

        int idEstudiante = solicitud.getIdEstudiante();
        StringBuilder texto = new StringBuilder();

        texto.append("Solicitud #").append(solicitud.getIdSolicitud()).append("\n");
        texto.append("Materia: ").append(solicitud.getMateria()).append("\n");
        texto.append("Estado: ").append(solicitud.getEstado()).append("\n");
        texto.append("Mensaje del estudiante: ").append(solicitud.getMensaje()).append("\n");

        texto.append("\n--- Perfil del estudiante ---\n");
        texto.append(obtenerPerfil(idEstudiante)).append("\n");

        texto.append("\n--- Logros ---\n");
        List<Logro> listaLogros = repositorioLogros.obtenerLogros(tutor, idEstudiante);
        if (listaLogros.isEmpty()) {
            texto.append("El estudiante no tiene logros registrados.\n");
        }
        for (Logro logro : listaLogros) {
            texto.append(logro).append("\n");
        }

        texto.append("\n--- Calendario (fechas reservadas) ---\n");
        List<PropuestaFecha> reservadas = calendario.obtenerReservadas(tutor, idEstudiante);
        if (reservadas.isEmpty()) {
            texto.append("El estudiante no tiene fechas reservadas.\n");
        }
        for (PropuestaFecha reserva : reservadas) {
            texto.append(reserva.descripcionReserva()).append("\n");
        }

        return texto.toString();
    }

    public void aceptarSolicitud(Perfil_usuario tutor, int idSolicitud) {
        validarSolicitudPendiente(tutor, idSolicitud);
        cambiarEstado(idSolicitud, EstadoSolicitud.APROBADO, null);
    }

    public void rechazarSolicitud(Perfil_usuario tutor, int idSolicitud, String motivo) {
        validarSolicitudPendiente(tutor, idSolicitud);

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Debes indicar el motivo del rechazo.");
        }

        cambiarEstado(idSolicitud, EstadoSolicitud.RECHAZADO, motivo.trim());
    }

    private void validarSolicitudPendiente(Perfil_usuario tutor, int idSolicitud) {
        if (!acceso.puedeGestionarSolicitud(tutor, idSolicitud)) {
            throw new IllegalStateException("No tienes permiso para responder esta solicitud o no existe.");
        }

        Solicitud solicitud = obtenerPorId(idSolicitud);

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("Esta solicitud ya fue respondida.");
        }
    }

    private void cambiarEstado(int idSolicitud, EstadoSolicitud estado, String motivo) {
        String sql = "UPDATE solicitudes SET estado = ?, motivoRechazo = ? WHERE idSolicitud = ?";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, estado.name());
            ps.setString(2, motivo);
            ps.setInt(3, idSolicitud);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar la solicitud: " + e.getMessage());
        }
    }

    private Solicitud obtenerPorId(int idSolicitud) {
        List<Solicitud> resultado = buscar("WHERE s.idSolicitud = ?", idSolicitud);

        if (resultado.isEmpty()) {
            throw new IllegalArgumentException("No existe la solicitud indicada.");
        }

        return resultado.get(0);
    }

    private List<Solicitud> buscar(String filtro, int parametro) {
        List<Solicitud> resultado = new ArrayList<>();

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(Solicitud.CONSULTA + filtro)) {

            ps.setInt(1, parametro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(Solicitud.desdeFila(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron obtener las solicitudes: " + e.getMessage());
        }

        return resultado;
    }

  private String obtenerPerfil(int idEstudiante) {
    String sql = "SELECT nombre, correoElectronico, fotoPerfil, descripcion, banner "
            + "FROM usuarios WHERE idUsuario = ?";

    try (Connection conexion = ConexionDB.conectar();
         PreparedStatement ps = conexion.prepareStatement(sql)) {

        ps.setInt(1, idEstudiante);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return "Nombre: " + rs.getString("nombre")
                        + "\nCorreo: " + rs.getString("correoElectronico")
                        + "\nFoto de perfil: " + rs.getString("fotoPerfil")
                        + "\nDescripcion: " + rs.getString("descripcion")
                        + "\nBanner: " + rs.getString("banner");
            }
        }

    } catch (SQLException e) {
        throw new IllegalStateException(
                "No se pudo obtener el perfil: " + e.getMessage()
        );
    }

    return "Perfil no disponible.";
}
}
