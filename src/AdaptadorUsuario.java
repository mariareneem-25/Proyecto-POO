public class AdaptadorUsuario {

    public static Usuario convertir(Perfil_usuario perfil) {
        if (perfil == null) {
            return null;
        }

        String idComoTexto = String.valueOf(perfil.getIdUsuario());

        switch (perfil.getTipoUsuario()) {
            case ESTUDIANTE:
                return new Estudiante(
                        idComoTexto,
                        perfil.getNombre(),
                        perfil.getCorreoElectronico(),
                        "",
                        0,
                        ""
                );

            case TUTOR: {
                Tutor tutor = new Tutor(
                        idComoTexto,
                        perfil.getNombre(),
                        perfil.getCorreoElectronico(),
                        ""
                );

                // se aplica el estado guardado en la base de datos (el Tutor nace PENDIENTE)
                EstadoSolicitud estado = Perfil_usuario.obtenerEstadoSolicitudTutor(perfil.getIdUsuario());
                if (estado == EstadoSolicitud.APROBADO) {
                    tutor.aprobarSolicitud(null);
                } else if (estado == EstadoSolicitud.RECHAZADO) {
                    tutor.rechazarSolicitud(null);
                }
                return tutor;
            }

            default:
                // Docente, Director u otros tipos: todavía no soportado en el foro ni en el chat
                return null;
        }
    }
}
