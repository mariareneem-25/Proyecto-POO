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

            case TUTOR:
                Tutor tutor = new Tutor(
                        idComoTexto,
                        perfil.getNombre(),
                        perfil.getCorreoElectronico(),
                        ""
                );

                tutor.cargarDatosTutor();
                return tutor;

            default:
                return null;
        }
    }
}
