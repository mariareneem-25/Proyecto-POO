import java.sql.ResultSet;
import java.sql.SQLException;

public class Solicitud {

    public static final String CONSULTA =
            "SELECT s.idSolicitud, s.idEstudiante, s.idTutor, s.materia, s.mensaje, s.estado, s.motivoRechazo, "
            + "e.nombre AS nombreEstudiante, t.nombre AS nombreTutor "
            + "FROM solicitudes s "
            + "JOIN usuarios e ON e.idUsuario = s.idEstudiante "
            + "JOIN usuarios t ON t.idUsuario = s.idTutor ";

    private int idSolicitud;
    private int idEstudiante;
    private int idTutor;
    private String materia;
    private String mensaje;
    private EstadoSolicitud estado;
    private String motivoRechazo;
    private String nombreEstudiante;
    private String nombreTutor;

    public Solicitud(int idSolicitud, int idEstudiante, int idTutor, String materia, String mensaje,
                     EstadoSolicitud estado, String motivoRechazo, String nombreEstudiante, String nombreTutor) {
        this.idSolicitud = idSolicitud;
        this.idEstudiante = idEstudiante;
        this.idTutor = idTutor;
        this.materia = materia;
        this.mensaje = mensaje;
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
        this.nombreEstudiante = nombreEstudiante;
        this.nombreTutor = nombreTutor;
    }

    public static Solicitud desdeFila(ResultSet rs) throws SQLException {
        return new Solicitud(
                rs.getInt("idSolicitud"),
                rs.getInt("idEstudiante"),
                rs.getInt("idTutor"),
                rs.getString("materia"),
                rs.getString("mensaje"),
                EstadoSolicitud.valueOf(rs.getString("estado")),
                rs.getString("motivoRechazo"),
                rs.getString("nombreEstudiante"),
                rs.getString("nombreTutor")
        );
    }

    public int getIdSolicitud() { return idSolicitud; }
    public int getIdEstudiante() { return idEstudiante; }
    public int getIdTutor() { return idTutor; }
    public String getMateria() { return materia; }
    public String getMensaje() { return mensaje; }
    public EstadoSolicitud getEstado() { return estado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public String getNombreEstudiante() { return nombreEstudiante; }
    public String getNombreTutor() { return nombreTutor; }

    @Override
    public String toString() {
        String texto = "Solicitud #" + idSolicitud
                + " | Estudiante: " + nombreEstudiante
                + " | Tutor: " + nombreTutor
                + " | Materia: " + materia
                + " | Estado: " + estado;

        if (estado == EstadoSolicitud.RECHAZADO) {
            texto = texto + " | Motivo: " + motivoRechazo;
        }

        return texto;
    }
}