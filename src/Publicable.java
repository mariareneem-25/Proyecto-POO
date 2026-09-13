import java.time.LocalDateTime;

// define el comportamiento compartido entre Publicacion y Comentario
// ya que ambas manejan autor, contenido y anonimato de la misma manera
// permite tratar publicaciones y comentarios de forma genérica sin que el resto del programa necesite saber cuál de las dos es

public interface Publicable {

    String getContenido();

    boolean esAnonimo();

    // si esAnonimo() es verdadero, debe devolver "Anónimo"
    // si es falso, debe devolver el nombre real del autor
    // ninguna pantalla debe leer el autor directamente
    // siempre a través de este método

    String getAutorMostrado();

    // si esAnonimo() es verdadero, debe devolver la ruta de una foto de perfil por defecto
    // si es falso, debe devolver la foto real
    
    String getFotoMostrada();

    LocalDateTime getFecha();
}
