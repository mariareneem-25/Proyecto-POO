import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final Foro foro = new Foro();

    public static void main(String[] args) {
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

        Usuario usuarioForo = AdaptadorUsuario.convertir(sesion);

        int subopcion = -1;
        while (subopcion != 4) {
            System.out.println("\n1. Actualizar perfil");
            System.out.println("2. Eliminar perfil");
            System.out.println("3. Foro académico");
            System.out.println("4. Cerrar sesión");
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
                subopcion = 4;
            } else if (subopcion == 3) {
                menuForo(usuarioForo);
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
}