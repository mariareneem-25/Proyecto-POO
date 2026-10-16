public class SolicitudTutoria {

    public enum EstadoSolicitudTutoria {
        PENDIENTE,
        ACEPTADA,
        RECHAZADA
    }

    private Estudiante estudiante;
    private Tutor tutor;
    private String materia;
    private String necesidades;
    private EstadoSolicitudTutoria estado;

    public SolicitudTutoria(Estudiante estudiante,
                            Tutor tutor,
                            String materia,
                            String necesidades) {

        if (estudiante == null) {
            throw new IllegalArgumentException(
                    "El estudiante es obligatorio.");
        }

        if (tutor == null) {
            throw new IllegalArgumentException(
                    "El tutor es obligatorio.");
        }

        if (materia == null || materia.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La materia es obligatoria.");
        }

        if (necesidades == null || necesidades.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe indicar las necesidades del estudiante.");
        }

        this.estudiante = estudiante;
        this.tutor = tutor;
        this.materia = materia;
        this.necesidades = necesidades;
        this.estado = EstadoSolicitudTutoria.PENDIENTE;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public Tutor getTutor() {
        return tutor;
    }

    public String getMateria() {
        return materia;
    }

    public String getNecesidades() {
        return necesidades;
    }

    public EstadoSolicitudTutoria getEstado() {
        return estado;
    }

    public boolean estaPendiente() {
        return estado == EstadoSolicitudTutoria.PENDIENTE;
    }

    public void aceptar() {
        if (!estaPendiente()) {
            throw new IllegalStateException(
                    "La solicitud ya fue respondida.");
        }

        estado = EstadoSolicitudTutoria.ACEPTADA;
    }

    public void rechazar() {
        if (!estaPendiente()) {
            throw new IllegalStateException(
                    "La solicitud ya fue respondida.");
        }

        estado = EstadoSolicitudTutoria.RECHAZADA;
    }

    @Override
    public String toString() {
        return "Estudiante: " + estudiante.getNombreCompleto()
                + " | Tutor: " + tutor.getNombreCompleto()
                + " | Materia: " + materia
                + " | Necesidades: " + necesidades
                + " | Estado: " + estado;
    }
}
