import java.util.ArrayList;
import java.util.List;

public class AgendaTutorias {

    private List<Tutoria> tutorias;
    private int siguienteId;

    public AgendaTutorias() {
        this.tutorias = new ArrayList<>();
        this.siguienteId = 1;
    }

    public Tutoria agendarTutoria(Estudiante estudiante,
                                  Tutor tutor,
                                  String materia,
                                  Horario horario) {

        if (estudiante == null || tutor == null || horario == null) {
            throw new IllegalArgumentException(
                    "El estudiante, tutor y horario son obligatorios.");
        }

        if (materia == null || materia.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La materia es obligatoria.");
        }

        if (!tutor.estaActivo()) {
            throw new IllegalStateException(
                    "El tutor debe estar aprobado para agendar tutorías.");
        }

        if (!tutorImparteMateria(tutor, materia)) {
            throw new IllegalArgumentException(
                    "El tutor no imparte la materia seleccionada.");
        }

        if (!estudianteDisponible(estudiante, horario)) {
            throw new IllegalArgumentException(
                    "El estudiante no está disponible en ese horario.");
        }

        if (!tutorDisponible(tutor, horario)) {
            throw new IllegalArgumentException(
                    "El tutor no está disponible en ese horario.");
        }

        if (horarioOcupado(tutor, horario)) {
            throw new IllegalStateException(
                    "El tutor ya tiene una tutoría activa en ese horario.");
        }

        Tutoria nuevaTutoria = new Tutoria(
                siguienteId,
                estudiante,
                tutor,
                materia,
                horario
        );

        tutorias.add(nuevaTutoria);
        siguienteId++;

        return nuevaTutoria;
    }

    private boolean tutorImparteMateria(Tutor tutor, String materia) {
        for (String materiaTutor : tutor.getMateriasQueDicta()) {
            if (materiaTutor.equalsIgnoreCase(materia)) {
                return true;
            }
        }

        return false;
    }

    private boolean estudianteDisponible(Estudiante estudiante,
                                          Horario horario) {

        for (Horario disponible : estudiante.getHorariosDisponibles()) {
            if (disponible.seTraslapaCon(horario)) {
                return true;
            }
        }

        return false;
    }

    private boolean tutorDisponible(Tutor tutor,
                                     Horario horario) {

        for (Horario disponible : tutor.getHorarioDisponibles()) {
            if (disponible.seTraslapaCon(horario)) {
                return true;
            }
        }

        return false;
    }

    private boolean horarioOcupado(Tutor tutor,
                                    Horario horario) {

        for (Tutoria tutoria : tutorias) {

            if (tutoria.estaActiva()
                    && tutoria.getTutor() == tutor
                    && tutoria.getHorario().seTraslapaCon(horario)) {

                return true;
            }
        }

        return false;
    }

    public List<Tutoria> obtenerTutoriasEstudiante(Estudiante estudiante) {

        List<Tutoria> resultado = new ArrayList<>();

        for (Tutoria tutoria : tutorias) {
            if (tutoria.getEstudiante() == estudiante) {
                resultado.add(tutoria);
            }
        }

        return resultado;
    }

    public List<Tutoria> obtenerTutoriasTutor(Tutor tutor) {

        List<Tutoria> resultado = new ArrayList<>();

        for (Tutoria tutoria : tutorias) {
            if (tutoria.getTutor() == tutor) {
                resultado.add(tutoria);
            }
        }

        return resultado;
    }

    public boolean cancelarTutoria(int idTutoria) {

        for (Tutoria tutoria : tutorias) {

            if (tutoria.getIdTutoria() == idTutoria
                    && tutoria.estaActiva()) {

                tutoria.cancelar();
                return true;
            }
        }

        return false;
    }

    public List<Tutoria> getTutorias() {
        return tutorias;
    }
}
