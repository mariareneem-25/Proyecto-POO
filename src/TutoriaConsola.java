import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// pantallas de las solicitudes de tutoría entre estudiantes y tutores
//
// estudiante: buscar tutores (ver su perfil completo y su calendario) y solicitar tutoría,
//             ver el estado de sus solicitudes y manejar sus logros
// tutor: ver las solicitudes de estudiantes (con su perfil, logros y calendario), aceptarlas o rechazarlas
//        al aceptar se abre el chat entre los dos; al rechazar solo le llega el mensaje al estudiante

public class TutoriaConsola {

    private final Scanner sc;
    private final ServicioChat servicioChat;
    private final Controlaracceso acceso = new Controlaracceso();
    private final Gestionarsolicitudes solicitudes = new Gestionarsolicitudes();
    private final SolicitudTutorDAO solicitudTutorDAO = new SolicitudTutorDAO();
    private final CalendarioDAO calendario = new CalendarioDAO();
    private final RepositorioLogros logros = new RepositorioLogros();

    public TutoriaConsola(Scanner sc, ServicioChat servicioChat) {
        this.sc = sc;
        this.servicioChat = servicioChat;
    }

    // ---------------------------------------------------------------
    // Estudiante: buscar tutores
    // ---------------------------------------------------------------

    public void buscarTutores(Perfil_usuario estudiante) {
        if (!acceso.puedeEnviarSolicitud(estudiante)) {
            System.out.println("Solo los estudiantes pueden buscar tutores.");
            return;
        }

        List<Perfil_usuario> todos = tutoresAprobados();
        if (todos.isEmpty()) {
            System.out.println("Todavía no hay tutores disponibles.");
            return;
        }

        List<Perfil_usuario> mostrados = todos;

        while (true) {
            System.out.println("\n--- Buscar tutores ---");
            if (mostrados.isEmpty()) {
                System.out.println("No se encontraron tutores con ese criterio.");
            }
            for (int i = 0; i < mostrados.size(); i++) {
                Perfil_usuario p = mostrados.get(i);
                System.out.println((i + 1) + ". " + nombreTutor(p) + " | Materias: " + materiasDe(p));
            }

            System.out.print("Número del tutor, B para buscar por nombre o materia, T para ver todos, 0 para volver: ");
            String entrada = sc.nextLine().trim();

            if (entrada.equals("0")) {
                return;
            }
            if (entrada.equalsIgnoreCase("t")) {
                mostrados = todos;
                continue;
            }
            if (entrada.equalsIgnoreCase("b")) {
                System.out.print("Nombre o materia a buscar: ");
                mostrados = filtrar(todos, sc.nextLine());
                continue;
            }

            try {
                int numero = Integer.parseInt(entrada);
                if (numero >= 1 && numero <= mostrados.size()) {
                    verPerfilTutor(estudiante, mostrados.get(numero - 1));
                }
            } catch (NumberFormatException e) {
                // entrada no válida: se vuelve a mostrar la lista
            }
        }
    }

    private void verPerfilTutor(Perfil_usuario estudiante, Perfil_usuario tutor) {
        SolicitudTutor info = infoTutor(tutor);

        while (true) {
            System.out.println("\n--- Perfil del tutor ---");
            System.out.println("Nombre: " + nombreTutor(tutor));
            System.out.println("Materias que imparte: " + valor(info == null ? null : info.getMaterias()));
            System.out.println("Descripción: " + valor(primero(info == null ? null : info.getDescripcion(), tutor.getDescripcion())));
            System.out.println("Foto de perfil: " + valor(primero(info == null ? null : info.getFotoPerfil(), tutor.getFotoPerfil())));
            System.out.println("Banner: " + valor(primero(info == null ? null : info.getBanner(), tutor.getBanner())));

            System.out.println("\nCalendario del tutor:");
            System.out.print(calendario.resumen(tutor.getIdUsuario(), false));

            System.out.println("\n1. Solicitar tutoría");
            System.out.println("2. Regresar");
            int opcion = leerEntero("Selecciona una opción: ");

            if (opcion == 1) {
                solicitarTutoria(estudiante, tutor, info);
                return;
            }
            if (opcion == 2) {
                return;
            }
        }
    }

    private void solicitarTutoria(Perfil_usuario estudiante, Perfil_usuario tutor, SolicitudTutor info) {
        String materia = pedirMateria(info);
        System.out.print("Mensaje para el tutor: ");
        String mensaje = sc.nextLine();

        try {
            solicitudes.crearSolicitud(estudiante, tutor.getIdUsuario(), materia, mensaje);
            System.out.println("Solicitud enviada a " + nombreTutor(tutor)
                    + ". Cuando la acepte, se abrirá su chat en el menú Chat.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo enviar la solicitud: " + e.getMessage());
        }
    }

    private String pedirMateria(SolicitudTutor info) {
        List<String> materias = listaMaterias(info);

        if (materias.isEmpty()) {
            System.out.print("Materia: ");
            return sc.nextLine();
        }

        for (int i = 0; i < materias.size(); i++) {
            System.out.println((i + 1) + ". " + materias.get(i));
        }
        System.out.print("Materia (número de la lista, o escribe otra): ");
        String entrada = sc.nextLine().trim();

        try {
            int numero = Integer.parseInt(entrada);
            if (numero >= 1 && numero <= materias.size()) {
                return materias.get(numero - 1);
            }
        } catch (NumberFormatException e) {
            // se usa lo que escribió
        }
        return entrada;
    }

    // ---------------------------------------------------------------
    // Estudiante: mis solicitudes y mis logros
    // ---------------------------------------------------------------

    public void misSolicitudesYLogros(Perfil_usuario estudiante) {
        if (!acceso.puedeEnviarSolicitud(estudiante)) {
            System.out.println("Solo los estudiantes pueden ver sus solicitudes.");
            return;
        }

        int opcion = -1;
        while (opcion != 5) {
            System.out.println("\n--- Mis solicitudes y logros ---");
            System.out.println("1. Ver mis solicitudes");
            System.out.println("2. Ver mis logros");
            System.out.println("3. Agregar logro");
            System.out.println("4. Completar logro");
            System.out.println("5. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            try {
                if (opcion == 1) {
                    verMisSolicitudes(estudiante);
                } else if (opcion == 2) {
                    verMisLogros(estudiante);
                } else if (opcion == 3) {
                    System.out.print("Tema del logro: ");
                    logros.agregarLogro(estudiante, sc.nextLine());
                    System.out.println("Logro agregado.");
                } else if (opcion == 4) {
                    System.out.print("Tema del logro a completar: ");
                    logros.completarLogro(estudiante, sc.nextLine());
                    System.out.println("Logro marcado como completado.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("No se pudo completar la acción: " + e.getMessage());
            }
        }
    }

    private void verMisSolicitudes(Perfil_usuario estudiante) {
        List<Solicitud> lista = solicitudes.listarMisSolicitudes(estudiante);
        if (lista.isEmpty()) {
            System.out.println("No has enviado solicitudes.");
            return;
        }

        for (Solicitud s : lista) {
            System.out.println("#" + s.getIdSolicitud() + " | Tutor: " + s.getNombreTutor()
                    + " | Materia: " + s.getMateria() + " | Estado: " + etiqueta(s.getEstado()));
            if (s.getEstado() == EstadoSolicitud.RECHAZADO) {
                System.out.println("   Mensaje del tutor: " + valor(s.getMotivoRechazo()));
            } else if (s.getEstado() == EstadoSolicitud.APROBADO) {
                System.out.println("   Ya pueden hablar en el menú Chat.");
            }
        }
    }

    private void verMisLogros(Perfil_usuario estudiante) {
        List<Logro> lista = logros.obtenerLogros(estudiante, estudiante.getIdUsuario());
        if (lista.isEmpty()) {
            System.out.println("No tienes logros registrados.");
            return;
        }
        for (Logro logro : lista) {
            System.out.println(logro);
        }
    }

    // ---------------------------------------------------------------
    // Tutor: solicitudes de estudiantes
    // ---------------------------------------------------------------

    public void solicitudesDeEstudiantes(Perfil_usuario tutor) {
        if (!acceso.puedeGestionarSolicitudes(tutor)) {
            System.out.println("Solo los tutores pueden ver las solicitudes de estudiantes.");
            return;
        }

        while (true) {
            List<Solicitud> pendientes;
            try {
                pendientes = solicitudes.listarPendientes(tutor);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
                return;
            }

            System.out.println("\n--- Solicitudes de estudiantes ---");
            if (pendientes.isEmpty()) {
                System.out.println("No tienes solicitudes pendientes.");
                return;
            }

            for (int i = 0; i < pendientes.size(); i++) {
                Solicitud s = pendientes.get(i);
                System.out.println((i + 1) + ". " + s.getNombreEstudiante() + " | Materia: " + s.getMateria());
            }

            int eleccion = leerEntero("Selecciona una solicitud (0 para volver): ");
            if (eleccion < 1 || eleccion > pendientes.size()) {
                return;
            }
            revisar(tutor, pendientes.get(eleccion - 1));
        }
    }

    // muestra el perfil del estudiante, sus logros y su calendario, y permite aceptar o rechazar
    private void revisar(Perfil_usuario tutor, Solicitud solicitud) {
        while (true) {
            try {
                System.out.println("\n" + solicitudes.consultarSolicitud(tutor, solicitud.getIdSolicitud()));
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
                return;
            }

            System.out.println("1. Aceptar");
            System.out.println("2. Rechazar");
            System.out.println("3. Regresar");
            int opcion = leerEntero("Selecciona una opción: ");

            try {
                if (opcion == 1) {
                    aceptar(tutor, solicitud);
                    return;
                } else if (opcion == 2) {
                    if (rechazar(tutor, solicitud)) {
                        return;
                    }
                } else if (opcion == 3) {
                    return;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("No se pudo completar la acción: " + e.getMessage());
                return;
            }
        }
    }

    private void aceptar(Perfil_usuario tutor, Solicitud solicitud) {
        solicitudes.aceptarSolicitud(tutor, solicitud.getIdSolicitud());

        try {
            ChatConsola.asegurarConversacion(servicioChat, solicitud.getIdEstudiante(), tutor.getIdUsuario());
            System.out.println("Solicitud aceptada. El chat con " + solicitud.getNombreEstudiante()
                    + " ya está disponible en el menú Chat.");
        } catch (RuntimeException e) {
            System.out.println("Solicitud aceptada, pero no se pudo abrir el chat ahora: " + e.getMessage());
            System.out.println("Se intentará de nuevo cuando entres a Chat.");
        }
    }

    // al rechazar no pasa nada más: el estudiante solo recibe el mensaje
    // devuelve true si se rechazó

    private boolean rechazar(Perfil_usuario tutor, Solicitud solicitud) {
        while (true) {
            System.out.print("Mensaje para el estudiante (obligatorio, 0 para cancelar): ");
            String motivo = sc.nextLine().trim();

            if (motivo.equals("0")) {
                return false;
            }
            if (!motivo.isEmpty()) {
                solicitudes.rechazarSolicitud(tutor, solicitud.getIdSolicitud(), motivo);
                System.out.println("Solicitud rechazada. El estudiante verá tu mensaje.");
                return true;
            }
            System.out.println("Debes escribir un mensaje.");
        }
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    // solo tutores con solicitud de tutor aprobada
    private List<Perfil_usuario> tutoresAprobados() {
        List<Perfil_usuario> aprobados = new ArrayList<>();
        for (Perfil_usuario p : Perfil_usuario.listarPorTipo(Perfil_usuario.TipoUsuario.TUTOR)) {
            SolicitudTutor info = infoTutor(p);
            if (info != null && info.getEstado() == EstadoSolicitud.APROBADO) {
                aprobados.add(p);
            }
        }
        return aprobados;
    }

    private SolicitudTutor infoTutor(Perfil_usuario tutor) {
        try {
            return solicitudTutorDAO.obtener(tutor.getIdUsuario());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<Perfil_usuario> filtrar(List<Perfil_usuario> tutores, String texto) {
        String buscado = texto.trim().toLowerCase();
        List<Perfil_usuario> encontrados = new ArrayList<>();
        for (Perfil_usuario p : tutores) {
            String contenido = (nombreTutor(p) + " " + materiasDe(p)).toLowerCase();
            if (contenido.contains(buscado)) {
                encontrados.add(p);
            }
        }
        return encontrados;
    }

    private String nombreTutor(Perfil_usuario p) {
        SolicitudTutor info = infoTutor(p);
        String nombre = primero(info == null ? null : info.getNombreCompleto(), p.getNombre());
        return (nombre == null || nombre.isBlank()) ? p.getCorreoElectronico() : nombre;
    }

    private String materiasDe(Perfil_usuario p) {
        SolicitudTutor info = infoTutor(p);
        return valor(info == null ? null : info.getMaterias());
    }

    private List<String> listaMaterias(SolicitudTutor info) {
        List<String> materias = new ArrayList<>();
        if (info != null && info.getMaterias() != null) {
            for (String materia : info.getMaterias().split(",")) {
                if (!materia.isBlank()) {
                    materias.add(materia.trim());
                }
            }
        }
        return materias;
    }

    private String primero(String preferido, String alternativa) {
        return (preferido == null || preferido.isBlank()) ? alternativa : preferido;
    }

    private String etiqueta(EstadoSolicitud estado) {
        switch (estado) {
            case APROBADO:
                return "ACEPTADA";
            case RECHAZADO:
                return "RECHAZADA";
            default:
                return "PENDIENTE";
        }
    }

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? "(vacío)" : texto;
    }

    private int leerEntero(String mensaje) {
        System.out.print(mensaje);
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
