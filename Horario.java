import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Representa un bloque de disponibilidad de tiempo.
 * La usan Estudiante y Tutor.
 */
public class Horario {

    private DayOfWeek dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    /**
    Recibe el día, hora de inicio y fin, los asigna al
     * crear el horario./** */
    public Horario(DayOfWeek dia, LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio.isAfter(horaFin)) {
            throw new IllegalArgumentException(
                    "La hora de inicio no puede ser posterior a la hora de fin.");
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

    /**
     * Verifica si este horario se cruza en tiempo con otro horario del
     * mismo día. /** */
    public boolean seTraslapaCon(Horario otro) {
        if (this.dia != otro.dia) {
            return false;
        }
        return this.horaInicio.isBefore(otro.horaFin) && otro.horaInicio.isBefore(this.horaFin);
    }

    /** Representación legible del horario (día + rango de horas). */
    @Override
    public String toString() {
        return dia + " " + horaInicio + " - " + horaFin;
    }
}