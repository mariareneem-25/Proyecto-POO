// punto de extensión para elegir las imágenes de la solicitud de tutor (foto de perfil y banner)
// por ahora solo se escribe la ruta o el nombre (SelectorImagenConsola)
// más adelante se puede crear otra implementación que suba el archivo de verdad,
// copiándolo con GestorArchivos.guardar(ruta, categoria), sin tocar SolicitudTutorConsola

public interface SelectorImagen {

    // devuelve la ruta o nombre elegido; si la persona no escribe nada, devuelve el valor actual
    String elegir(GestorArchivos.Categoria categoria, String etiqueta, String valorActual);
}
