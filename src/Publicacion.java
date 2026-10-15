import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


// representa una pregunta publicada en el foro académico

public class Publicacion implements Publicable {

    private static final int LIMITE_ADJUNTOS = 5;

    private final String id;
    private final Usuario autorReal;
    private final boolean anonimo;
    private final String titulo;
    private final String cuerpo;
    private final List<Multimedia> archivosAdjuntos;
    private final List<Comentario> respuestas;
    private final LocalDateTime fecha;

    // constructor usado al crear una publicación nueva desde la aplicación
    // valida que si se pidió anonimato, el autor tenga permiso real para usarlo 
    // ya que solo estudiantes pueden suarlo según puedeSerAnonimo()).
    
    public Publicacion(Usuario autorReal, String titulo, String cuerpo, boolean anonimo) {
        if (autorReal == null) {
            throw new IllegalArgumentException("La publicación necesita un autor");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException("El cuerpo es obligatorio");
        }
        if (anonimo && !autorReal.puedeSerAnonimo()) {
            throw new IllegalStateException(
                    "Este tipo de usuario no tiene permitido publicar de forma anónima");
        }

        this.id = UUID.randomUUID().toString();
        this.autorReal = autorReal;
        this.titulo = titulo;
        this.cuerpo = cuerpo;
        this.anonimo = anonimo;
        this.archivosAdjuntos = new ArrayList<>();
        this.respuestas = new ArrayList<>();
        this.fecha = LocalDateTime.now();
    }

    // constructor usado al reconstruir una publicación con datos que ya existían en la base de datos 
    // id y fecha originales
    // sin volver a validar reglas de creación
    
    public Publicacion(String id, Usuario autorReal, String titulo, String cuerpo,
                        boolean anonimo, LocalDateTime fecha) {
        this.id = id;
        this.autorReal = autorReal;
        this.titulo = titulo;
        this.cuerpo = cuerpo;
        this.anonimo = anonimo;
        this.fecha = fecha;
        this.archivosAdjuntos = new ArrayList<>();
        this.respuestas = new ArrayList<>();
    }

    // encapsula la regla del límite de 5 adjuntos entre imágenes y videos

    public void agregarMultimedia(Multimedia m) {
        if (cantidadAdjuntos() >= LIMITE_ADJUNTOS) {
            throw new IllegalStateException(
                    "No se pueden adjuntar más de " + LIMITE_ADJUNTOS + " archivos por publicación");
        }
        archivosAdjuntos.add(m);
    }

    public void agregarRespuesta(Comentario c) {
        respuestas.add(c);
    }

    public int cantidadAdjuntos() {
        return archivosAdjuntos.size();
    }

    public String getId() {
        return id;
    }

    public Usuario getAutorReal() {
        return autorReal;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<Multimedia> getArchivosAdjuntos() {
        return archivosAdjuntos;
    }

    public List<Comentario> getRespuestas() {
        return respuestas;
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