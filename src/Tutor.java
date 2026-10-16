import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;

public class Tutor extends Usuario {

    // Heredados de Usuario (NO se repiten aquí, ya vienen con "extends Usuario"):

    private String carrera;
    private int anioCarrera;
    private String especializacion;
    private String descripcionTutor;
    private List<Horario> horariosDisponibles;
    private List<String> materiasQueDicta;
    private String fotoMismo;
    private EstadoSolicitud estadoSolicitud;
    private Usuario aprobadoPor;

    public Tutor(String idUsuario, String nombreCompleto, String correo, String contrasenaHash) {
        super(idUsuario, nombreCompleto, correo, contrasenaHash);
        this.horariosDisponibles = new ArrayList<>();
        this.materiasQueDicta = new ArrayList<>();
        this.estadoSolicitud = EstadoSolicitud.PENDIENTE;
        this.aprobadoPor = null;
    }

    public boolean estaActivo() {
        return estadoSolicitud == EstadoSolicitud.APROBADO;
    }

    public void aprobarSolicitud(Usuario aprobador) {
        this.estadoSolicitud = EstadoSolicitud.APROBADO;
        this.aprobadoPor = aprobador;
    }

    public void rechazarSolicitud(Usuario aprobador) {
        this.estadoSolicitud = EstadoSolicitud.RECHAZADO;
        this.aprobadoPor = aprobador;
    }

    public void agregarHorario(Horario horario) {
        this.horariosDisponibles.add(horario);
    }

    public void materiasQueDicta(String materia) {
        this.materiasQueDicta.add(materia);
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public int getAnioCarrera() {
        return anioCarrera;
    }

    public void setAnioCarrera(int anioCarrera) {
        this.anioCarrera = anioCarrera;
    }

    public String getEspecializacion() {
        return especializacion;
    }

    public void setEspecializacion(String especializacion) {
        this.especializacion = especializacion;
    }

    public String getDescripcionTutor() {
        return descripcionTutor;
    }

    public void setDescripcionTutor(String descripcionTutor) {
        this.descripcionTutor = descripcionTutor;
    }

    public List<Horario> getHorarioDisponibles() {
        return horariosDisponibles;
    }

    public List<String> getMateriasQueDicta() {
        return materiasQueDicta;
    }

    public String getFotoMismo() {
        return fotoMismo;
    }

    public void setFotoMismo(String fotoMismo) {
        this.fotoMismo = fotoMismo;
    }

    public EstadoSolicitud getEstadoSolicitud() {
        return estadoSolicitud;
    }

    public Usuario getAprobadoPor() {
        return aprobadoPor;
    }
    public boolean guardarDatosTutor() {

    String sql = "INSERT OR REPLACE INTO tutores "
            + "(idTutor, estadoSolicitud) VALUES (?, ?)";

    try (Connection conexion = ConexionDB.conectar();
         PreparedStatement ps = conexion.prepareStatement(sql)) {

        ps.setInt(1, Integer.parseInt(getIdUsuario()));
        ps.setString(2, estadoSolicitud.name());

        ps.executeUpdate();
        return true;

    } catch (SQLException | NumberFormatException e) {
        System.out.println(
                "Error al guardar los datos del tutor: " + e.getMessage()
        );
        return false;
    }
}

public boolean guardarMateria(String materia) {

    if (materia == null || materia.trim().isEmpty()) {
        return false;
    }

    String sql = "INSERT INTO materias_tutor "
            + "(idTutor, materia) VALUES (?, ?)";

    try (Connection conexion = ConexionDB.conectar();
         PreparedStatement ps = conexion.prepareStatement(sql)) {

        ps.setInt(1, Integer.parseInt(getIdUsuario()));
        ps.setString(2, materia);

        ps.executeUpdate();

        if (!materiasQueDicta.contains(materia)) {
            materiasQueDicta.add(materia);
        }

        return true;

    } catch (SQLException | NumberFormatException e) {
        System.out.println(
                "Error al guardar la materia: " + e.getMessage()
        );
        return false;
    }
}

public boolean guardarHorario(Horario horario) {

    if (horario == null) {
        return false;
    }

    String sql = "INSERT INTO horarios_tutor "
            + "(idTutor, dia, horaInicio, horaFin) VALUES (?, ?, ?, ?)";

    try (Connection conexion = ConexionDB.conectar();
         PreparedStatement ps = conexion.prepareStatement(sql)) {

        ps.setInt(1, Integer.parseInt(getIdUsuario()));
        ps.setString(2, horario.getDia().name());
        ps.setString(3, horario.getHoraInicio().toString());
        ps.setString(4, horario.getHoraFin().toString());

        ps.executeUpdate();
        horariosDisponibles.add(horario);

        return true;

    } catch (SQLException | NumberFormatException e) {
        System.out.println(
                "Error al guardar el horario: " + e.getMessage()
        );
        return false;
        }
    }

    public void cargarDatosTutor() {

    int idTutor;

    try {
        idTutor = Integer.parseInt(getIdUsuario());
    } catch (NumberFormatException e) {
        System.out.println("ID de tutor inválido.");
        return;
    }

    String sqlTutor =
            "SELECT estadoSolicitud FROM tutores WHERE idTutor = ?";

    String sqlMaterias =
            "SELECT materia FROM materias_tutor WHERE idTutor = ?";

    String sqlHorarios =
            "SELECT dia, horaInicio, horaFin FROM horarios_tutor WHERE idTutor = ?";

    try (Connection conexion = ConexionDB.conectar()) {

        try (PreparedStatement ps = conexion.prepareStatement(sqlTutor)) {
            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    estadoSolicitud = EstadoSolicitud.valueOf(
                            rs.getString("estadoSolicitud")
                    );
                }
            }
        }

        materiasQueDicta.clear();

        try (PreparedStatement ps = conexion.prepareStatement(sqlMaterias)) {
            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    materiasQueDicta.add(rs.getString("materia"));
                }
            }
        }

        horariosDisponibles.clear();

        try (PreparedStatement ps = conexion.prepareStatement(sqlHorarios)) {
            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    DayOfWeek dia = DayOfWeek.valueOf(
                            rs.getString("dia")
                    );

                    LocalTime horaInicio = LocalTime.parse(
                            rs.getString("horaInicio")
                    );

                    LocalTime horaFin = LocalTime.parse(
                            rs.getString("horaFin")
                    );

                    horariosDisponibles.add(
                            new Horario(dia, horaInicio, horaFin)
                    );
                }
            }
        }

    } catch (SQLException | IllegalArgumentException e) {
        System.out.println(
                "Error al cargar los datos del tutor: " + e.getMessage()
        );
    }
}
    
}
