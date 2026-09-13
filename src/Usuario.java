public abstract class Usuario {

    private String idUsuario;
    private String nombreCompleto;
    private String correo;
    private String contrasenaHash;
    private String fotoPerfilPath;

    public Usuario(String idUsuario, String nombreCompleto, String correo, String contrasenaHash) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
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
}