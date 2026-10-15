import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// conversación privada entre un estudiante y un tutor
public class Conversacion {

    private final String id;
    private final Estudiante estudiante;
    private final Tutor tutor;
    private final List<Mensaje> mensajes;
    private final LocalDateTime fechaCreacion;

    public Conversacion(Estudiante estudiante, Tutor tutor) {
        if (estudiante == null || tutor == null) {
            throw new IllegalArgumentException("La conversación necesita un estudiante y un tutor");
        }
        this.id = UUID.randomUUID().toString();
        this.estudiante = estudiante;
        this.tutor = tutor;
        this.mensajes = new ArrayList<>();
        this.fechaCreacion = LocalDateTime.now();
    }

    // reconstruir una conversación que ya existía en la base de datos
    public Conversacion(String id, Estudiante estudiante, Tutor tutor, LocalDateTime fechaCreacion) {
        this.id = id;
        this.estudiante = estudiante;
        this.tutor = tutor;
        this.mensajes = new ArrayList<>();
        this.fechaCreacion = fechaCreacion;
    }

    private static boolean mismoUsuario(Usuario a, Usuario b) {
        return Objects.equals(a.getIdUsuario(), b.getIdUsuario());
    }

    public boolean esParticipante(Usuario usuario) {
        return usuario != null && (mismoUsuario(estudiante, usuario) || mismoUsuario(tutor, usuario));
    }

    public Usuario participantePorId(String idUsuario) {
        if (Objects.equals(estudiante.getIdUsuario(), idUsuario)) {
            return estudiante;
        }
        if (Objects.equals(tutor.getIdUsuario(), idUsuario)) {
            return tutor;
        }
        return null;
    }

    public Usuario getOtroParticipante(Usuario usuario) {
        return mismoUsuario(estudiante, usuario) ? tutor : estudiante;
    }

    public void agregarMensaje(Mensaje mensaje) {
        if (mensaje == null || !esParticipante(mensaje.getRemitente())) {
            throw new IllegalArgumentException("Solo los participantes pueden escribir en esta conversación");
        }
        mensajes.add(mensaje);
    }

    public String getId() {
        return id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public Tutor getTutor() {
        return tutor;
    }

    public List<Mensaje> getMensajes() {
        return Collections.unmodifiableList(mensajes);
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}