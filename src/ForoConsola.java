import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// pantalla de consola del foro académico
// Main solo la crea y llama a abrir(); aquí vive todo el menú del foro
// permisos: los estudiantes publican y comentan; tutores y docentes solo comentan
// (los permisos salen de Usuario.puedePublicar() y Usuario.puedeComentar())

public class ForoConsola {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Scanner sc;
    private final Foro foro;
    private final UsuarioRepositorio repositorio;

    public ForoConsola(Scanner sc, Foro foro, UsuarioRepositorio repositorio) {
        this.sc = sc;
        this.foro = foro;
        this.repositorio = repositorio;
    }

    public void abrir(Usuario usuario) {
        if (usuario == null) {
            System.out.println("Tu tipo de cuenta todavía no puede participar en el foro académico.");
            return;
        }

        // se recargan las publicaciones guardadas para ver también las de sesiones anteriores
        try {
            foro.cargarDesdeBaseDeDatos(repositorio);
        } catch (RuntimeException e) {
            System.out.println("No se pudieron cargar las publicaciones: " + e.getMessage());
        }

        // quien no puede publicar no ve la opción de crear y "Volver" pasa a ser la 2
        int opcionSalir = usuario.puedePublicar() ? 3 : 2;

        int opcion = -1;
        while (opcion != opcionSalir) {
            System.out.println("\n--- Foro académico ---");
            System.out.println("1. Ver publicaciones y comentarios");
            if (usuario.puedePublicar()) {
                System.out.println("2. Crear publicación");
            }
            System.out.println(opcionSalir + ". Volver");
            opcion = leerEntero("Selecciona una opción: ");

            if (opcion == 1) {
                verPublicaciones(usuario);
            } else if (opcion == 2 && usuario.puedePublicar()) {
                crearPublicacion(usuario);
            }
        }
    }

    // ---------------------------------------------------------------
    // Publicaciones
    // ---------------------------------------------------------------

    private void verPublicaciones(Usuario usuario) {
        List<Publicacion> feed = foro.obtenerFeed();

        if (feed.isEmpty()) {
            System.out.println("Todavía no hay publicaciones en el foro.");
            return;
        }

        for (int i = 0; i < feed.size(); i++) {
            Publicacion p = feed.get(i);
            System.out.println((i + 1) + ". [" + p.getTitulo() + "] por " + autor(p)
                    + " - " + contarComentarios(p.getRespuestas()) + " comentarios");
        }

        int eleccion = leerEntero("Abrir publicación (0 para volver): ");
        if (eleccion >= 1 && eleccion <= feed.size()) {
            abrirPublicacion(usuario, feed.get(eleccion - 1));
        }
    }

    private void crearPublicacion(Usuario usuario) {
        System.out.print("Título: ");
        String titulo = sc.nextLine();

        System.out.print("Contenido: ");
        String cuerpo = sc.nextLine();

        boolean anonimo = preguntarAnonimo(usuario, "Publicar");

        try {
            Publicacion publicacion = new Publicacion(usuario, titulo, cuerpo, anonimo);
            foro.publicar(publicacion);
            System.out.println("Publicación creada correctamente.");
        } catch (RuntimeException e) {
            System.out.println("No se pudo crear la publicación: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Una publicación con sus comentarios
    // ---------------------------------------------------------------

    private void abrirPublicacion(Usuario usuario, Publicacion p) {
        int opcion = -1;
        while (opcion != 0) {
            System.out.println("\n[" + p.getTitulo() + "]");
            System.out.println("Por: " + autor(p) + " | " + p.getFecha().format(FORMATO_FECHA));
            System.out.println(p.getContenido());

            List<Comentario> planos = new ArrayList<>();
            if (p.getRespuestas().isEmpty()) {
                System.out.println("\nTodavía no hay comentarios.");
            } else {
                System.out.println("\nComentarios:");
                mostrarComentarios(p.getRespuestas(), 0, planos);
            }

            if (!usuario.puedeComentar()) {
                return;
            }

            System.out.println("\n1. Comentar la publicación");
            if (!planos.isEmpty()) {
                System.out.println("2. Responder a un comentario");
            }
            System.out.println("0. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            if (opcion == 1) {
                comentarPublicacion(usuario, p);
            } else if (opcion == 2 && !planos.isEmpty()) {
                responderComentario(usuario, p, planos);
            }
        }
    }

    private void comentarPublicacion(Usuario usuario, Publicacion p) {
        System.out.print("Comentario: ");
        String texto = sc.nextLine();
        boolean anonimo = preguntarAnonimo(usuario, "Comentar");

        try {
            foro.responder(p, new Comentario(usuario, texto, anonimo));
            System.out.println("Comentario publicado.");
        } catch (RuntimeException e) {
            System.out.println("No se pudo comentar: " + e.getMessage());
        }
    }

    private void responderComentario(Usuario usuario, Publicacion p, List<Comentario> planos) {
        int eleccion = leerEntero("Número del comentario a responder (0 para cancelar): ");
        if (eleccion < 1 || eleccion > planos.size()) {
            return;
        }

        System.out.print("Respuesta: ");
        String texto = sc.nextLine();
        boolean anonimo = preguntarAnonimo(usuario, "Responder");

        try {
            Comentario padre = planos.get(eleccion - 1);
            foro.responderComentario(p.getId(), padre, new Comentario(usuario, texto, anonimo));
            System.out.println("Respuesta publicada.");
        } catch (RuntimeException e) {
            System.out.println("No se pudo responder: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    // imprime los comentarios con sangría por nivel y los numera
    // llena "planos" para poder elegir uno por su número

    private void mostrarComentarios(List<Comentario> comentarios, int nivel, List<Comentario> planos) {
        for (Comentario c : comentarios) {
            planos.add(c);
            System.out.println("  ".repeat(nivel) + planos.size() + ". " + autor(c)
                    + " (" + c.getFecha().format(FORMATO_FECHA) + "): " + c.getContenido());
            mostrarComentarios(c.getRespuestasAlComentario(), nivel + 1, planos);
        }
    }

    private int contarComentarios(List<Comentario> comentarios) {
        int total = 0;
        for (Comentario c : comentarios) {
            total += 1 + contarComentarios(c.getRespuestasAlComentario());
        }
        return total;
    }

    // nombre mostrado (respeta el anonimato) y, si no es anónimo, el rol de tutores y docentes

    private String autor(Publicable p) {
        String nombre = p.getAutorMostrado();
        if (p.esAnonimo()) {
            return nombre;
        }

        Usuario real = (p instanceof Publicacion)
                ? ((Publicacion) p).getAutorReal()
                : ((Comentario) p).getAutorReal();

        if (real instanceof Docente) {
            return nombre + " (Docente)";
        }
        if (real instanceof Tutor) {
            return nombre + " (Tutor)";
        }
        return nombre;
    }

    private boolean preguntarAnonimo(Usuario usuario, String accion) {
        if (!usuario.puedeSerAnonimo()) {
            return false;
        }
        System.out.print("¿" + accion + " de forma anónima? (s/n): ");
        return sc.nextLine().trim().equalsIgnoreCase("s");
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