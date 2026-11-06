/*nueva clase de tutor con los cambios, 
-No permite al tutor llenar su perfil como tutor antes de que se mande su solicitud al rector
-Se necesita un botón que confirme que el tutor quiere enviar la solicitud o solo guardar cambios
-Cuenta  aparece en estado ACTIVO en vez de PENDIENTE, eso aparece en la clase EstadoSolicitud */ 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Tutor extends Usuario {

    private String carrera;
    private int anualidadCarrera;
    private String presentacion;

    private EstadoSolicitud estadoSolicitud;

    private final List<String> materiasQueDicta;
    private final List<Horario> horariosDisponibles;
    private String fotoMismo; //se recibe la ruta o el URL de la imagen del tutor, perfil
    private String banner; // ruta o url de imgen de portada 
    private Perfil_usuario aprobadoPor; //usuario de tipo Perfil_Usuario que aprobó o rechazó la solicitud del tutor

    // Un tutor nuevo todavía no ha enviado su solicitud.
    public Tutor(
            String idUsuario,
            String nombreCompleto,
            String correo,
            String contrasenaHash) {

        this(
                idUsuario,
                nombreCompleto,
                correo,
                contrasenaHash,
                EstadoSolicitud.BORRADOR
        );
    }

    // Permite reconstruir un tutor con su estado guardado.
    public Tutor(
            String idUsuario,
            String nombreCompleto,
            String correo,
            String contrasenaHash,
            EstadoSolicitud estadoSolicitud) {

        super(
                idUsuario,
                nombreCompleto,
                normalizarCorreoInstitucional(correo),
                contrasenaHash
        );

        if (estadoSolicitud == null) {
            throw new IllegalArgumentException(
                    "El estado de la solicitud es obligatorio."
            );
        }

        this.carrera = "";
        this.anualidadCarrera = 0;
        this.presentacion = "";
        this.fotoMismo = "";
        this.banner = "";

        this.estadoSolicitud = estadoSolicitud;
        this.aprobadoPor = null;

        this.materiasQueDicta = new ArrayList<>();
        this.horariosDisponibles = new ArrayList<>();
    }

    private static String normalizarCorreoInstitucional(String correo) {
        if (correo == null) {
            throw new IllegalArgumentException(
                    "El correo institucional es obligatorio."
            );
        }

        String normalizado =
                correo.trim().toLowerCase(Locale.ROOT);

        if (!normalizado.matches("[^\\s@]+@uvg\\.edu\\.gt")) {
            throw new IllegalArgumentException(
                    "Debes ingresar un correo válido con @uvg.edu.gt."
            );
        }

        return normalizado;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera == null ? "" : carrera.trim();
    }

    public int getAnualidadCarrera() {
        return anualidadCarrera;
    }

    public void setAnualidadCarrera(int anualidadCarrera) {
        if (anualidadCarrera < 0) {
            throw new IllegalArgumentException(
                    "El año de carrera no puede ser negativo."
            );
        }

        // Cero representa un dato que todavía falta completar.
        this.anualidadCarrera = anualidadCarrera;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion == null
                ? ""
                : presentacion.trim();
    }
//se presenta la descripcion y la presentacion del mismo dato
    public String getDescripcionTutor(){
        return presentacion;
    }

    public void setDescriptionTutor(String descripcionTutor)
    {
        setPresentacion(descripcionTutor);
    }
    public String getFotoMismo() {
        return fotoMismo;
    }
    public void setFotoMismo(String fotoMismo){
        this.fotoMismo=fotoMismo==null?"":fotoMismo.trim();
    }
    public String getBanner() {
        return banner;
    } //se recibe la ruta o el URL de la imagen del tutor

    public EstadoSolicitud getEstadoSolicitud() {
        return estadoSolicitud;
    }

    public Perfil_usuario getAprobadoPor() {
        return aprobadoPor;
    }

    public boolean estaActivo() {
        return estadoSolicitud == EstadoSolicitud.APROBADO;
    }
    
    public boolean esBorrador(){
        return estadoSolicitud == EstadoSolicitud.BORRADOR;
    }

    public boolean puedeEditarPerfil() {
        return estadoSolicitud == EstadoSolicitud.BORRADOR
                || estadoSolicitud == EstadoSolicitud.RECHAZADO;
    }

    public List<String> getMateriasQueDicta() {
        return Collections.unmodifiableList(materiasQueDicta);
    }

    public void agregarMateria(String materia) {
        if (materia == null || materia.isBlank()) {
            throw new IllegalArgumentException(
                    "La materia no puede estar vacía."
            );
        }

        String materiaLimpia = materia.trim();

        boolean existe = materiasQueDicta.stream()
                .anyMatch(
                        actual -> actual.equalsIgnoreCase(materiaLimpia)
                );

        if (!existe) {
            materiasQueDicta.add(materiaLimpia);
        }
    }

    public void limpiarMaterias() {
        materiasQueDicta.clear();
    }

    // Conserva el nombre utilizado por AgendaTutorias.
    public List<Horario> getHorarioDisponibles() {
        return Collections.unmodifiableList(horariosDisponibles);
    }

    public List<Horario> getHorariosDisponibles() {
        return getHorarioDisponibles();
    }

    public void agregarHorario(Horario horario) {
        if (horario == null) {
            throw new IllegalArgumentException(
                    "El horario no puede ser nulo."
            );
        }

        if (horario.getDia() == null
                || horario.getHoraInicio() == null
                || horario.getHoraFin() == null) {

            throw new IllegalArgumentException(
                    "El horario necesita día, hora inicial y hora final."
            );
        }

        if (!horario.getHoraInicio().isBefore(horario.getHoraFin())) {
            throw new IllegalArgumentException(
                    "La hora inicial debe ser anterior a la hora final."
            );
        }

        boolean existe = horariosDisponibles.stream()
                .anyMatch(actual ->
                        actual.getDia() == horario.getDia()
                        && actual.getHoraInicio().equals(
                                horario.getHoraInicio()
                        )
                        && actual.getHoraFin().equals(
                                horario.getHoraFin()
                        )
                );

        if (!existe) {
            horariosDisponibles.add(horario);
        }
    }

    public void limpiarHorarios() {
        horariosDisponibles.clear();
    }

    public List<String> camposPendientes() {
        List<String> pendientes = new ArrayList<>();

        if (getNombreCompleto() == null
                || getNombreCompleto().isBlank()) {
            pendientes.add("nombre completo");
        }

        if (getCorreo() == null || getCorreo().isBlank()) {
            pendientes.add("correo institucional");
        }

        if (carrera.isBlank()) {
            pendientes.add("carrera");
        }

        if (anualidadCarrera < 1) {
            pendientes.add("año de carrera mayor a cero");
        }

        if (presentacion.isBlank()) {
            pendientes.add("presentación del tutor");
        }
        if (fotoMismo.isBlank()){
            pendientes.add("foto del Tutor");
        }
        if (banner.isBlank()){
            pendientes.add("banner del Tutor");

        }

        if (materiasQueDicta.isEmpty()) {
            pendientes.add("al menos una materia");
        }

        if (horariosDisponibles.isEmpty()) {
            pendientes.add("al menos un horario disponible");
        }

        return pendientes;
    }

    public boolean perfilCompleto() {
        return camposPendientes().isEmpty();
    }

   
    //public void guardarCambios() {
        //validarEdicionPermitida();
        //new TutorDAO().guardar(this, false);
    //}
    public void guardarCambios(){
        validarEdicionPermitida();
        guardarConEstado(EstadoSolicitud.BORRADOR,false);
    }

     // Guarda incluso un perfil incompleto.
    // No crea ni envía una solicitud al rector.

    // Guarda el perfil completo y envía la solicitud.
    public void enviarSolicitud() {
        validarEdicionPermitida();

        List<String> pendientes = camposPendientes();

        if (!pendientes.isEmpty()) {
            throw new IllegalStateException(
                    "Antes de enviar la solicitud debes completar: "
                    + String.join(", ", pendientes)
            );
        }

        //new TutorDAO().guardar(this, true);

        // Cambia el estado solo después de guardar correctamente.
        //estadoSolicitud = EstadoSolicitud.PENDIENTE;
        guardarConEstado(EstadoSolicitud.PENDIENTE,true);
    }

    // El menú puede pasar aquí la respuesta del usuario.
    // Si cancela, no se guarda ni se envía nada.
    public boolean enviarSolicitud(boolean confirmado) {
        if (!confirmado) {
            return false;
        }

        enviarSolicitud();
        return true;
    }
    private void guardarConEstado(EstadoSolicitud nuevoEstado, boolean enviarSolicitud) {
        
        EstadoSolicitud estadoAnterior=estadoSolicitud;
        Perfil_usuario aprobadorAnterior=aprobadoPor; 
        
        estadoSolicitud= nuevoEstado;
        aprobadoPor= null; 
        try{
            new TutorDAO().guardar(this, enviarSolicitud);
        }catch(RuntimeException e){
            estadoSolicitud=estadoAnterior;
            aprobadoPor=aprobadorAnterior; 
            throw e;
           }
    }

    public void aprobarSolicitud(Perfil_usuario rector) {
        validarRector(rector);

        if (estadoSolicitud != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se puede aprobar una solicitud enviada y pendiente."
            );
        }

        boolean guardado =
                Perfil_usuario.guardarEstadoSolicitudTutor(
                        obtenerIdNumerico(),
                        EstadoSolicitud.APROBADO,
                        rector.getIdUsuario()
                );

        if (!guardado) {
            throw new IllegalStateException(
                    "No se pudo guardar la aprobación."
            );
        }

        estadoSolicitud = EstadoSolicitud.APROBADO;
        aprobadoPor=rector;
    }

    public void rechazarSolicitud(Perfil_usuario rector) {
        validarRector(rector);

        if (estadoSolicitud != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se puede rechazar una solicitud enviada y pendiente."
            );
        }

        boolean guardado =
                Perfil_usuario.guardarEstadoSolicitudTutor(
                        obtenerIdNumerico(),
                        EstadoSolicitud.RECHAZADO,
                        rector.getIdUsuario()
                );

        if (!guardado) {
            throw new IllegalStateException(
                    "No se pudo guardar el rechazo."
            );
        }

        // Después del rechazo puede corregir el perfil y reenviar.
        estadoSolicitud = EstadoSolicitud.RECHAZADO;
        aprobadoPor=rector;
    }

    private void validarEdicionPermitida() {
        if (!puedeEditarPerfil()) {
            throw new IllegalStateException(
                    "La solicitud ya fue enviada o aprobada. "
                    + "No se pueden guardar cambios ni volver a enviarla."
            );
        }
    }

    private void validarRector(Perfil_usuario rector) {
        if (rector == null
                || rector.getTipoUsuario()
                    != Perfil_usuario.TipoUsuario.DIRECTOR
                || rector.getEstadoCuenta()
                    != Perfil_usuario.EstadoCuenta.ACTIVA) {

            throw new IllegalArgumentException(
                    "Solo el rector, con rol DIRECTOR y cuenta activa, "
                    + "puede aprobar o rechazar solicitudes."
            );
        }
    }

    private int obtenerIdNumerico() {
        try {
            return Integer.parseInt(getIdUsuario());

        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "El tutor necesita un identificador numérico válido.",
                    e
            );
        }
    }

    @Override
    public boolean puedeSerAnonimo() {
        return false;
    }

}
