public abstract class Usuario {

    private final String idUsuario;
    private final String nombreCompleto;
    private final String correo;
    private String fotoPerfilPath ;

    public Usuario(String idUsuario, String nombreCompleto, String correo, String contrasenaHash) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
    
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public String getFotoPerfilPath() {
        return fotoPerfilPath;
    }

    public void setFotoPerfilPath(String fotoPerfilPath) {
        this.fotoPerfilPath = fotoPerfilPath;
    }

    // como default, ningún usuario puede publicar de forma anónima
    // como solo los estudiantes pueden, la clase Estudiante sobrescribe este método para devolver true

    public boolean puedeSerAnonimo() {
        return false;
    }

    // permisos del foro académico
    // por defecto un usuario solo puede comentar; Estudiante sobrescribe puedePublicar() para poder publicar
    // así Tutor y Docente pueden comentar pero no publicar

    public boolean puedePublicar() {
        return false;
    }

    public boolean puedeComentar() {
        return true;
    }
}