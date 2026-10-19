public class Mensaje {

    private int idMensaje;
    private int idSolicitud;
    private int idEmisor;
    private String nombreEmisor;
    private String texto;
    private String fecha;

    public Mensaje(int idMensaje, int idSolicitud, int idEmisor, String nombreEmisor, String texto, String fecha) {
        this.idMensaje = idMensaje;
        this.idSolicitud = idSolicitud;
        this.idEmisor = idEmisor;
        this.nombreEmisor = nombreEmisor;
        this.texto = texto;
        this.fecha = fecha;
    }

    public int getIdMensaje() { return idMensaje; }
    public int getIdSolicitud() { return idSolicitud; }
    public int getIdEmisor() { return idEmisor; }
    public String getNombreEmisor() { return nombreEmisor; }
    public String getTexto() { return texto; }
    public String getFecha() { return fecha; }

    @Override
    public String toString() {
        return "[" + fecha + "] " + nombreEmisor + ": " + texto;
    }
}