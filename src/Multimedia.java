import java.util.UUID;

// representa un archivo adjunto (imagen o video) de una publicación
// usar un solo tipo con un enum interno permite tener una lista mixta de imágenes y videos en Publicacion
// sin necesitar dos listas separadas.

public class Multimedia {

    private final String id;
    private final TipoMultimedia tipo;
    private final String rutaArchivo;

    // constructor usado al crear un archivo multimedia nuevo
    // como cuando el usuario sube una imagen o video
    
    public Multimedia(TipoMultimedia tipo, String rutaArchivo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de archivo multimedia es obligatorio");
        }
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo es obligatoria");
        }
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.rutaArchivo = rutaArchivo;
    }

    // constructor usado al reconstruir un objeto Multimedia con datos que ya existían en la base de datos 
    // por eso recibe el id, en vez de generarlo de nuevo
    
    public Multimedia(String id, TipoMultimedia tipo, String rutaArchivo) {
        this.id = id;
        this.tipo = tipo;
        this.rutaArchivo = rutaArchivo;
    }

    public String getId() {
        return id;
    }

    public TipoMultimedia getTipo() {
        return tipo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }
}