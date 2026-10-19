public class Controlaracceso {

    public boolean sesionValida(Perfil_usuario usuario) {
        return usuario != null
                && usuario.isSesionActiva()
                && usuario.getEstadoCuenta() == Perfil_usuario.EstadoCuenta.ACTIVA;
    }

    public boolean esEstudiante(Perfil_usuario usuario) {
        return sesionValida(usuario)
                && usuario.getTipoUsuario() == Perfil_usuario.TipoUsuario.ESTUDIANTE;
    }

    public boolean esTutor(Perfil_usuario usuario) {
        return sesionValida(usuario)
                && usuario.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR;
    }

    public boolean puedeEnviarSolicitud(Perfil_usuario usuario) {
        return esEstudiante(usuario);
    }

    public boolean puedeGestionarSolicitudes(Perfil_usuario usuario) {
        return esTutor(usuario);
    }

    public boolean puedeGestionarLogros(Perfil_usuario usuario) {
        return esEstudiante(usuario);
    }

    public boolean puedeUsarChat(Perfil_usuario usuario) {
        return esEstudiante(usuario) || esTutor(usuario);
    }

    public boolean puedeUsarCalendario(Perfil_usuario usuario) {
        return esEstudiante(usuario) || esTutor(usuario);
    }

    public boolean puedeGestionarSolicitud(Perfil_usuario usuario, int idSolicitud) {
        if (!esTutor(usuario)) {
            return false;
        }

        return ConexionDB.existe(
                "SELECT 1 FROM solicitudes WHERE idSolicitud = ? AND idTutor = ?",
                idSolicitud, usuario.getIdUsuario());
    }

    public boolean puedeAccederChat(Perfil_usuario usuario, int idSolicitud) {
        if (!puedeUsarChat(usuario)) {
            return false;
        }

        return ConexionDB.existe(
                "SELECT 1 FROM solicitudes WHERE idSolicitud = ? AND estado = 'APROBADO' "
                        + "AND (idEstudiante = ? OR idTutor = ?)",
                idSolicitud, usuario.getIdUsuario(), usuario.getIdUsuario());
    }

    public boolean puedeVerInformacionDe(Perfil_usuario usuario, int idOtroUsuario) {
        if (!sesionValida(usuario)) {
            return false;
        }

        if (usuario.getIdUsuario() == idOtroUsuario) {
            return true;
        }

        if (!esTutor(usuario)) {
            return false;
        }

        return ConexionDB.existe(
                "SELECT 1 FROM solicitudes WHERE idTutor = ? AND idEstudiante = ? "
                        + "AND estado IN ('PENDIENTE', 'APROBADO')",
                usuario.getIdUsuario(), idOtroUsuario);
    }
}