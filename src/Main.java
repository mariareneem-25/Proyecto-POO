import java.io.PrintStream;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private static final UsuarioRepositorio repositorioUsuarios = new UsuarioRepositorioPerfil();
    private static final Foro foro = new Foro();
    private static final ServicioChat servicioChat = new ServicioChat(repositorioUsuarios);
    private static final Controlaracceso acceso = new Controlaracceso();
    private static final Gestionarsolicitudes solicitudes = new Gestionarsolicitudes();
    private static final HabilitarChat chatTutoria = new HabilitarChat();
    private static final GestionarCalendario calendario = new GestionarCalendario();
    private static final RepositorioLogros logros = new RepositorioLogros();

    public static void main(String[] args) {
        // quita tildes y la ñ de todo lo que se imprime en consola
        System.setOut(new PrintStream(System.out, true) {
            @Override
            public void print(String s) {
                if (s == null) {
                    super.print(s);
                    return;
                }
                String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .replace("\u00BF", "")
                        .replace("\u00A1", "");
                super.print(sinTildes);
            }
        });

        ConexionDB.crearTablas();
        Perfil_usuario.registrarCorreo("estudiante1@uvg.edu.gt", "estudiante");
        Perfil_usuario.registrarCorreo("director1@uvg.edu.gt", "director");
        Perfil_usuario.registrarCorreo("docente1@uvg.edu.gt", "docente");

        iniciarMenu();
    }

    private static void iniciarMenu() {
        int opcion = -1;

        while (opcion != 4) {
            System.out.println("\n=== Menú ===");
            System.out.println("1. Agregar correo autorizado");
            System.out.println("2. Registrarse");
            System.out.println("3. Iniciar sesión");
            System.out.println("4. Salir");
            System.out.print("Selecciona una opción: ");

            if (sc.hasNextInt()) {
                opcion = sc.nextInt();
            } else {
                sc.next();
                continue;
            }
            sc.nextLine();

            if (opcion == 1) {
                agregarCorreoAutorizado();
            } else if (opcion == 2) {
                registrar();
            } else if (opcion == 3) {
                iniciarSesion();
            }
        }

        System.out.println("Programa finalizado.");
    }

    private static int leerEntero(String mensaje) {
        System.out.print(mensaje);
        String linea = sc.nextLine().trim();
        try {
            return Integer.parseInt(linea);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void agregarCorreoAutorizado() {
        System.out.print("Correo a autorizar: ");
        String correo = sc.nextLine();

        System.out.println("Tipo de usuario (estudiante / docente / director): ");
        String tipo = sc.nextLine();

        if (Perfil_usuario.registrarCorreo(correo, tipo)) {
            System.out.println("Correo autorizado correctamente.");
        } else {
            System.out.println("No se pudo autorizar el correo (puede que ya exista).");
        }
    }

    private static void registrar() {
        System.out.print("Nombre: ");
        String nombre = sc.nextLine();

        System.out.print("Correo institucional: ");
        String correo = sc.nextLine();

        System.out.print("Contraseña: ");
        String contrasena = sc.nextLine();

        System.out.print("Foto de perfil (ruta o nombre de archivo): ");
        String fotoPerfil = sc.nextLine();

        if (!Perfil_usuario.verificarCorreo(correo)) {
            System.out.println("Registro rechazado: el correo no pertenece al dominio institucional, "
                    + "no está autorizado o ya fue utilizado para crear una cuenta.");
            return;
        }

        String tipoTexto = Perfil_usuario.obtenerTipoUsuario(correo);
        if (tipoTexto == null) {
            System.out.println("Registro rechazado: no se encontró el tipo de usuario para este correo.");
            return;
        }

        Perfil_usuario.TipoUsuario tipo;
        switch (tipoTexto.toLowerCase()) {
            case "estudiante":
                tipo = seleccionarRolEstudiante();
                break;
            case "director":
                tipo = Perfil_usuario.TipoUsuario.DIRECTOR;
                break;
            case "docente":
                tipo = Perfil_usuario.TipoUsuario.DOCENTE;
                break;
            default:
                System.out.println("Registro rechazado: tipo de usuario no reconocido.");
                return;
        }

        Perfil_usuario usuario = Perfil_usuario.crearUsuario(nombre, correo, contrasena, fotoPerfil);

        if (usuario != null) {
            System.out.println("Cuenta creada correctamente:");
            System.out.println(usuario);
        } else {
            System.out.println("No se pudo guardar la cuenta en la base de datos.");
        }
    }

    private static Perfil_usuario.TipoUsuario seleccionarRolEstudiante() {
        int opcion = -1;

        while (opcion != 1 && opcion != 2) {
            System.out.println("Tu correo corresponde a un estudiante. ¿Cómo deseas registrarte?");
            System.out.println("1. Estudiante (solicita tutorías)");
            System.out.println("2. Tutor (brinda tutorías)");
            System.out.print("Selecciona una opción: ");

            if (sc.hasNextInt()) {
                opcion = sc.nextInt();
            } else {
                sc.next();
                continue;
            }
            sc.nextLine();
        }

        return (opcion == 2) ? Perfil_usuario.TipoUsuario.TUTOR : Perfil_usuario.TipoUsuario.ESTUDIANTE;
    }

    private static void iniciarSesion() {
        System.out.print("Correo: ");
        String correo = sc.nextLine();

        System.out.print("Contraseña: ");
        String contrasena = sc.nextLine();

        Perfil_usuario sesion = Perfil_usuario.iniciarSesion(correo, contrasena);

        if (sesion == null) {
            System.out.println("No se pudo iniciar sesión. Verifica tu correo, tu contraseña o el estado de tu cuenta.");
            return;
        }

        System.out.println("Sesión iniciada como: " + sesion.getNombre());

        // se convierte la sesión a un Usuario real (Estudiante/Tutor) una sola vez
        Usuario usuarioActual = AdaptadorUsuario.convertir(sesion);

        if (usuarioActual instanceof Tutor) {
            System.out.println("Estado de tu solicitud de tutor: " + ((Tutor) usuarioActual).getEstadoSolicitud());
        }

        // docentes y directores pueden validar las solicitudes de tutor
        boolean puedeValidar = sesion.getTipoUsuario() == Perfil_usuario.TipoUsuario.DOCENTE
                || sesion.getTipoUsuario() == Perfil_usuario.TipoUsuario.DIRECTOR;
        int opcionSalir = puedeValidar ? 9 : 8;

        int subopcion = -1;
        while (subopcion != opcionSalir) {
            System.out.println("\n1. Actualizar perfil");
            System.out.println("2. Eliminar perfil");
            System.out.println("3. Foro académico");
            System.out.println("4. Chat");
            System.out.println("5. Solicitudes de tutoría");
            System.out.println("6. Chat de tutoría");
            System.out.println("7. Calendario");
            if (puedeValidar) {
                System.out.println("8. Validar solicitudes de tutor");
            }
            System.out.println(opcionSalir + ". Cerrar sesión");
            System.out.print("Selecciona una opción: ");

            if (sc.hasNextInt()) {
                subopcion = sc.nextInt();
            } else {
                sc.next();
                continue;
            }
            sc.nextLine();

            if (subopcion == 1) {
                actualizarPerfil(sesion);
            } else if (subopcion == 2) {
                if (sesion.eliminarPerfil()) {
                    System.out.println("Perfil eliminado.");
                } else {
                    System.out.println("No se pudo eliminar el perfil.");
                }
                subopcion = opcionSalir;
            } else if (subopcion == 3) {
                menuForo(usuarioActual);
            } else if (subopcion == 4) {
                menuChat(usuarioActual);
            } else if (subopcion == 5) {
                menuSolicitudes(sesion);
            } else if (subopcion == 6) {
                menuChatTutoria(sesion);
            } else if (subopcion == 7) {
                verCalendario(sesion);
            } else if (puedeValidar && subopcion == 8) {
                validarSolicitudesTutor(sesion);
            }
        }

        sesion.cerrarSesion();
    }

    private static void actualizarPerfil(Perfil_usuario sesion) {
        System.out.print("Nuevo nombre: ");
        String nuevoNombre = sc.nextLine();
        System.out.print("Nueva contraseña: ");
        String nuevaContrasena = sc.nextLine();
        System.out.print("Nueva foto de perfil: ");
        String nuevaFoto = sc.nextLine();

        if (sesion.actualizarPerfil(nuevoNombre, nuevaContrasena, nuevaFoto)) {
            System.out.println("Perfil actualizado.");
        } else {
            System.out.println("No se pudo actualizar el perfil.");
        }
    }

    // ---------------------------------------------------------------
    // Validación de solicitudes de tutor (docentes y directores)
    // ---------------------------------------------------------------

    private static void validarSolicitudesTutor(Perfil_usuario validador) {
        List<Perfil_usuario> pendientes = Perfil_usuario.listarTutoresPendientes();

        if (pendientes.isEmpty()) {
            System.out.println("No hay solicitudes de tutor pendientes.");
            return;
        }

        System.out.println("\nSolicitudes de tutor pendientes:");
        for (int i = 0; i < pendientes.size(); i++) {
            Perfil_usuario p = pendientes.get(i);
            System.out.println((i + 1) + ". " + p.getNombre() + " <" + p.getCorreoElectronico() + ">");
        }

        int eleccion = leerEntero("Selecciona una solicitud (0 para volver): ");
        if (eleccion < 1 || eleccion > pendientes.size()) {
            return;
        }

        Perfil_usuario tutor = pendientes.get(eleccion - 1);

        System.out.println("\n1. Aprobar");
        System.out.println("2. Rechazar");
        System.out.println("3. Cancelar");
        int decision = leerEntero("Selecciona una opción: ");

        if (decision == 1) {
            if (Perfil_usuario.guardarEstadoSolicitudTutor(
                    tutor.getIdUsuario(), EstadoSolicitud.APROBADO, validador.getIdUsuario())) {
                System.out.println("Solicitud aprobada.");
            } else {
                System.out.println("No se pudo guardar la decisión.");
            }
        } else if (decision == 2) {
            if (Perfil_usuario.guardarEstadoSolicitudTutor(
                    tutor.getIdUsuario(), EstadoSolicitud.RECHAZADO, validador.getIdUsuario())) {
                System.out.println("Solicitud rechazada.");
            } else {
                System.out.println("No se pudo guardar la decisión.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Foro académico
    // ---------------------------------------------------------------

    private static void menuForo(Usuario usuarioForo) {
        if (usuarioForo == null) {
            System.out.println("Tu tipo de cuenta todavía no puede participar en el foro académico.");
            return;
        }

        int opcion = -1;
        while (opcion != 3) {
            System.out.println("\n--- Foro académico ---");
            System.out.println("1. Ver publicaciones");
            System.out.println("2. Crear publicación");
            System.out.println("3. Volver");
            System.out.print("Selecciona una opción: ");

            if (sc.hasNextInt()) {
                opcion = sc.nextInt();
            } else {
                sc.next();
                continue;
            }
            sc.nextLine();

            if (opcion == 1) {
                verPublicaciones();
            } else if (opcion == 2) {
                crearPublicacion(usuarioForo);
            }
        }
    }

    private static void verPublicaciones() {
        List<Publicacion> feed = foro.obtenerFeed();

        if (feed.isEmpty()) {
            System.out.println("Todavía no hay publicaciones en el foro.");
            return;
        }

        for (Publicacion p : feed) {
            System.out.println("\n[" + p.getTitulo() + "]");
            System.out.println("Por: " + p.getAutorMostrado());
            System.out.println(p.getContenido());
            System.out.println("Respuestas: " + p.getRespuestas().size());
        }
    }

    private static void crearPublicacion(Usuario usuarioForo) {
        System.out.print("Título: ");
        String titulo = sc.nextLine();

        System.out.print("Contenido: ");
        String cuerpo = sc.nextLine();

        boolean anonimo = false;
        if (usuarioForo.puedeSerAnonimo()) {
            System.out.print("¿Publicar de forma anónima? (s/n): ");
            String respuesta = sc.nextLine();
            anonimo = respuesta.equalsIgnoreCase("s");
        }

        try {
            Publicacion publicacion = new Publicacion(usuarioForo, titulo, cuerpo, anonimo);
            foro.publicar(publicacion);
            System.out.println("Publicación creada correctamente.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo crear la publicación: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Chat estudiante - tutor
    // ---------------------------------------------------------------

    private static void menuChat(Usuario usuario) {
        if (usuario == null) {
            System.out.println("Tu tipo de cuenta todavía no puede usar el chat.");
            return;
        }

        int opcion = -1;
        while (opcion != 3) {
            System.out.println("\n--- Chat ---");
            System.out.println("1. Mis conversaciones");
            System.out.println("2. Nueva conversación");
            System.out.println("3. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            if (opcion == 1) {
                verConversaciones(usuario);
            } else if (opcion == 2) {
                nuevaConversacion(usuario);
            }
        }
    }

    private static void verConversaciones(Usuario usuario) {
        List<Conversacion> conversaciones;
        try {
            conversaciones = servicioChat.conversacionesDe(usuario);
        } catch (RuntimeException e) {
            System.out.println("No se pudieron cargar las conversaciones: " + e.getMessage());
            return;
        }

        if (conversaciones.isEmpty()) {
            System.out.println("Todavía no tienes conversaciones.");
            return;
        }

        for (int i = 0; i < conversaciones.size(); i++) {
            Conversacion c = conversaciones.get(i);
            System.out.println((i + 1) + ". " + c.getOtroParticipante(usuario).getNombreCompleto()
                    + " (" + c.getMensajes().size() + " mensajes)");
        }

        int eleccion = leerEntero("Selecciona una conversación (0 para volver): ");
        if (eleccion >= 1 && eleccion <= conversaciones.size()) {
            abrirConversacion(conversaciones.get(eleccion - 1), usuario);
        }
    }

    private static void nuevaConversacion(Usuario usuario) {
        boolean soyEstudiante = usuario instanceof Estudiante;

        Perfil_usuario.TipoUsuario tipoBuscado = soyEstudiante
                ? Perfil_usuario.TipoUsuario.TUTOR
                : Perfil_usuario.TipoUsuario.ESTUDIANTE;

        List<Perfil_usuario> candidatos = Perfil_usuario.listarPorTipo(tipoBuscado);
        candidatos.removeIf(p -> String.valueOf(p.getIdUsuario()).equals(usuario.getIdUsuario()));

        if (candidatos.isEmpty()) {
            System.out.println("No hay " + (soyEstudiante ? "tutores" : "estudiantes") + " disponibles.");
            return;
        }

        System.out.println("\n" + (soyEstudiante ? "Tutores" : "Estudiantes") + " disponibles:");
        for (int i = 0; i < candidatos.size(); i++) {
            System.out.println((i + 1) + ". " + candidatos.get(i).getNombre());
        }

        int eleccion = leerEntero("Selecciona una persona (0 para volver): ");
        if (eleccion < 1 || eleccion > candidatos.size()) {
            return;
        }

        Usuario otro = AdaptadorUsuario.convertir(candidatos.get(eleccion - 1));
        if (otro == null) {
            System.out.println("No se pudo abrir la conversación con ese usuario.");
            return;
        }

        try {
            Estudiante estudiante = soyEstudiante ? (Estudiante) usuario : (Estudiante) otro;
            Tutor tutor = soyEstudiante ? (Tutor) otro : (Tutor) usuario;

            Conversacion conversacion = servicioChat.iniciarConversacion(estudiante, tutor);
            abrirConversacion(conversacion, usuario);
        } catch (RuntimeException e) {
            System.out.println("No se pudo iniciar la conversación: " + e.getMessage());
        }
    }

    private static void abrirConversacion(Conversacion conversacion, Usuario usuario) {
        int opcion = -1;

        while (opcion != 4) {
            mostrarMensajes(conversacion, usuario);

            System.out.println("\n1. Enviar mensaje");
            System.out.println("2. Enviar imagen o archivo");
            System.out.println("3. Actualizar");
            System.out.println("4. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            try {
                if (opcion == 1) {
                    System.out.print("Mensaje: ");
                    servicioChat.enviarTexto(conversacion, usuario, sc.nextLine());
                } else if (opcion == 2) {
                    System.out.print("Ruta del archivo: ");
                    String ruta = sc.nextLine();
                    System.out.print("Descripción (opcional): ");
                    String descripcion = sc.nextLine();
                    servicioChat.enviarArchivo(conversacion, usuario, ruta, descripcion);
                    System.out.println("Archivo enviado.");
                } else if (opcion == 3) {
                    conversacion = servicioChat.recargar(conversacion);
                }
            } catch (RuntimeException e) {
                System.out.println("No se pudo completar la acción: " + e.getMessage());
            }
        }
    }

    private static void mostrarMensajes(Conversacion conversacion, Usuario usuario) {
        System.out.println("\n=== Chat con "
                + conversacion.getOtroParticipante(usuario).getNombreCompleto() + " ===");

        List<Mensaje> mensajes = conversacion.getMensajes();
        if (mensajes.isEmpty()) {
            System.out.println("(Todavía no hay mensajes)");
            return;
        }

        for (Mensaje m : mensajes) {
            boolean esMio = m.getRemitente().getIdUsuario().equals(usuario.getIdUsuario());
            String autor = esMio ? "Tú" : m.getRemitente().getNombreCompleto();

            String linea = "[" + m.getFecha().format(FORMATO_FECHA) + "] " + autor + ": ";
            if (m.tieneArchivo()) {
                linea += "[" + m.getTipo() + "] " + GestorArchivos.nombreOriginal(m.getRutaArchivo())
                        + " (" + m.getRutaArchivo() + ")";
                if (!m.getContenido().isBlank()) {
                    linea += " - " + m.getContenido();
                }
            } else {
                linea += m.getContenido();
            }
            System.out.println(linea);
        }
    }

    private static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return sc.nextLine();
    }

    private static void menuSolicitudes(Perfil_usuario sesion) {
        if (acceso.puedeEnviarSolicitud(sesion)) {
            menuSolicitudesEstudiante(sesion);
        } else if (acceso.puedeGestionarSolicitudes(sesion)) {
            menuSolicitudesTutor(sesion);
        } else {
            System.out.println("Tu tipo de cuenta no tiene acceso a las solicitudes de tutoría.");
        }
    }

    private static void menuSolicitudesEstudiante(Perfil_usuario sesion) {
        int opcion = -1;

        while (opcion != 7) {
            System.out.println("\n--- Solicitudes de tutoría (estudiante) ---");
            System.out.println("1. Ver tutores disponibles");
            System.out.println("2. Enviar solicitud a un tutor");
            System.out.println("3. Ver mis solicitudes");
            System.out.println("4. Ver mis logros");
            System.out.println("5. Agregar logro");
            System.out.println("6. Completar logro");
            System.out.println("7. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            if (opcion == 1) {
                verTutores(sesion);
            } else if (opcion == 2) {
                enviarSolicitud(sesion);
            } else if (opcion == 3) {
                verMisSolicitudes(sesion);
            } else if (opcion == 4) {
                verMisLogros(sesion);
            } else if (opcion == 5) {
                agregarLogro(sesion);
            } else if (opcion == 6) {
                completarLogro(sesion);
            }
        }
    }

    private static void verTutores(Perfil_usuario sesion) {
        try {
            List<String> tutores = solicitudes.listarTutores(sesion);

            if (tutores.isEmpty()) {
                System.out.println("Todavía no hay tutores registrados.");
            }

            for (String tutor : tutores) {
                System.out.println(tutor);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void enviarSolicitud(Perfil_usuario sesion) {
        int idTutor = leerEntero("Id del tutor: ");
        String materia = leerTexto("Materia: ");
        String mensaje = leerTexto("Mensaje para el tutor: ");

        try {
            int idSolicitud = solicitudes.crearSolicitud(sesion, idTutor, materia, mensaje);
            System.out.println("Solicitud enviada. Número de solicitud: " + idSolicitud);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo enviar la solicitud: " + e.getMessage());
        }
    }

    private static void verMisSolicitudes(Perfil_usuario sesion) {
        try {
            List<Solicitud> lista = solicitudes.listarMisSolicitudes(sesion);

            if (lista.isEmpty()) {
                System.out.println("No has enviado solicitudes.");
            }

            for (Solicitud solicitud : lista) {
                System.out.println(solicitud);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void verMisLogros(Perfil_usuario sesion) {
        try {
            List<Logro> lista = logros.obtenerLogros(sesion, sesion.getIdUsuario());

            if (lista.isEmpty()) {
                System.out.println("No tienes logros registrados.");
            }

            for (Logro logro : lista) {
                System.out.println(logro);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void agregarLogro(Perfil_usuario sesion) {
        String tema = leerTexto("Tema del logro: ");

        try {
            logros.agregarLogro(sesion, tema);
            System.out.println("Logro agregado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo agregar el logro: " + e.getMessage());
        }
    }

    private static void completarLogro(Perfil_usuario sesion) {
        String tema = leerTexto("Tema del logro a completar: ");

        try {
            logros.completarLogro(sesion, tema);
            System.out.println("Logro marcado como completado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo completar el logro: " + e.getMessage());
        }
    }

    private static void menuSolicitudesTutor(Perfil_usuario sesion) {
        int opcion = -1;

        while (opcion != 5) {
            System.out.println("\n--- Solicitudes de tutoría (tutor) ---");
            System.out.println("1. Ver solicitudes pendientes");
            System.out.println("2. Consultar una solicitud");
            System.out.println("3. Aceptar una solicitud");
            System.out.println("4. Rechazar una solicitud");
            System.out.println("5. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            if (opcion == 1) {
                verSolicitudesPendientes(sesion);
            } else if (opcion == 2) {
                consultarSolicitud(sesion);
            } else if (opcion == 3) {
                aceptarSolicitud(sesion);
            } else if (opcion == 4) {
                rechazarSolicitud(sesion);
            }
        }
    }

    private static void verSolicitudesPendientes(Perfil_usuario sesion) {
        try {
            List<Solicitud> lista = solicitudes.listarPendientes(sesion);

            if (lista.isEmpty()) {
                System.out.println("No tienes solicitudes pendientes.");
            }

            for (Solicitud solicitud : lista) {
                System.out.println(solicitud);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void consultarSolicitud(Perfil_usuario sesion) {
        int idSolicitud = leerEntero("Número de solicitud: ");

        try {
            System.out.println(solicitudes.consultarSolicitud(sesion, idSolicitud));
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void aceptarSolicitud(Perfil_usuario sesion) {
        int idSolicitud = leerEntero("Número de solicitud a aceptar: ");

        try {
            solicitudes.aceptarSolicitud(sesion, idSolicitud);
            System.out.println("Solicitud aceptada. El chat privado ya está habilitado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo aceptar la solicitud: " + e.getMessage());
        }
    }

    private static void rechazarSolicitud(Perfil_usuario sesion) {
        int idSolicitud = leerEntero("Número de solicitud a rechazar: ");
        String motivo = leerTexto("Motivo del rechazo: ");

        try {
            solicitudes.rechazarSolicitud(sesion, idSolicitud, motivo);
            System.out.println("Solicitud rechazada.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo rechazar la solicitud: " + e.getMessage());
        }
    }

    private static void menuChatTutoria(Perfil_usuario sesion) {
        if (!acceso.puedeUsarChat(sesion)) {
            System.out.println("Tu tipo de cuenta no puede usar el chat privado.");
            return;
        }

        int opcion = -1;

        while (opcion != 6) {
            System.out.println("\n--- Chat de tutoría ---");
            System.out.println("1. Ver mis chats");
            System.out.println("2. Ver mensajes de un chat");
            System.out.println("3. Enviar mensaje");
            System.out.println("4. Proponer fecha de tutoría");
            System.out.println("5. Ver y responder propuestas de fecha");
            System.out.println("6. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            if (opcion == 1) {
                verChats(sesion);
            } else if (opcion == 2) {
                verMensajes(sesion);
            } else if (opcion == 3) {
                enviarMensaje(sesion);
            } else if (opcion == 4) {
                proponerFecha(sesion);
            } else if (opcion == 5) {
                responderPropuestas(sesion);
            }
        }
    }

    private static void verChats(Perfil_usuario sesion) {
        try {
            List<Solicitud> chats = chatTutoria.listarChatsDisponibles(sesion);

            if (chats.isEmpty()) {
                System.out.println("Todavía no tienes chats habilitados. Se habilitan cuando se acepta una solicitud.");
            }

            for (Solicitud solicitud : chats) {
                System.out.println("Chat #" + solicitud.getIdSolicitud() + " | Estudiante: " + solicitud.getNombreEstudiante()
                        + " | Tutor: " + solicitud.getNombreTutor() + " | Materia: " + solicitud.getMateria());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void verMensajes(Perfil_usuario sesion) {
        int idSolicitud = leerEntero("Número de chat: ");

        try {
            List<MensajeChat> mensajes = chatTutoria.obtenerMensajes(sesion, idSolicitud);

            if (mensajes.isEmpty()) {
                System.out.println("Todavía no hay mensajes en este chat.");
            }

            for (MensajeChat mensaje : mensajes) {
                System.out.println(mensaje);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void enviarMensaje(Perfil_usuario sesion) {
        int idSolicitud = leerEntero("Número de chat: ");
        String texto = leerTexto("Mensaje: ");

        try {
            chatTutoria.enviarMensaje(sesion, idSolicitud, texto);
            System.out.println("Mensaje enviado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo enviar el mensaje: " + e.getMessage());
        }
    }

    private static void proponerFecha(Perfil_usuario sesion) {
        int idSolicitud = leerEntero("Número de chat: ");
        String fecha = leerTexto("Fecha y hora (aaaa-MM-dd HH:mm): ");

        try {
            int idPropuesta = calendario.proponerFecha(sesion, idSolicitud, fecha);
            System.out.println("Propuesta enviada por el chat (propuesta #" + idPropuesta + ").");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo proponer la fecha: " + e.getMessage());
        }
    }

    private static void responderPropuestas(Perfil_usuario sesion) {
        int idSolicitud = leerEntero("Número de chat: ");

        try {
            List<PropuestaFecha> pendientes = calendario.listarPropuestasPendientes(sesion, idSolicitud);

            if (pendientes.isEmpty()) {
                System.out.println("No hay propuestas pendientes en este chat.");
                return;
            }

            for (PropuestaFecha propuesta : pendientes) {
                System.out.println(propuesta);
            }

            int idPropuesta = leerEntero("Número de propuesta a responder (0 para cancelar): ");
            if (idPropuesta == 0) {
                return;
            }

            String respuesta = leerTexto("¿Aceptar la fecha? (s/n): ");
            calendario.responderPropuesta(sesion, idPropuesta, respuesta.equalsIgnoreCase("s"));
            System.out.println("Respuesta enviada por el chat.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("No se pudo responder la propuesta: " + e.getMessage());
        }
    }

    private static void verCalendario(Perfil_usuario sesion) {
        try {
            List<PropuestaFecha> reservadas = calendario.verCalendario(sesion);

            System.out.println("\n--- Calendario: fechas reservadas ---");
            if (reservadas.isEmpty()) {
                System.out.println("No tienes fechas reservadas.");
            }

            for (PropuestaFecha reserva : reservadas) {
                System.out.println(reserva.descripcionReserva());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }
}