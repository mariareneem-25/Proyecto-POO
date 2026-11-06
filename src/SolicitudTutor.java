import java.util.ArrayList;
import java.util.List;

// información que llena un estudiante registrado como tutor para pedir su aprobación
// pasa por los estados BORRADOR (guardada, editable) -> PENDIENTE (enviada) -> APROBADO o RECHAZADO

public class SolicitudTutor {

    private final int idTutor;
    private String nombreCompleto;
    private String descripcion;
    private String materias;       // texto separado por comas: "Cálculo 1, Física 1"
    private String fotoPerfil;     // ruta o nombre de archivo (la subida de archivos se implementa después)
    private String banner;         // ruta o nombre de archivo (la subida de archivos se implementa después)
    private EstadoSolicitud estado;
    private Integer idRevisor;     // docente o director al que se envió la solicitud
    private String motivoRechazo;

    public SolicitudTutor(int idTutor) {
        this.idTutor = idTutor;
        this.estado = EstadoSolicitud.BORRADOR;
    }

    public SolicitudTutor(int idTutor, String nombreCompleto, String descripcion, String materias,
                          String fotoPerfil, String banner, EstadoSolicitud estado,
                          Integer idRevisor, String motivoRechazo) {
        this.idTutor = idTutor;
        this.nombreCompleto = nombreCompleto;
        this.descripcion = descripcion;
        this.materias = materias;
        this.fotoPerfil = fotoPerfil;
        this.banner = banner;
        this.estado = estado;
        this.idRevisor = idRevisor;
        this.motivoRechazo = motivoRechazo;
    }

    // la solicitud solo se puede enviar cuando todos los campos están llenos
    public boolean estaCompleta() {
        return camposFaltantes().isEmpty();
    }

    public List<String> camposFaltantes() {
        List<String> faltantes = new ArrayList<>();
        if (estaVacio(nombreCompleto)) faltantes.add("nombre completo");
        if (estaVacio(descripcion)) faltantes.add("descripción");
        if (estaVacio(materias)) faltantes.add("materias");
        if (estaVacio(fotoPerfil)) faltantes.add("foto de perfil");
        if (estaVacio(banner)) faltantes.add("banner");
        return faltantes;
    }

    private static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    // recibe "Cálculo 1, Física 1 ,, Programación" y lo deja limpio: "Cálculo 1, Física 1, Programación"
    public void setMateriasDesdeTexto(String texto) {
        if (texto == null) {
            this.materias = "";
            return;
        }
        List<String> limpias = new ArrayList<>();
        for (String materia : texto.split(",")) {
            if (!materia.isBlank()) {
                limpias.add(materia.trim());
            }
        }
        this.materias = String.join(", ", limpias);
    }

    public int getIdTutor() { return idTutor; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getDescripcion() { return descripcion; }
    public String getMaterias() { return materias; }
    public String getFotoPerfil() { return fotoPerfil; }
    public String getBanner() { return banner; }
    public EstadoSolicitud getEstado() { return estado; }
    public Integer getIdRevisor() { return idRevisor; }
    public String getMotivoRechazo() { return motivoRechazo; }

    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
    public void setBanner(String banner) { this.banner = banner; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
}
