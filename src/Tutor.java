import java.util.ArrayList;
import java.util.List; 

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
        this.calificacion=null;
        this.calificadoPor=null;
    }

    public boolean estaActivo() {
        return estadoSolicitud == EstadoSolicitud.APROBADO;
    }
    /*conprueba que el usuario sea docente o directo */
    private void verificarPermisoValidacion(Usuario usuario){
        if (!(usuario instanceof Docente)
                !(usuario instanceof Director)){
                    throw new SecurityException(
                        "Solo docentes y directores pueden validar tutores."
                    );
                }
    }
    /*Se aprueba la solicitud y registro*/ 
    public void aprobarSolicitud(usuario aprobador){
        verificarPermisoValidacion(aprobador);
        this.estadoSolicitud = EstadoSolicitud.APROBADO;
        this.aprobadoPor = aprobador;
    }

    /*Se rechaza la solicitud y registro*/
    public void rechazarSolicitud(Usuario aprobador){
        verificarPermisoValidacion(aprobador);
        this.estadoSolicitud = EstadoSolicitud.RECHAZADO;
        this.aprobadoPor = aprobador;
    }

    /*registra calificación del tutor del 1-10. Nueva calificación cancela la anterior*/
    public void calificarTutor(int calificacion, Usuario evaluador){
        verificarPermisoValidacion(evaluador);
        if (calificacion < 1 || calificacion > 10) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 10.");
        }
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
}