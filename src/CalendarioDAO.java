import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// calendario de estudiantes y tutores (tablas en proyecto.db)
//
// marcas: días que cada persona marca como OCUPADO o como FECHA DE PREFERENCIA (las edita ella misma)
// reservas: tutorías acordadas entre un estudiante y un tutor; NO se editan a mano
//           solo se crean cuando la otra persona acepta una solicitud hecha por el chat
//           y solo se eliminan cuando la otra persona acepta una solicitud de eliminar
// solicitudes_calendario: las solicitudes de reservar o eliminar una fecha que se mandan por el chat

public class CalendarioDAO {

    public enum TipoMarca { OCUPADO, PREFERENCIA }

    public enum TipoSolicitud { RESERVAR, ELIMINAR }

    // una reserva dura una hora; dos reservas de la misma persona no pueden empalmarse
    private static final int MINUTOS_POR_RESERVA = 60;

    public static class Marca {
        public final LocalDate fecha;
        public final TipoMarca tipo;

        public Marca(LocalDate fecha, TipoMarca tipo) {
            this.fecha = fecha;
            this.tipo = tipo;
        }
    }

    public static class Reserva {
        public final int id;
        public final int idEstudiante;
        public final int idTutor;
        public final LocalDate fecha;
        public final LocalTime hora;
        public final String nombreEstudiante;
        public final String nombreTutor;

        public Reserva(int id, int idEstudiante, int idTutor, LocalDate fecha, LocalTime hora,
                       String nombreEstudiante, String nombreTutor) {
            this.id = id;
            this.idEstudiante = idEstudiante;
            this.idTutor = idTutor;
            this.fecha = fecha;
            this.hora = hora;
            this.nombreEstudiante = nombreEstudiante;
            this.nombreTutor = nombreTutor;
        }
    }

    public static class SolicitudCalendario {
        public final int id;
        public final int idEstudiante;
        public final int idTutor;
        public final int idEmisor;
        public final TipoSolicitud tipo;
        public final LocalDate fecha;
        public final LocalTime hora;
        public final Integer idReserva;
        public final String nombreEmisor;

        public SolicitudCalendario(int id, int idEstudiante, int idTutor, int idEmisor, TipoSolicitud tipo,
                                   LocalDate fecha, LocalTime hora, Integer idReserva, String nombreEmisor) {
            this.id = id;
            this.idEstudiante = idEstudiante;
            this.idTutor = idTutor;
            this.idEmisor = idEmisor;
            this.tipo = tipo;
            this.fecha = fecha;
            this.hora = hora;
            this.idReserva = idReserva;
            this.nombreEmisor = nombreEmisor;
        }

        public String descripcion() {
            return (tipo == TipoSolicitud.RESERVAR ? "Reservar " : "Eliminar la reserva del ")
                    + fecha + " a las " + hora;
        }
    }

    public CalendarioDAO() {
        crearTablas();
    }

    private void crearTablas() {
        String marcas = "CREATE TABLE IF NOT EXISTS calendario_marcas ("
                + "idMarca INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "idUsuario INTEGER NOT NULL, "
                + "fecha TEXT NOT NULL, "
                + "tipo TEXT NOT NULL, "
                + "UNIQUE (idUsuario, fecha))";

        String reservas = "CREATE TABLE IF NOT EXISTS reservas ("
                + "idReserva INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "idEstudiante INTEGER NOT NULL, "
                + "idTutor INTEGER NOT NULL, "
                + "fecha TEXT NOT NULL, "
                + "hora TEXT NOT NULL, "
                + "estado TEXT NOT NULL)";

        String solicitudes = "CREATE TABLE IF NOT EXISTS solicitudes_calendario ("
                + "idSolicitudCal INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "idEstudiante INTEGER NOT NULL, "
                + "idTutor INTEGER NOT NULL, "
                + "idEmisor INTEGER NOT NULL, "
                + "tipo TEXT NOT NULL, "
                + "fecha TEXT NOT NULL, "
                + "hora TEXT NOT NULL, "
                + "idReserva INTEGER, "
                + "estado TEXT NOT NULL)";

        try (Connection conexion = ConexionDB.conectar();
             Statement st = conexion.createStatement()) {
            st.execute(marcas);
            st.execute(reservas);
            st.execute(solicitudes);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo preparar el calendario: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Marcas (ocupado / fecha de preferencia)
    // ---------------------------------------------------------------

    // guarda todas las fechas juntas o ninguna
    public void guardarMarcas(int idUsuario, List<LocalDate> fechas, TipoMarca tipo) {
        for (LocalDate fecha : fechas) {
            if (fecha.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("No puedes marcar días que ya pasaron (" + fecha + ").");
            }
            if (tieneReserva(idUsuario, fecha)) {
                throw new IllegalStateException("El " + fecha + " ya tiene una reserva. "
                        + "Para quitarla, envía una solicitud de eliminar la fecha por el chat.");
            }
        }

        String sql = "INSERT INTO calendario_marcas (idUsuario, fecha, tipo) VALUES (?, ?, ?) "
                + "ON CONFLICT(idUsuario, fecha) DO UPDATE SET tipo = excluded.tipo";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            for (LocalDate fecha : fechas) {
                ps.setInt(1, idUsuario);
                ps.setString(2, fecha.toString());
                ps.setString(3, tipo.name());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron guardar las fechas: " + e.getMessage());
        }
    }

    public void eliminarMarcas(int idUsuario, List<LocalDate> fechas) {
        String sql = "DELETE FROM calendario_marcas WHERE idUsuario = ? AND fecha = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            for (LocalDate fecha : fechas) {
                ps.setInt(1, idUsuario);
                ps.setString(2, fecha.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron eliminar las fechas: " + e.getMessage());
        }
    }

    // marcas de hoy en adelante
    public List<Marca> listarMarcas(int idUsuario) {
        List<Marca> lista = new ArrayList<>();
        String sql = "SELECT fecha, tipo FROM calendario_marcas WHERE idUsuario = ? AND fecha >= ? ORDER BY fecha";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setString(2, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Marca(LocalDate.parse(rs.getString("fecha")),
                            TipoMarca.valueOf(rs.getString("tipo"))));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer el calendario: " + e.getMessage());
        }
        return lista;
    }

    private boolean estaOcupado(int idUsuario, LocalDate fecha) {
        return ConexionDB.existe("SELECT 1 FROM calendario_marcas WHERE idUsuario = ? AND fecha = ? AND tipo = ?",
                idUsuario, fecha.toString(), TipoMarca.OCUPADO.name());
    }

    // ---------------------------------------------------------------
    // Reservas
    // ---------------------------------------------------------------

    private static final String CONSULTA_RESERVAS =
            "SELECT r.idReserva, r.idEstudiante, r.idTutor, r.fecha, r.hora, "
            + "e.nombre AS nombreEstudiante, t.nombre AS nombreTutor "
            + "FROM reservas r "
            + "JOIN usuarios e ON e.idUsuario = r.idEstudiante "
            + "JOIN usuarios t ON t.idUsuario = r.idTutor "
            + "WHERE r.estado = 'ACTIVA' AND r.fecha >= ? ";

    // reservas de hoy en adelante donde la persona participa (como estudiante o como tutor)
    public List<Reserva> listarReservas(int idUsuario) {
        return consultarReservas("AND (r.idEstudiante = ? OR r.idTutor = ?) ORDER BY r.fecha, r.hora",
                idUsuario, idUsuario);
    }

    // reservas de hoy en adelante entre un estudiante y un tutor
    public List<Reserva> listarReservasDelPar(int idEstudiante, int idTutor) {
        return consultarReservas("AND r.idEstudiante = ? AND r.idTutor = ? ORDER BY r.fecha, r.hora",
                idEstudiante, idTutor);
    }

    private List<Reserva> consultarReservas(String filtro, int a, int b) {
        List<Reserva> lista = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(CONSULTA_RESERVAS + filtro)) {

            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, a);
            ps.setInt(3, b);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Reserva(
                            rs.getInt("idReserva"), rs.getInt("idEstudiante"), rs.getInt("idTutor"),
                            LocalDate.parse(rs.getString("fecha")), LocalTime.parse(rs.getString("hora")),
                            rs.getString("nombreEstudiante"), rs.getString("nombreTutor")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron leer las reservas: " + e.getMessage());
        }
        return lista;
    }

    private boolean tieneReserva(int idUsuario, LocalDate fecha) {
        return ConexionDB.existe("SELECT 1 FROM reservas WHERE estado = 'ACTIVA' AND fecha = ? "
                + "AND (idEstudiante = ? OR idTutor = ?)", fecha.toString(), idUsuario, idUsuario);
    }

    // la persona ya tiene una reserva que se empalma con esa fecha y hora
    private boolean hayEmpalme(int idUsuario, LocalDate fecha, LocalTime hora) {
        for (Reserva r : listarReservas(idUsuario)) {
            if (r.fecha.equals(fecha)
                    && Math.abs(Duration.between(r.hora, hora).toMinutes()) < MINUTOS_POR_RESERVA) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Solicitudes de reservar o eliminar (se envían por el chat)
    // ---------------------------------------------------------------

    public int solicitarReserva(int idEmisor, int idEstudiante, int idTutor, LocalDate fecha, LocalTime hora) {
        validarParticipante(idEmisor, idEstudiante, idTutor);
        validarReservable(idEstudiante, idTutor, fecha, hora);

        if (ConexionDB.existe("SELECT 1 FROM solicitudes_calendario WHERE estado = 'PENDIENTE' AND tipo = ? "
                + "AND idEstudiante = ? AND idTutor = ? AND fecha = ? AND hora = ?",
                TipoSolicitud.RESERVAR.name(), idEstudiante, idTutor, fecha.toString(), hora.toString())) {
            throw new IllegalStateException("Ya hay una solicitud pendiente para esa fecha y hora.");
        }

        return insertarSolicitud(idEmisor, idEstudiante, idTutor, TipoSolicitud.RESERVAR, fecha, hora, null);
    }

    public int solicitarEliminacion(int idEmisor, int idReserva) {
        Reserva reserva = obtenerReserva(idReserva);
        if (reserva == null) {
            throw new IllegalArgumentException("Esa reserva no existe o ya fue eliminada.");
        }
        validarParticipante(idEmisor, reserva.idEstudiante, reserva.idTutor);

        if (ConexionDB.existe("SELECT 1 FROM solicitudes_calendario WHERE estado = 'PENDIENTE' AND tipo = ? "
                + "AND idReserva = ?", TipoSolicitud.ELIMINAR.name(), idReserva)) {
            throw new IllegalStateException("Ya hay una solicitud pendiente para eliminar esa reserva.");
        }

        return insertarSolicitud(idEmisor, reserva.idEstudiante, reserva.idTutor,
                TipoSolicitud.ELIMINAR, reserva.fecha, reserva.hora, idReserva);
    }

    // solicitudes pendientes entre el estudiante y el tutor (enviadas por cualquiera de los dos)
    public List<SolicitudCalendario> pendientesDelPar(int idEstudiante, int idTutor) {
        List<SolicitudCalendario> lista = new ArrayList<>();
        String sql = "SELECT s.idSolicitudCal, s.idEstudiante, s.idTutor, s.idEmisor, s.tipo, s.fecha, s.hora, "
                + "s.idReserva, u.nombre AS nombreEmisor "
                + "FROM solicitudes_calendario s JOIN usuarios u ON u.idUsuario = s.idEmisor "
                + "WHERE s.estado = 'PENDIENTE' AND s.idEstudiante = ? AND s.idTutor = ? "
                + "ORDER BY s.idSolicitudCal";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idEstudiante);
            ps.setInt(2, idTutor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int reserva = rs.getInt("idReserva");
                    Integer idReserva = rs.wasNull() ? null : reserva;
                    lista.add(new SolicitudCalendario(
                            rs.getInt("idSolicitudCal"), rs.getInt("idEstudiante"), rs.getInt("idTutor"),
                            rs.getInt("idEmisor"), TipoSolicitud.valueOf(rs.getString("tipo")),
                            LocalDate.parse(rs.getString("fecha")), LocalTime.parse(rs.getString("hora")),
                            idReserva, rs.getString("nombreEmisor")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron leer las solicitudes: " + e.getMessage());
        }
        return lista;
    }

    // solo responde la persona a la que le mandaron la solicitud (no quien la envió)
    // devuelve un texto que describe lo que pasó, para avisarlo en el chat
    public String responder(int idSolicitudCal, int idReceptor, boolean aceptar) {
        SolicitudCalendario solicitud = obtenerSolicitud(idSolicitudCal);
        if (solicitud == null) {
            throw new IllegalArgumentException("Esa solicitud ya no está pendiente.");
        }
        validarParticipante(idReceptor, solicitud.idEstudiante, solicitud.idTutor);
        if (solicitud.idEmisor == idReceptor) {
            throw new IllegalStateException("Esa solicitud la debe responder la otra persona.");
        }

        if (!aceptar) {
            cambiarEstadoSolicitud(idSolicitudCal, "RECHAZADA");
            return "Solicitud rechazada: " + solicitud.descripcion();
        }

        if (solicitud.tipo == TipoSolicitud.RESERVAR) {
            validarReservable(solicitud.idEstudiante, solicitud.idTutor, solicitud.fecha, solicitud.hora);
            insertarReserva(solicitud);
            cambiarEstadoSolicitud(idSolicitudCal, "ACEPTADA");
            return "Reserva aceptada: " + solicitud.fecha + " a las " + solicitud.hora + ". Ya está en ambos calendarios.";
        }

        actualizar("UPDATE reservas SET estado = 'CANCELADA' WHERE idReserva = ?", solicitud.idReserva);
        cambiarEstadoSolicitud(idSolicitudCal, "ACEPTADA");
        return "Eliminación aceptada: se quitó la reserva del " + solicitud.fecha + " a las " + solicitud.hora
                + " de ambos calendarios.";
    }

    // ---------------------------------------------------------------
    // Texto del calendario (para mostrarlo)
    // ---------------------------------------------------------------

    // calendario de hoy en adelante, ordenado por fecha
    // conQuien = true muestra con quién es cada reserva (solo para el dueño del calendario)
    public String resumen(int idUsuario, boolean conQuien) {
        List<String[]> filas = new ArrayList<>();

        for (Marca m : listarMarcas(idUsuario)) {
            String etiqueta = m.tipo == TipoMarca.OCUPADO ? "Ocupado" : "Fecha de preferencia";
            filas.add(new String[]{m.fecha.toString() + " 00:00", m.fecha + "            | " + etiqueta});
        }
        for (Reserva r : listarReservas(idUsuario)) {
            String detalle = "Reservado";
            if (conQuien) {
                String otro = (r.idEstudiante == idUsuario) ? "Tutor: " + nombre(r.nombreTutor)
                        : "Estudiante: " + nombre(r.nombreEstudiante);
                detalle = "Reservado | " + otro;
            }
            filas.add(new String[]{r.fecha + " " + r.hora, r.fecha + " " + r.hora + " | " + detalle});
        }

        if (filas.isEmpty()) {
            return "Sin fechas marcadas ni reservadas.\n";
        }

        filas.sort((x, y) -> x[0].compareTo(y[0]));
        StringBuilder texto = new StringBuilder();
        for (String[] fila : filas) {
            texto.append(fila[1]).append("\n");
        }
        return texto.toString();
    }

    private String nombre(String nombre) {
        return (nombre == null || nombre.isBlank()) ? "(sin nombre)" : nombre;
    }

    // ---------------------------------------------------------------
    // Utilidades internas
    // ---------------------------------------------------------------

    private void validarParticipante(int idPersona, int idEstudiante, int idTutor) {
        if (idPersona != idEstudiante && idPersona != idTutor) {
            throw new IllegalStateException("No participas en esta conversación.");
        }
    }

    // la fecha debe ser futura, ninguno la tiene marcada como ocupado y ninguno tiene otra reserva empalmada
    private void validarReservable(int idEstudiante, int idTutor, LocalDate fecha, LocalTime hora) {
        if (!LocalDateTime.of(fecha, hora).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha y hora deben ser futuras.");
        }
        if (estaOcupado(idEstudiante, fecha) || estaOcupado(idTutor, fecha)) {
            throw new IllegalStateException("Alguno de los dos tiene ese día marcado como ocupado.");
        }
        if (hayEmpalme(idEstudiante, fecha, hora) || hayEmpalme(idTutor, fecha, hora)) {
            throw new IllegalStateException("Alguno de los dos ya tiene una reserva que se empalma con esa hora.");
        }
    }

    private Reserva obtenerReserva(int idReserva) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT r.idReserva, r.idEstudiante, r.idTutor, r.fecha, r.hora, "
                + "e.nombre AS nombreEstudiante, t.nombre AS nombreTutor "
                + "FROM reservas r JOIN usuarios e ON e.idUsuario = r.idEstudiante "
                + "JOIN usuarios t ON t.idUsuario = r.idTutor "
                + "WHERE r.estado = 'ACTIVA' AND r.idReserva = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lista.add(new Reserva(
                            rs.getInt("idReserva"), rs.getInt("idEstudiante"), rs.getInt("idTutor"),
                            LocalDate.parse(rs.getString("fecha")), LocalTime.parse(rs.getString("hora")),
                            rs.getString("nombreEstudiante"), rs.getString("nombreTutor")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer la reserva: " + e.getMessage());
        }
        return lista.isEmpty() ? null : lista.get(0);
    }

    private SolicitudCalendario obtenerSolicitud(int idSolicitudCal) {
        String sql = "SELECT s.idSolicitudCal, s.idEstudiante, s.idTutor, s.idEmisor, s.tipo, s.fecha, s.hora, "
                + "s.idReserva, u.nombre AS nombreEmisor "
                + "FROM solicitudes_calendario s JOIN usuarios u ON u.idUsuario = s.idEmisor "
                + "WHERE s.idSolicitudCal = ? AND s.estado = 'PENDIENTE'";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idSolicitudCal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int reserva = rs.getInt("idReserva");
                    Integer idReserva = rs.wasNull() ? null : reserva;
                    return new SolicitudCalendario(
                            rs.getInt("idSolicitudCal"), rs.getInt("idEstudiante"), rs.getInt("idTutor"),
                            rs.getInt("idEmisor"), TipoSolicitud.valueOf(rs.getString("tipo")),
                            LocalDate.parse(rs.getString("fecha")), LocalTime.parse(rs.getString("hora")),
                            idReserva, rs.getString("nombreEmisor"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer la solicitud: " + e.getMessage());
        }
        return null;
    }

    private int insertarSolicitud(int idEmisor, int idEstudiante, int idTutor, TipoSolicitud tipo,
                                  LocalDate fecha, LocalTime hora, Integer idReserva) {
        String sql = "INSERT INTO solicitudes_calendario "
                + "(idEstudiante, idTutor, idEmisor, tipo, fecha, hora, idReserva, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE')";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idEstudiante);
            ps.setInt(2, idTutor);
            ps.setInt(3, idEmisor);
            ps.setString(4, tipo.name());
            ps.setString(5, fecha.toString());
            ps.setString(6, hora.toString());
            ps.setObject(7, idReserva);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la solicitud: " + e.getMessage());
        }
    }

    private void insertarReserva(SolicitudCalendario s) {
        actualizar("INSERT INTO reservas (idEstudiante, idTutor, fecha, hora, estado) VALUES (?, ?, ?, ?, 'ACTIVA')",
                s.idEstudiante, s.idTutor, s.fecha.toString(), s.hora.toString());
    }

    private void cambiarEstadoSolicitud(int idSolicitudCal, String estado) {
        actualizar("UPDATE solicitudes_calendario SET estado = ? WHERE idSolicitudCal = ?", estado, idSolicitudCal);
    }

    private void actualizar(String sql, Object... parametros) {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error en la base de datos: " + e.getMessage());
        }
    }
}
