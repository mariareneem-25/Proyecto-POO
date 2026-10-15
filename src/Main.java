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

        System.out.println("Tipo de usuario (estudiante / director): ");
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

        int subopcion = -1;
        while (subopcion != 5) {
            System.out.println("\n1. Actualizar perfil");
            System.out.println("2. Eliminar perfil");
            System.out.println("3. Foro académico");
            System.out.println("4. Chat");
            System.out.println("5. Cerrar sesión");
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
                subopcion = 5;
            } else if (subopcion == 3) {
                menuForo(usuarioActual);
            } else if (subopcion == 4) {
                menuChat(usuarioActual);
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

        while (opcion != 3) {
            mostrarMensajes(conversacion, usuario);

            System.out.println("\n1. Enviar mensaje");
            System.out.println("2. Actualizar");
            System.out.println("3. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            try {
                if (opcion == 1) {
                    System.out.print("Mensaje: ");
                    servicioChat.enviarTexto(conversacion, usuario, sc.nextLine());
                } else if (opcion == 2) {
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
            System.out.println("[" + m.getFecha().format(FORMATO_FECHA) + "] " + autor + ": " + m.getContenido());
        }
    }
}