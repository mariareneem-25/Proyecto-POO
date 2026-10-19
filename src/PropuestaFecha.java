import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PropuestaFecha {

    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static final String CONSULTA =
            "SELECT p.idPropuesta, p.idSolicitud, p.idProponente, p.fechaHora, p.estado, "
            + "s.materia, s.idEstudiante, e.nombre AS nombreEstudiante, t.nombre AS nombreTutor "
            + "FROM propuestasFecha p "
            + "JOIN solicitudes s ON s.idSolicitud = p.idSolicitud "
            + "JOIN usuarios e ON e.idUsuario = s.idEstudiante "
            + "JOIN usuarios t ON t.idUsuario = s.idTutor ";

    private int idPropuesta;
    private int idSolicitud;
    private int idProponente;
    private LocalDateTime fechaHora;
    private EstadoSolicitud estado;
    private String materia;
    private int idEstudiante;
    private String nombreEstudiante;
    private String nombreTutor;

    public PropuestaFecha(int idPropuesta, int idSolicitud, int idProponente, LocalDateTime fechaHora,
                          EstadoSolicitud estado, String materia, int idEstudiante,
                          String nombreEstudiante, String nombreTutor) {
        this.idPropuesta = idPropuesta;
        this.idSolicitud = idSolicitud;
        this.idProponente = idProponente;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.materia = materia;
        this.idEstudiante = idEstudiante;
        this.nombreEstudiante = nombreEstudiante;
        this.nombreTutor = nombreTutor;
    }

    public static PropuestaFecha desdeFila(ResultSet rs) throws SQLException {
        return new PropuestaFecha(
                rs.getInt("idPropuesta"),
                rs.getInt("idSolicitud"),
                rs.getInt("idProponente"),
                LocalDateTime.parse(rs.getString("fechaHora"), FORMATO),
                EstadoSolicitud.valueOf(rs.getString("estado")),
                rs.getString("materia"),
                rs.getInt("idEstudiante"),
                rs.getString("nombreEstudiante"),
                rs.getString("nombreTutor")
        );
    }

    public int getIdPropuesta() { return idPropuesta; }
    public int getIdSolicitud() { return idSolicitud; }
    public int getIdProponente() { return idProponente; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public EstadoSolicitud getEstado() { return estado; }
    public String getMateria() { return materia; }

    public String getNombreProponente() {
        if (idProponente == idEstudiante) {
            return nombreEstudiante;
        }
        return nombreTutor;
    }

    public String descripcionReserva() {
        return fechaHora.format(FORMATO)
                + " | Materia: " + materia
                + " | Estudiante: " + nombreEstudiante
                + " | Tutor: " + nombreTutor;
    }

    @Override
    public String toString() {
        return "Propuesta #" + idPropuesta
                + " | " + fechaHora.format(FORMATO)
                + " | Materia: " + materia
                + " | Propuesta por: " + getNombreProponente()
                + " | Estado: " + estado;
    }
}