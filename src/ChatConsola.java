import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

// pantallas del chat entre un estudiante y un tutor
// los chats NO se buscan ni se crean a mano: se habilitan cuando el tutor acepta la solicitud del estudiante
// dentro de cada chat se pueden mandar mensajes, imágenes y archivos, y solicitudes de calendario:
// reservar una fecha o eliminar una fecha reservada (las acepta o rechaza la otra persona)

public class ChatConsola {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Scanner sc;
    private final ServicioChat servicioChat;
    private final CalendarioDAO calendario = new CalendarioDAO();

    public ChatConsola(Scanner sc, ServicioChat servicioChat) {
        this.sc = sc;
        this.servicioChat = servicioChat;
    }

    // crea el chat entre el estudiante y el tutor si todavía no existe
    // se usa cuando el tutor acepta una solicitud

    public static void asegurarConversacion(ServicioChat servicio, int idEstudiante, int idTutor) {
        Usuario estudiante = AdaptadorUsuario.convertir(Perfil_usuario.buscarPorId(idEstudiante));
        Usuario tutor = AdaptadorUsuario.convertir(Perfil_usuario.buscarPorId(idTutor));

        if (!(estudiante instanceof Estudiante) || !(tutor instanceof Tutor)) {
            throw new IllegalStateException("No se pudo abrir el chat entre las dos cuentas.");
        }
        servicio.iniciarConversacion((Estudiante) estudiante, (Tutor) tutor);
    }

    public void abrir(Usuario usuario, Perfil_usuario sesion) {
        if (usuario == null || usuario instanceof Docente) {
            System.out.println("Tu tipo de cuenta todavía no puede usar el chat.");
            return;
        }

        // por si hay solicitudes aceptadas que todavía no tienen su chat
        sincronizarChats(sesion);

        while (true) {
            List<Conversacion> conversaciones;
            try {
                conversaciones = servicioChat.conversacionesDe(usuario);
            } catch (RuntimeException e) {
                System.out.println("No se pudieron cargar los chats: " + e.getMessage());
                return;
            }

            System.out.println("\n--- Chat ---");
            if (conversaciones.isEmpty()) {
                System.out.println("Todavía no tienes chats. Se habilitan cuando un tutor acepta una solicitud de tutoría.");
                return;
            }

            for (int i = 0; i < conversaciones.size(); i++) {
                Conversacion c = conversaciones.get(i);
                int porResponder = solicitudesPorResponder(c, usuario);
                System.out.println((i + 1) + ". " + nombre(c.getOtroParticipante(usuario))
                        + " (" + c.getMensajes().size() + " mensajes)"
                        + (porResponder > 0 ? " | " + porResponder + " solicitud(es) de calendario por responder" : ""));
            }

            int eleccion = leerEntero("Selecciona un chat (0 para volver): ");
            if (eleccion < 1 || eleccion > conversaciones.size()) {
                return;
            }
            abrirConversacion(conversaciones.get(eleccion - 1), usuario);
        }
    }

    private void sincronizarChats(Perfil_usuario sesion) {
        String sql = "SELECT idEstudiante, idTutor FROM solicitudes "
                + "WHERE estado = 'APROBADO' AND (idEstudiante = ? OR idTutor = ?)";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, sesion.getIdUsuario());
            ps.setInt(2, sesion.getIdUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        asegurarConversacion(servicioChat, rs.getInt("idEstudiante"), rs.getInt("idTutor"));
                    } catch (RuntimeException e) {
                        // si un chat no se puede crear, se sigue con los demás
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("No se pudieron revisar tus solicitudes aceptadas: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Un chat
    // ---------------------------------------------------------------

    private void abrirConversacion(Conversacion conversacion, Usuario usuario) {
        int opcion = -1;

        while (opcion != 8) {
            mostrarMensajes(conversacion, usuario);
            mostrarPendientes(conversacion, usuario);

            System.out.println("\n1. Enviar mensaje");
            System.out.println("2. Enviar imagen o archivo");
            System.out.println("3. Actualizar");
            System.out.println("4. Solicitar reservar una fecha");
            System.out.println("5. Solicitar eliminar una fecha reservada");
            System.out.println("6. Responder solicitudes de calendario");
            System.out.println("7. Ver mi calendario");
            System.out.println("8. Volver");
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
                } else if (opcion == 4) {
                    solicitarReserva(conversacion, usuario);
                } else if (opcion == 5) {
                    solicitarEliminacion(conversacion, usuario);
                } else if (opcion == 6) {
                    responderSolicitudes(conversacion, usuario);
                } else if (opcion == 7) {
                    System.out.println("\n--- Mi calendario ---");
                    System.out.print(calendario.resumen(Integer.parseInt(usuario.getIdUsuario()), true));
                }
            } catch (RuntimeException e) {
                System.out.println("No se pudo completar la acción: " + e.getMessage());
            }
        }
    }

    private void solicitarReserva(Conversacion conversacion, Usuario usuario) {
        System.out.print("Fecha a reservar (aaaa-MM-dd, Enter para cancelar): ");
        String textoFecha = sc.nextLine().trim();
        if (textoFecha.isEmpty()) {
            return;
        }
        LocalDate fecha = CalendarioConsola.parsear(textoFecha);

        System.out.print("Hora (HH:mm, por ejemplo 15:00): ");
        LocalTime hora = parsearHora(sc.nextLine());

        calendario.solicitarReserva(idDe(usuario), idEstudiante(conversacion), idTutor(conversacion), fecha, hora);
        servicioChat.enviarTexto(conversacion, usuario,
                "[Calendario] Solicito reservar el " + fecha + " a las " + hora
                        + ". Responde desde \"Responder solicitudes de calendario\".");
        System.out.println("Solicitud enviada. Se reserva cuando la otra persona la acepte.");
    }

    private void solicitarEliminacion(Conversacion conversacion, Usuario usuario) {
        List<CalendarioDAO.Reserva> reservas =
                calendario.listarReservasDelPar(idEstudiante(conversacion), idTutor(conversacion));
        if (reservas.isEmpty()) {
            System.out.println("No hay fechas reservadas entre ustedes.");
            return;
        }

        for (int i = 0; i < reservas.size(); i++) {
            System.out.println((i + 1) + ". " + reservas.get(i).fecha + " a las " + reservas.get(i).hora);
        }
        int eleccion = leerEntero("Reserva a eliminar (0 para cancelar): ");
        if (eleccion < 1 || eleccion > reservas.size()) {
            return;
        }

        CalendarioDAO.Reserva reserva = reservas.get(eleccion - 1);
        calendario.solicitarEliminacion(idDe(usuario), reserva.id);
        servicioChat.enviarTexto(conversacion, usuario,
                "[Calendario] Solicito eliminar la reserva del " + reserva.fecha + " a las " + reserva.hora
                        + ". Responde desde \"Responder solicitudes de calendario\".");
        System.out.println("Solicitud enviada. Se elimina cuando la otra persona la acepte.");
    }

    private void responderSolicitudes(Conversacion conversacion, Usuario usuario) {
        int yo = idDe(usuario);
        List<CalendarioDAO.SolicitudCalendario> pendientes =
                calendario.pendientesDelPar(idEstudiante(conversacion), idTutor(conversacion));
        pendientes.removeIf(s -> s.idEmisor == yo);

        if (pendientes.isEmpty()) {
            System.out.println("No tienes solicitudes de calendario por responder en este chat.");
            return;
        }

        for (int i = 0; i < pendientes.size(); i++) {
            CalendarioDAO.SolicitudCalendario s = pendientes.get(i);
            System.out.println((i + 1) + ". " + s.descripcion() + " (enviada por " + s.nombreEmisor + ")");
        }
        int eleccion = leerEntero("Solicitud a responder (0 para cancelar): ");
        if (eleccion < 1 || eleccion > pendientes.size()) {
            return;
        }

        System.out.print("¿Aceptar? (s/n): ");
        boolean aceptar = sc.nextLine().trim().equalsIgnoreCase("s");

        String resultado = calendario.responder(pendientes.get(eleccion - 1).id, yo, aceptar);
        servicioChat.enviarTexto(conversacion, usuario, "[Calendario] " + resultado);
        System.out.println(resultado);
    }

    private void mostrarMensajes(Conversacion conversacion, Usuario usuario) {
        System.out.println("\n=== Chat con " + nombre(conversacion.getOtroParticipante(usuario)) + " ===");

        List<Mensaje> mensajes = conversacion.getMensajes();
        if (mensajes.isEmpty()) {
            System.out.println("(Todavía no hay mensajes)");
            return;
        }

        for (Mensaje m : mensajes) {
            boolean esMio = m.getRemitente().getIdUsuario().equals(usuario.getIdUsuario());
            String autor = esMio ? "Tú" : nombre(m.getRemitente());

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

    // resumen de lo que está pendiente de responder o esperando respuesta en este chat
    private void mostrarPendientes(Conversacion conversacion, Usuario usuario) {
        int yo = idDe(usuario);
        List<CalendarioDAO.SolicitudCalendario> pendientes =
                calendario.pendientesDelPar(idEstudiante(conversacion), idTutor(conversacion));
        if (pendientes.isEmpty()) {
            return;
        }

        System.out.println("\nSolicitudes de calendario pendientes:");
        for (CalendarioDAO.SolicitudCalendario s : pendientes) {
            System.out.println("  - " + s.descripcion() + (s.idEmisor == yo
                    ? " (esperando respuesta)" : " (te la envió " + s.nombreEmisor + ", respóndela con la opción 6)"));
        }
    }

    private int solicitudesPorResponder(Conversacion conversacion, Usuario usuario) {
        int yo = idDe(usuario);
        int total = 0;
        for (CalendarioDAO.SolicitudCalendario s
                : calendario.pendientesDelPar(idEstudiante(conversacion), idTutor(conversacion))) {
            if (s.idEmisor != yo) {
                total++;
            }
        }
        return total;
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    private LocalTime parsearHora(String texto) {
        try {
            return LocalTime.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("\"" + texto.trim() + "\" no es una hora válida (usa HH:mm).");
        }
    }

    private int idDe(Usuario usuario) {
        return Integer.parseInt(usuario.getIdUsuario());
    }

    private int idEstudiante(Conversacion conversacion) {
        return Integer.parseInt(conversacion.getEstudiante().getIdUsuario());
    }

    private int idTutor(Conversacion conversacion) {
        return Integer.parseInt(conversacion.getTutor().getIdUsuario());
    }

    private String nombre(Usuario usuario) {
        String nombre = usuario.getNombreCompleto();
        return (nombre == null || nombre.isBlank()) ? "(sin nombre)" : nombre;
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
