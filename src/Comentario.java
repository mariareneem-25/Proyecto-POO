import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


// representa una respuesta a una publicacion o a un comentario debajo de la publicacion
// como es el mismo tipo, se pueden hacer hilos de comentarios para cumplir el proposito de conversacion en el foro 

public class Comentario implements Publicable {

    private final String id;
    private final Usuario autorReal;
    private final boolean anonimo;
    private final String cuerpo;
    private final List<Comentario> respuestasAlComentario;
    private final LocalDateTime fecha;

    // el constructor se usa al crear un comentario nuevo
    // valida a la hora de que se pide publicar de manera anonima de que el autor tenga permiso para usarlo
    // solo estudiantes pueden usarlo, según puedeSerAnonimo().
    
    public Comentario(Usuario autorReal, String cuerpo, boolean anonimo) {
        if (autorReal == null) {
            throw new IllegalArgumentException("El comentario necesita un autor");
        }
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException("El cuerpo del comentario no puede estar vacío");
        }
        if (anonimo && !autorReal.puedeSerAnonimo()) {
            throw new IllegalStateException(
                    "Este tipo de usuario no tiene permitido comentar de forma anónima");
        }

        this.id = UUID.randomUUID().toString();
        this.autorReal = autorReal;
        this.cuerpo = cuerpo;
        this.anonimo = anonimo;
        this.respuestasAlComentario = new ArrayList<>();
        this.fecha = LocalDateTime.now();
    }

    // el constructor se usa al reconstruir un comentario con datos que ya existían en la base de datos 
    // id y fecha originales
    // sin volver a validar reglas de creación

    public Comentario(String id, Usuario autorReal, String cuerpo, boolean anonimo, LocalDateTime fecha) {
        this.id = id;
        this.autorReal = autorReal;
        this.cuerpo = cuerpo;
        this.anonimo = anonimo;
        this.fecha = fecha;
        this.respuestasAlComentario = new ArrayList<>();
    }

    // agrega una respuesta anidada a este comentario
    // permite contestar respuestas y no solo posts

    public void agregarRespuesta(Comentario c) {
        respuestasAlComentario.add(c);
    }

    public String getId() {
        return id;
    }

    public Usuario getAutorReal() {
        return autorReal;
    }

    public List<Comentario> getRespuestasAlComentario() {
        return respuestasAlComentario;
    }

    @Override
    public String getContenido() {
        return cuerpo;
    }

    @Override
    public boolean esAnonimo() {
        return anonimo;
    }

    @Override
    public String getAutorMostrado() {
        return anonimo ? "Anónimo" : autorReal.getNombreCompleto();
    }

    @Override
    public String getFotoMostrada() {
        return anonimo ? "default_anonimo.png" : autorReal.getFotoPerfilPath();
    }

    @Override
    public LocalDateTime getFecha() {
        return fecha;
    }
}
