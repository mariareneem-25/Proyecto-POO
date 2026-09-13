import java.util.ArrayList;
import java.util.List;

public class Estudiante extends Usuario {

    private int anualidadCarrera;
    private String carrera;
    private List<Horario> horariosDisponibles;
    private List<Logro> logros;

    public Estudiante(String idUsuario,
                      String nombreCompleto,
                      String correo,
                      String contrasenaHash,
                      int anualidadCarrera,
                      String carrera) {

        super(idUsuario, nombreCompleto, correo, contrasenaHash);

        this.anualidadCarrera = anualidadCarrera;
        this.carrera = carrera;
        this.horariosDisponibles = new ArrayList<>();
        this.logros = new ArrayList<>();
    }

    public int getAnualidadCarrera() {
        return anualidadCarrera;
    }

    public String getCarrera() {
        return carrera;
    }

    public List<Horario> getHorariosDisponibles() {
        return horariosDisponibles;
    }

    public List<Logro> getLogros() {
        return logros;
    }

    public void agregarHorario(Horario horario) {
        horariosDisponibles.add(horario);
    }

    public void agregarLogro(Logro logro) {
        logros.add(logro);
    }

    public boolean tieneLogros() {
        return !logros.isEmpty();
    }

    @Override
    public boolean puedeSerAnonimo() {
        return true;
    }
}
