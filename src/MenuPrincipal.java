import java.util.ArrayList;
import java.util.List;

// decide qué opciones del menú principal ve cada tipo de cuenta
// Main solo pide la lista y ejecuta lo que se elija
// docentes y directores no ven Chat ni Calendario
// para estudiantes y tutores el chat de tutoría queda dentro de "Chat"

public class MenuPrincipal {

    public static final String ACTUALIZAR_PERFIL = "Actualizar perfil";
    public static final String ELIMINAR_PERFIL = "Eliminar perfil";
    public static final String FORO = "Foro académico";
    public static final String CHAT = "Chat";
    public static final String SOLICITUDES = "Solicitudes de tutoría";
    public static final String CALENDARIO = "Calendario";
    public static final String CERRAR_SESION = "Cerrar sesión";

    private MenuPrincipal() {
    }

    public static List<String> opcionesPara(Perfil_usuario sesion, Controlaracceso acceso) {
        // solo docentes y directores ven "Solicitudes de tutoría" (ahí validan a los tutores)
        boolean esDocenteODirector = sesion.getTipoUsuario() == Perfil_usuario.TipoUsuario.DOCENTE
                || sesion.getTipoUsuario() == Perfil_usuario.TipoUsuario.DIRECTOR;

        List<String> opciones = new ArrayList<>();
        opciones.add(ACTUALIZAR_PERFIL);
        opciones.add(ELIMINAR_PERFIL);
        opciones.add(FORO);

        // solo estudiantes y tutores usan chat y calendario
        if (acceso.puedeUsarChat(sesion)) {
            opciones.add(CHAT);
        }

        if (esDocenteODirector) {
            opciones.add(SOLICITUDES);
        }

        if (acceso.puedeUsarCalendario(sesion)) {
            opciones.add(CALENDARIO);
        }

        opciones.add(CERRAR_SESION);
        return opciones;
    }
}