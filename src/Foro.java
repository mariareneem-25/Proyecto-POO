import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// representa el foro académico
// mantiene las publicaciones en memoria para que la aplicación las recorra rápido
// cada acción que cambie datos como publicar y responder, se guarda en SQLite
// para que la información no se pierda entre sesiones.

public class Foro {

    private final List<Publicacion> publicaciones;
    private final ForoDAO foroDAO;

    public Foro() {
        this.publicaciones = new ArrayList<>();
        this.foroDAO = new ForoDAO();
    }

    
    // agrega una nueva publicación al foro y la guarda en la base de datos

    public void publicar(Publicacion p) {
        publicaciones.add(p);
        foroDAO.guardarPublicacion(p);
    }

    
    // responde a una publicación (no a otro comentario)
    
    public void responder(Publicacion publicacion, Comentario comentario) {
        publicacion.agregarRespuesta(comentario);
        foroDAO.guardarComentario(comentario, publicacion.getId(), null);
    }

    // responde a un comentario ya existente, creando un hilo de respuestas
    // se necesita el id de la publicación raíz para poder guardarlo correctamente en la base de datos

    public void responderComentario(String publicacionId, Comentario comentarioPadre, Comentario respuesta) {
        comentarioPadre.agregarRespuesta(respuesta);
        foroDAO.guardarComentario(respuesta, publicacionId, comentarioPadre.getId());
    }

    // devuelve las publicaciones ordenadas de la más reciente a la más antigua
    // esto es para que funcione la interfaz de deslizar hacia abajo
    // que en realidad solo esta recorriendo la lista 
    
    public List<Publicacion> obtenerFeed() {
        List<Publicacion> ordenado = new ArrayList<>(publicaciones);
        ordenado.sort(Comparator.comparing(Publicacion::getFecha).reversed());
        return ordenado;
    }

    public Optional<Publicacion> buscarPublicacionPorId(String id) {
        return publicaciones.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    // Recupera todas las publicaciones ya guardadas en SQLite 
    
    public void cargarDesdeBaseDeDatos(UsuarioRepositorio usuarioRepositorio) {
        publicaciones.clear();
        publicaciones.addAll(foroDAO.cargarPublicaciones(usuarioRepositorio));
    }
}