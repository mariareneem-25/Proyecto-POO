import java.time.DayOfWeek;
import java.time.LocalTime;

public class Horario {

    private DayOfWeek dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    public Horario(DayOfWeek dia, LocalTime horaInicio, LocalTime horaFin) {

        if (horaInicio.isAfter(horaFin)) {
            throw new IllegalArgumentException(
                "La hora de inicio no puede ser posterior a la hora de fin"
            );
        }

        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public DayOfWeek getDia() {
        return dia;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public boolean seTraslapaCon(Horario otro) {

        if (otro == null || !this.dia.equals(otro.dia)) {
            return false;
        }

        return this.horaInicio.isBefore(otro.horaFin)
                && otro.horaInicio.isBefore(this.horaFin);
    }

    @Override
    public String toString() {
        return dia + ": " + horaInicio + " - " + horaFin;
    }
}
