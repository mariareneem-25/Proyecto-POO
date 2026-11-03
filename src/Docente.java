// usuario docente: puede ver el foro académico y comentar las publicaciones, pero no publicar
// los permisos salen de Usuario (puedePublicar() = false, puedeComentar() = true)

public class Docente extends Usuario {

    public Docente(String idUsuario, String nombreCompleto, String correo, String contrasenaHash) {
        super(idUsuario, nombreCompleto, correo, contrasenaHash);
    }
}