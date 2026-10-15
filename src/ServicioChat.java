import java.util.List;
import java.util.Optional;

// lógica del chat entre estudiante y tutor
public class ServicioChat {

    private final ChatDAO chatDAO;
    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioChat(UsuarioRepositorio usuarioRepositorio) {
        this.chatDAO = new ChatDAO();
        this.usuarioRepositorio = usuarioRepositorio;
    }

    // si ya existe una conversación entre ambos, la devuelve; si no, la crea
    public Conversacion iniciarConversacion(Estudiante estudiante, Tutor tutor) {
        validarPuedenComunicarse(estudiante, tutor);

        Optional<Conversacion> existente = chatDAO.buscarPorParticipantes(
                estudiante.getIdUsuario(), tutor.getIdUsuario(), usuarioRepositorio);
        if (existente.isPresent()) {
            return existente.get();
        }

        Conversacion nueva = new Conversacion(estudiante, tutor);
        chatDAO.guardarConversacion(nueva);
        return nueva;
    }

    public List<Conversacion> conversacionesDe(Usuario usuario) {
        return chatDAO.listarDe(usuario.getIdUsuario(), usuarioRepositorio);
    }

    // vuelve a leer la conversación desde la base de datos (para ver mensajes nuevos de la otra persona)
    public Conversacion recargar(Conversacion conversacion) {
        return chatDAO.buscarPorId(conversacion.getId(), usuarioRepositorio).orElse(conversacion);
    }

    public Mensaje enviarTexto(Conversacion conversacion, Usuario remitente, String texto) {
        validarPuedeEnviar(conversacion, remitente);

        Mensaje mensaje = new Mensaje(remitente, texto);
        chatDAO.guardarMensaje(mensaje, conversacion.getId());
        conversacion.agregarMensaje(mensaje);
        return mensaje;
    }

    // FUTURO: enviarArchivo(...) para imágenes y archivos
    // FUTURO: llamadas y videollamadas

    // ---------------------------------------------------------------
    // Validaciones y puntos abiertos para el futuro

    private void validarPuedenComunicarse(Estudiante estudiante, Tutor tutor) {
        if (!puedeUsarChat(estudiante) || !puedeUsarChat(tutor)) {
            throw new IllegalStateException("Alguno de los usuarios todavía no puede usar el chat");
        }
        if (!estanAgendados(estudiante, tutor)) {
            throw new IllegalStateException("Aún no tienen una tutoría agendada entre ustedes");
        }
    }

    private void validarPuedeEnviar(Conversacion conversacion, Usuario remitente) {
        if (!conversacion.esParticipante(remitente)) {
            throw new IllegalArgumentException("No participas en esta conversación");
        }
        validarPuedenComunicarse(conversacion.getEstudiante(), conversacion.getTutor());
    }

    // PENDIENTE: verificar que el estudiante y el tutor ya tengan una tutoría agendada.
    // Cuando exista el módulo de agendamiento, consultar aquí y devolver false si no hay una.
    protected boolean estanAgendados(Estudiante estudiante, Tutor tutor) {
        return true;
    }

    // PENDIENTE: cuando exista el flujo de aprobación de tutores, bloquear a los no aprobados:
    // if (usuario instanceof Tutor) { return ((Tutor) usuario).estaActivo(); }
    protected boolean puedeUsarChat(Usuario usuario) {
        return true;
    }
}
