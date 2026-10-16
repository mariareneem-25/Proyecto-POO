import java.time.LocalDateTime;
import java.util.UUID;

public class Mensaje {

    private final String id;
    private final Usuario remitente;
    private final TipoMensaje tipo;
    private final String contenido;
    private final String rutaArchivo;
    private final LocalDateTime fecha;

    // mensaje de texto nuevo
    public Mensaje(Usuario remitente, String contenido) {
        if (remitente == null) {
            throw new IllegalArgumentException("El mensaje necesita un remitente");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        this.id = UUID.randomUUID().toString();
        this.remitente = remitente;
        this.tipo = TipoMensaje.TEXTO;
        this.contenido = contenido;
        this.rutaArchivo = null;
        this.fecha = LocalDateTime.now();
    }

    // mensaje nuevo con archivo adjunto (imagen, video u otro archivo)
    // descripcion es opcional (puede ir vacía)
    public Mensaje(Usuario remitente, TipoMensaje tipo, String descripcion, String rutaArchivo) {
        if (remitente == null) {
            throw new IllegalArgumentException("El mensaje necesita un remitente");
        }
        if (tipo == null || tipo == TipoMensaje.TEXTO) {
            throw new IllegalArgumentException("El tipo debe ser IMAGEN, VIDEO o ARCHIVO");
        }
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }

        this.id = UUID.randomUUID().toString();
        this.remitente = remitente;
        this.tipo = tipo;
        this.contenido = (descripcion == null) ? "" : descripcion;
        this.rutaArchivo = rutaArchivo;
        this.fecha = LocalDateTime.now();
    }

    // reconstruir un mensaje que ya existía en la base de datos
    public Mensaje(String id, Usuario remitente, TipoMensaje tipo, String contenido,
                   String rutaArchivo, LocalDateTime fecha) {
        this.id = id;
        this.remitente = remitente;
        this.tipo = tipo;
        this.contenido = (contenido == null) ? "" : contenido;
        this.rutaArchivo = rutaArchivo;
        this.fecha = fecha;
    }

    public String getId() {
        return id;
    }

    public Usuario getRemitente() {
        return remitente;
    }

    public TipoMensaje getTipo() {
        return tipo;
    }

    public String getContenido() {
        return contenido;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public boolean tieneArchivo() {
        return rutaArchivo != null && !rutaArchivo.isBlank();
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}