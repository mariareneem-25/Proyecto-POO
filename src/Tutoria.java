public class Tutoria {

    private int idTutoria;
    private Estudiante estudiante;
    private Tutor tutor;
    private String materia;
    private Horario horario;
    private boolean activa;

    public Tutoria(int idTutoria,
                   Estudiante estudiante,
                   Tutor tutor,
                   String materia,
                   Horario horario) {

        if (estudiante == null) {
            throw new IllegalArgumentException(
                "El estudiante no puede ser nulo."
            );
        }

        if (tutor == null) {
            throw new IllegalArgumentException(
                "El tutor no puede ser nulo."
            );
        }

        if (materia == null || materia.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "La materia no puede estar vacía."
            );
        }

        if (horario == null) {
            throw new IllegalArgumentException(
                "El horario no puede ser nulo."
            );
        }

        this.idTutoria = idTutoria;
        this.estudiante = estudiante;
        this.tutor = tutor;
        this.materia = materia;
        this.horario = horario;
        this.activa = true;
    }

    public int getIdTutoria() {
        return idTutoria;
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

    public Horario getHorario() {
        return horario;
    }

    public boolean estaActiva() {
        return activa;
    }

    public void cancelar() {
        this.activa = false;
    }

    @Override
    public String toString() {
        return "Tutoria #" + idTutoria
                + " | Materia: " + materia
                + " | Estudiante: " + estudiante.getNombreCompleto()
                + " | Tutor: " + tutor.getNombreCompleto()
                + " | Horario: " + horario
                + " | Estado: " + (activa ? "Activa" : "Cancelada");
    }
}
