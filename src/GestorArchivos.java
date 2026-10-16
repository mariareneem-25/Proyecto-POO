import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// copia los archivos que suben los usuarios a una carpeta propia de la aplicación,
// separados por categoría. Se usa en el chat y queda listo para perfiles, foro y banner del tutor.
public class GestorArchivos {

    public enum Categoria {
        CHAT("chat"),
        PERFIL("perfiles"),
        FORO("foro"),
        BANNER_TUTOR("banners");

        private final String carpeta;

        Categoria(String carpeta) {
            this.carpeta = carpeta;
        }

        public String getCarpeta() {
            return carpeta;
        }
    }

    private static final String CARPETA_BASE = "archivos_subidos";
    private static final long TAMANO_MAXIMO_BYTES = 25L * 1024 * 1024; // 25 MB

    // copia cualquier archivo a archivos_subidos/<categoria>/ y devuelve la nueva ruta
    public static String guardar(String rutaOrigen, Categoria categoria) {
        if (rutaOrigen == null || rutaOrigen.isBlank()) {
            throw new IllegalArgumentException("Debes indicar la ruta del archivo");
        }
        if (categoria == null) {
            throw new IllegalArgumentException("Debes indicar la categoría del archivo");
        }

        Path origen = Paths.get(limpiarRuta(rutaOrigen));

        if (!Files.isRegularFile(origen)) {
            throw new IllegalArgumentException("No se encontró el archivo: " + origen);
        }

        try {
            if (Files.size(origen) > TAMANO_MAXIMO_BYTES) {
                throw new IllegalArgumentException("El archivo supera el tamaño máximo de 25 MB");
            }

            Path carpeta = Paths.get(CARPETA_BASE, categoria.getCarpeta());
            Files.createDirectories(carpeta);
            Path destino = carpeta.resolve(UUID.randomUUID() + "_" + origen.getFileName());
            Files.copy(origen, destino);
            return destino.toString();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo: " + e.getMessage(), e);
        }
    }

    // igual que guardar, pero solo acepta imágenes (para foto de perfil, banner del tutor, etc.)
    public static String guardarImagen(String rutaOrigen, Categoria categoria) {
        if (rutaOrigen == null || !esImagen(rutaOrigen)) {
            throw new IllegalArgumentException(
                    "El archivo debe ser una imagen (.png, .jpg, .jpeg, .gif, .webp, .bmp)");
        }
        return guardar(rutaOrigen, categoria);
    }

    public static boolean esImagen(String ruta) {
        String r = limpiarRuta(ruta).toLowerCase();
        return r.endsWith(".png") || r.endsWith(".jpg") || r.endsWith(".jpeg")
                || r.endsWith(".gif") || r.endsWith(".webp") || r.endsWith(".bmp");
    }

    public static boolean esVideo(String ruta) {
        String r = limpiarRuta(ruta).toLowerCase();
        return r.endsWith(".mp4") || r.endsWith(".mov") || r.endsWith(".avi")
                || r.endsWith(".mkv") || r.endsWith(".webm");
    }

    // para el foro: devuelve IMAGEN, VIDEO o null si el archivo no es ninguno de los dos
    public static TipoMultimedia tipoMultimediaDe(String ruta) {
        if (esImagen(ruta)) {
            return TipoMultimedia.IMAGEN;
        }
        if (esVideo(ruta)) {
            return TipoMultimedia.VIDEO;
        }
        return null;
    }

    // devuelve el nombre del archivo sin el identificador que se le agrega al guardarlo
    public static String nombreOriginal(String rutaGuardada) {
        String nombre = Paths.get(rutaGuardada).getFileName().toString();
        if (nombre.length() > 37 && nombre.charAt(36) == '_') {
            return nombre.substring(37);
        }
        return nombre;
    }

    // quita espacios y las comillas que Windows agrega al usar "Copiar como ruta de acceso"
    private static String limpiarRuta(String ruta) {
        return ruta.trim().replace("\"", "");
    }

    // ---------------------------------------------------------------
    // CÓMO USARLO MÁS ADELANTE:
    //
    // Foto de perfil:
    //   String foto = GestorArchivos.guardarImagen(ruta, GestorArchivos.Categoria.PERFIL);
    //   sesion.actualizarPerfil(nombre, contrasena, foto);
    //
    // Foro (imágenes y videos en una publicación):
    //   TipoMultimedia tipo = GestorArchivos.tipoMultimediaDe(ruta);
    //   String guardada = GestorArchivos.guardar(ruta, GestorArchivos.Categoria.FORO);
    //   publicacion.agregarMultimedia(new Multimedia(tipo, guardada));
    //
    // Banner del tutor (falta agregar el atributo "banner" en Tutor):
    //   String banner = GestorArchivos.guardarImagen(ruta, GestorArchivos.Categoria.BANNER_TUTOR);
    // ---------------------------------------------------------------
}