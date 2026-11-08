import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// pantallas de consola de la solicitud de tutor
// Main solo la crea y llama a gestionarAcceso() (tutores) o revisarSolicitudes() (docentes y directores)
//
// lado del tutor: llena su información, la guarda o la envía a un docente/director
// lado del revisor: ve la información, la acepta, la rechaza (siempre con motivo) o regresa

public class SolicitudTutorConsola {

    private final Scanner sc;
    private final SelectorImagen selectorImagen;
    private final SolicitudTutorDAO dao = new SolicitudTutorDAO();

    public SolicitudTutorConsola(Scanner sc) {
        this(sc, new SelectorImagenConsola(sc));
    }

    public SolicitudTutorConsola(Scanner sc, SelectorImagen selectorImagen) {
        this.sc = sc;
        this.selectorImagen = selectorImagen;
    }

    // ---------------------------------------------------------------
    // Lado del tutor
    // ---------------------------------------------------------------

    // se llama al iniciar sesión un tutor
    // devuelve true solo si su solicitud ya fue aprobada y puede entrar al menú

    public boolean gestionarAcceso(Perfil_usuario tutor) {
        SolicitudTutor solicitud = dao.obtener(tutor.getIdUsuario());

        if (solicitud == null) {
            // primera vez: se adelanta lo que ya dio al registrarse (no se guarda hasta que elija guardar)
            solicitud = new SolicitudTutor(tutor.getIdUsuario());
            solicitud.setNombreCompleto(tutor.getNombre());
            solicitud.setFotoPerfil(tutor.getFotoPerfil());
        }

        switch (solicitud.getEstado()) {
            case APROBADO:
                return true;

            case PENDIENTE:
                mostrarPendiente(solicitud);
                return false;

            case RECHAZADO:
                return menuRechazo(solicitud);

            default:
                return formulario(solicitud);
        }
    }

    private boolean formulario(SolicitudTutor solicitud) {
        System.out.println("\n--- Solicitud para ser tutor ---");
        System.out.println("Debes completar tu información y enviar tu solicitud para acceder al menú.");

        while (true) {
            System.out.println();
            System.out.println("1. Nombre completo: " + valor(solicitud.getNombreCompleto()));
            System.out.println("2. Descripción: " + valor(solicitud.getDescripcion()));
            System.out.println("3. Materias que impartes: " + valor(solicitud.getMaterias()));
            System.out.println("4. Foto de perfil: " + valor(solicitud.getFotoPerfil()));
            System.out.println("5. Banner: " + valor(solicitud.getBanner()));
            System.out.println("6. Guardar y salir");
            if (solicitud.estaCompleta()) {
                System.out.println("7. Enviar solicitud");
            } else {
                System.out.println("(Para poder enviar la solicitud falta: "
                        + String.join(", ", solicitud.camposFaltantes()) + ")");
            }
            System.out.println("0. Salir sin guardar");
            int opcion = leerEntero("Selecciona una opción: ");

            switch (opcion) {
                case 1:
                    System.out.print("Nombre completo: ");
                    solicitud.setNombreCompleto(sc.nextLine().trim());
                    break;
                case 2:
                    System.out.print("Descripción: ");
                    solicitud.setDescripcion(sc.nextLine().trim());
                    break;
                case 3:
                    System.out.print("Materias que impartes (separadas por coma): ");
                    solicitud.setMateriasDesdeTexto(sc.nextLine());
                    break;
                case 4:
                    solicitud.setFotoPerfil(selectorImagen.elegir(
                            GestorArchivos.Categoria.PERFIL, "Foto de perfil", solicitud.getFotoPerfil()));
                    break;
                case 5:
                    solicitud.setBanner(selectorImagen.elegir(
                            GestorArchivos.Categoria.BANNER_TUTOR, "Banner", solicitud.getBanner()));
                    break;
                case 6:
                    if (guardar(solicitud)) {
                        System.out.println("Progreso guardado. Podrás seguir editando cuando vuelvas a iniciar sesión.");
                    }
                    return false;
                case 7:
                    if (solicitud.estaCompleta() && enviar(solicitud)) {
                        return false;
                    }
                    break;
                case 0:
                    return false;
                default:
                    break;
            }
        }
    }

    private boolean guardar(SolicitudTutor solicitud) {
        try {
            dao.guardarBorrador(solicitud);
            return true;
        } catch (RuntimeException e) {
            System.out.println("No se pudo guardar: " + e.getMessage());
            return false;
        }
    }

    // guarda el borrador, deja elegir a quién enviarla y la marca como pendiente
    // devuelve true si la solicitud quedó enviada

    private boolean enviar(SolicitudTutor solicitud) {
        if (!guardar(solicitud)) {
            return false;
        }

        Perfil_usuario revisor = elegirRevisor();
        if (revisor == null) {
            System.out.println("Tu información quedó guardada, pero la solicitud todavía no se ha enviado.");
            return false;
        }

        try {
            if (dao.enviar(solicitud.getIdTutor(), revisor.getIdUsuario())) {
                System.out.println("Solicitud enviada a " + nombreVisible(revisor)
                        + ". Podrás acceder al menú cuando la aprueben.");
                return true;
            }
            System.out.println("No se pudo enviar la solicitud.");
        } catch (RuntimeException e) {
            System.out.println("No se pudo enviar la solicitud: " + e.getMessage());
        }
        return false;
    }

    // lista docentes y directores (nombre y foto) y permite buscar por nombre
    // devuelve la cuenta elegida o null si se cancela

    private Perfil_usuario elegirRevisor() {
        List<Perfil_usuario> todos = new ArrayList<>();
        todos.addAll(Perfil_usuario.listarPorTipo(Perfil_usuario.TipoUsuario.DOCENTE));
        todos.addAll(Perfil_usuario.listarPorTipo(Perfil_usuario.TipoUsuario.DIRECTOR));

        if (todos.isEmpty()) {
            System.out.println("Todavía no hay docentes ni directores registrados para recibir tu solicitud.");
            return null;
        }

        List<Perfil_usuario> mostrados = todos;

        while (true) {
            System.out.println("\n--- ¿A quién quieres enviar tu solicitud? ---");

            if (mostrados.isEmpty()) {
                System.out.println("No se encontraron cuentas con ese nombre.");
            }
            for (int i = 0; i < mostrados.size(); i++) {
                Perfil_usuario p = mostrados.get(i);
                System.out.println((i + 1) + ". " + nombreVisible(p) + " (" + rol(p) + ")"
                        + " | Foto: " + valor(p.getFotoPerfil()));
            }

            System.out.print("Número de la cuenta, B para buscar por nombre, T para ver todas, 0 para cancelar: ");
            String entrada = sc.nextLine().trim();

            if (entrada.equals("0")) {
                return null;
            }
            if (entrada.equalsIgnoreCase("b")) {
                System.out.print("Nombre a buscar: ");
                mostrados = filtrarPorNombre(todos, sc.nextLine());
                continue;
            }
            if (entrada.equalsIgnoreCase("t")) {
                mostrados = todos;
                continue;
            }

            try {
                int numero = Integer.parseInt(entrada);
                if (numero >= 1 && numero <= mostrados.size()) {
                    Perfil_usuario elegido = mostrados.get(numero - 1);
                    System.out.print("¿Enviar tu solicitud a " + nombreVisible(elegido) + "? (s/n): ");
                    if (sc.nextLine().trim().equalsIgnoreCase("s")) {
                        return elegido;
                    }
                }
            } catch (NumberFormatException e) {
                // entrada no válida: se vuelve a mostrar la lista
            }
        }
    }

    private List<Perfil_usuario> filtrarPorNombre(List<Perfil_usuario> cuentas, String texto) {
        String buscado = texto.trim().toLowerCase();
        List<Perfil_usuario> encontradas = new ArrayList<>();
        for (Perfil_usuario p : cuentas) {
            if (nombreVisible(p).toLowerCase().contains(buscado)) {
                encontradas.add(p);
            }
        }
        return encontradas;
    }

    private void mostrarPendiente(SolicitudTutor solicitud) {
        String destino = "un docente o director";
        if (solicitud.getIdRevisor() != null) {
            Perfil_usuario revisor = Perfil_usuario.buscarPorId(solicitud.getIdRevisor());
            if (revisor != null) {
                destino = nombreVisible(revisor);
            }
        }
        System.out.println("\nTu solicitud para ser tutor fue enviada a " + destino + " y sigue pendiente.");
        System.out.println("Podrás acceder al menú cuando sea aprobada.");
    }

    // solicitud rechazada: se muestra el motivo y se puede crear una nueva
    // "Seguir como estudiante" ya aparece en el menú pero todavía no funciona

    private boolean menuRechazo(SolicitudTutor solicitud) {
        String quien = "un docente o director";
        if (solicitud.getIdRevisor() != null) {
            Perfil_usuario revisor = Perfil_usuario.buscarPorId(solicitud.getIdRevisor());
            if (revisor != null) {
                quien = nombreVisible(revisor);
            }
        }

        System.out.println("\nTu solicitud para ser tutor fue rechazada por " + quien + ".");
        String motivo = solicitud.getMotivoRechazo();
        System.out.println("Motivo: " + ((motivo == null || motivo.isBlank()) ? "(sin motivo registrado)" : motivo));

        while (true) {
            System.out.println("\n1. Crear nueva solicitud");
            System.out.println("2. Seguir como estudiante");
            System.out.println("3. Volver");
            int opcion = leerEntero("Selecciona una opción: ");

            if (opcion == 1) {
                try {
                    if (dao.reabrir(solicitud.getIdTutor())) {
                        solicitud.setEstado(EstadoSolicitud.BORRADOR);
                        return formulario(solicitud);
                    }
                    System.out.println("No se pudo crear una nueva solicitud.");
                } catch (RuntimeException e) {
                    System.out.println("No se pudo crear una nueva solicitud: " + e.getMessage());
                }
            } else if (opcion == 2) {
                // pendiente de implementar
                System.out.println("Esta opción todavía no está disponible.");
            } else if (opcion == 3) {
                return false;
            }
        }
    }

    // ---------------------------------------------------------------
    // Lado del docente o director
    // ---------------------------------------------------------------

    // solo ve las solicitudes que le enviaron a él
    // siempre puede aceptar, rechazar (con motivo) o regresar

    public void revisarSolicitudes(Perfil_usuario revisor) {
        if (revisor == null || !revisor.isSesionActiva()
                || (revisor.getTipoUsuario() != Perfil_usuario.TipoUsuario.DOCENTE
                && revisor.getTipoUsuario() != Perfil_usuario.TipoUsuario.DIRECTOR)) {
            System.out.println("Tu tipo de cuenta no puede revisar solicitudes de tutor.");
            return;
        }

        while (true) {
            List<SolicitudTutor> pendientes;
            try {
                pendientes = dao.pendientesPara(revisor.getIdUsuario());
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }

            if (pendientes.isEmpty()) {
                System.out.println("No tienes solicitudes de tutor pendientes.");
                return;
            }

            System.out.println("\nSolicitudes de tutor pendientes:");
            for (int i = 0; i < pendientes.size(); i++) {
                System.out.println((i + 1) + ". " + valor(pendientes.get(i).getNombreCompleto()));
            }

            int eleccion = leerEntero("Selecciona una solicitud (0 para volver): ");
            if (eleccion < 1 || eleccion > pendientes.size()) {
                return;
            }

            revisarUna(revisor, pendientes.get(eleccion - 1));
        }
    }

    private void revisarUna(Perfil_usuario revisor, SolicitudTutor solicitud) {
        while (true) {
            System.out.println("\n--- Solicitud de tutor ---");
            System.out.println("Nombre completo: " + valor(solicitud.getNombreCompleto()));
            System.out.println("Materias que imparte: " + valor(solicitud.getMaterias()));
            System.out.println("Descripción: " + valor(solicitud.getDescripcion()));
            System.out.println("Foto de perfil: " + valor(solicitud.getFotoPerfil()));
            System.out.println("Banner: " + valor(solicitud.getBanner()));

            System.out.println("\n1. Aceptar");
            System.out.println("2. Rechazar");
            System.out.println("3. Regresar");
            int decision = leerEntero("Selecciona una opción: ");

            try {
                if (decision == 1) {
                    if (dao.aprobar(solicitud, revisor.getIdUsuario())) {
                        System.out.println("Solicitud aceptada. El tutor ya puede acceder al menú.");
                    } else {
                        System.out.println("No se pudo aceptar la solicitud (puede que ya no esté pendiente).");
                    }
                    return;
                } else if (decision == 2) {
                    String motivo = pedirMotivo();
                    if (motivo == null) {
                        continue;
                    }
                    if (dao.rechazar(solicitud.getIdTutor(), revisor.getIdUsuario(), motivo)) {
                        System.out.println("Solicitud rechazada. El tutor verá tu motivo.");
                    } else {
                        System.out.println("No se pudo rechazar la solicitud (puede que ya no esté pendiente).");
                    }
                    return;
                } else if (decision == 3) {
                    return;
                }
            } catch (RuntimeException e) {
                System.out.println("No se pudo guardar la decisión: " + e.getMessage());
                return;
            }
        }
    }

    // el motivo es obligatorio; escribir 0 cancela el rechazo y devuelve null

    private String pedirMotivo() {
        while (true) {
            System.out.print("Motivo del rechazo (obligatorio, 0 para cancelar): ");
            String motivo = sc.nextLine().trim();
            if (motivo.equals("0")) {
                return null;
            }
            if (!motivo.isEmpty()) {
                return motivo;
            }
            System.out.println("Debes escribir el motivo del rechazo.");
        }
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? "(vacío)" : texto;
    }

    // si la cuenta no tiene nombre todavía, se muestra el correo para poder reconocerla

    private String nombreVisible(Perfil_usuario p) {
        String nombre = p.getNombre();
        return (nombre == null || nombre.isBlank()) ? p.getCorreoElectronico() : nombre;
    }

    private String rol(Perfil_usuario p) {
        return p.getTipoUsuario() == Perfil_usuario.TipoUsuario.DIRECTOR ? "Director de carrera" : "Docente";
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
