// implementación de UsuarioRepositorio que lee los usuarios de Perfil_usuario (proyecto.db)
// y los convierte a Estudiante/Tutor con AdaptadorUsuario
public class UsuarioRepositorioPerfil implements UsuarioRepositorio {

    @Override
    public Usuario obtenerPorId(String idUsuario) {
        try {
            Perfil_usuario perfil = Perfil_usuario.buscarPorId(Integer.parseInt(idUsuario));
            return AdaptadorUsuario.convertir(perfil);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
