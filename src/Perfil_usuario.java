import java.sql.*;
import java.util.Scanner;

public class Perfil_usuario {

    public enum TipoUsuario {
        ESTUDIANTE, TUTOR, DIRECTOR
    }

    public enum EstadoCuenta {
        ACTIVA, SUSPENDIDA, ELIMINADA
    }

    private int idUsuario;
    private String nombre;
    private String correoElectronico;
    private String contrasena;
    private String fotoPerfil;
    private TipoUsuario tipoUsuario;
    private EstadoCuenta estadoCuenta;

    private boolean sesionActiva = false;

    public Perfil_usuario() {
    }

    private Perfil_usuario(int idUsuario, String nombre, String correoElectronico, String contrasena,
                            String fotoPerfil, TipoUsuario tipoUsuario, EstadoCuenta estadoCuenta) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.correoElectronico = correoElectronico;
        this.contrasena = contrasena;
        this.fotoPerfil = fotoPerfil;
        this.tipoUsuario = tipoUsuario;
        this.estadoCuenta = estadoCuenta;
    }

    public int getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getCorreoElectronico() { return correoElectronico; }
    public String getFotoPerfil() { return fotoPerfil; }
    public TipoUsuario getTipoUsuario() { return tipoUsuario; }
    public EstadoCuenta getEstadoCuenta() { return estadoCuenta; }
    public boolean isSesionActiva() { return sesionActiva; }

    public static boolean registrarCorreo(String correo, String tipo) {
        String sql = "INSERT INTO correos_autorizados (correo, tipo) VALUES (?, ?)";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, correo.toLowerCase());
            ps.setString(2, tipo.toLowerCase());
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al registrar correo: " + e.getMessage());
            return false;
        }
    }

    public static boolean verificarCorreo(String correo) {
        if (correo == null || !correo.toLowerCase().endsWith("@uvg.edu.gt")) {
            return false;
        }

        String sqlAutorizado = "SELECT 1 FROM correos_autorizados WHERE correo = ?";
        String sqlYaUsado = "SELECT 1 FROM usuarios WHERE correoElectronico = ?";

        try (Connection conexion = ConexionDB.conectar()) {

            try (PreparedStatement ps = conexion.prepareStatement(sqlAutorizado)) {
                ps.setString(1, correo.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                }
            }

            try (PreparedStatement ps = conexion.prepareStatement(sqlYaUsado)) {
                ps.setString(1, correo.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return false;
                }
            }

            return true;

        } catch (SQLException e) {
            System.out.println("Error al verificar correo: " + e.getMessage());
            return false;
        }
    }

    public static String obtenerTipoUsuario(String correo) {
        String sql = "SELECT tipo FROM correos_autorizados WHERE correo = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, correo.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("tipo");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener el tipo de usuario: " + e.getMessage());
        }
        return null;
    }

    public static TipoUsuario seleccionarRolEstudiante() {
        Scanner sc = new Scanner(System.in);
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
            }
        }

        return (opcion == 2) ? TipoUsuario.TUTOR : TipoUsuario.ESTUDIANTE;
    }

    public static Perfil_usuario crearUsuario(String nombre, String correo, String contrasena, String fotoPerfil) {

        if (!verificarCorreo(correo)) {
            rechazarRegistro();
            return null;
        }

        String tipoTexto = obtenerTipoUsuario(correo);
        if (tipoTexto == null) {
            rechazarRegistro();
            return null;
        }

        TipoUsuario tipo;
        switch (tipoTexto.toLowerCase()) {
            case "estudiante":
                tipo = seleccionarRolEstudiante();
                break;
            case "director":
                tipo = TipoUsuario.DIRECTOR;
                break;
            default:
                rechazarRegistro();
                return null;
        }

        Perfil_usuario usuario = new Perfil_usuario(0, nombre, correo.toLowerCase(), contrasena,
                fotoPerfil, tipo, EstadoCuenta.ACTIVA);

        if (!usuario.guardarEnBD()) {
            return null;
        }

        return usuario;
    }

    public static void rechazarRegistro() {
        System.out.println("Registro rechazado: el correo ingresado no pertenece al dominio institucional uvg, "
                + "no se encuentra autorizado o ya fue utilizado para crear una cuenta.");
    }

    public static Perfil_usuario iniciarSesion(String correo, String contrasena) {
        String sql = "SELECT * FROM usuarios WHERE correoElectronico = ? AND contrasena = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, correo.toLowerCase());
            ps.setString(2, contrasena);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Correo o contraseña incorrectos.");
                    return null;
                }

                EstadoCuenta estado = EstadoCuenta.valueOf(rs.getString("estadoCuenta"));
                if (estado != EstadoCuenta.ACTIVA) {
                    System.out.println("La cuenta no está activa (estado: " + estado + ").");
                    return null;
                }

                Perfil_usuario usuario = new Perfil_usuario(
                        rs.getInt("idUsuario"),
                        rs.getString("nombre"),
                        rs.getString("correoElectronico"),
                        rs.getString("contrasena"),
                        rs.getString("fotoPerfil"),
                        TipoUsuario.valueOf(rs.getString("tipoUsuario")),
                        estado
                );
                usuario.sesionActiva = true;
                return usuario;
            }

        } catch (SQLException e) {
            System.out.println("Error al iniciar sesión: " + e.getMessage());
            return null;
        }
    }

    public void cerrarSesion() {
        this.sesionActiva = false;
    }

    public boolean actualizarPerfil(String nuevoNombre, String nuevaContrasena, String nuevaFotoPerfil) {
        String sql = "UPDATE usuarios SET nombre = ?, contrasena = ?, fotoPerfil = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, nuevoNombre);
            ps.setString(2, nuevaContrasena);
            ps.setString(3, nuevaFotoPerfil);
            ps.setInt(4, this.idUsuario);

            int filas = ps.executeUpdate();
            if (filas > 0) {
                this.nombre = nuevoNombre;
                this.contrasena = nuevaContrasena;
                this.fotoPerfil = nuevaFotoPerfil;
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.out.println("Error al actualizar el perfil: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarPerfil() {
        String sql = "UPDATE usuarios SET estadoCuenta = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, EstadoCuenta.ELIMINADA.name());
            ps.setInt(2, this.idUsuario);

            int filas = ps.executeUpdate();
            if (filas > 0) {
                this.estadoCuenta = EstadoCuenta.ELIMINADA;
                this.sesionActiva = false;
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.out.println("Error al eliminar el perfil: " + e.getMessage());
            return false;
        }
    }

    private boolean guardarEnBD() {
        String sql = "INSERT INTO usuarios (nombre, correoElectronico, contrasena, fotoPerfil, tipoUsuario, estadoCuenta) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nombre);
            ps.setString(2, correoElectronico);
            ps.setString(3, contrasena);
            ps.setString(4, fotoPerfil);
            ps.setString(5, tipoUsuario.name());
            ps.setString(6, estadoCuenta.name());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    this.idUsuario = rs.getInt(1);
                }
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Error al guardar el usuario: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String toString() {
        return "Perfil_usuario{" +
                "idUsuario=" + idUsuario +
                ", nombre='" + nombre + '\'' +
                ", correoElectronico='" + correoElectronico + '\'' +
                ", tipoUsuario=" + tipoUsuario +
                ", estadoCuenta=" + estadoCuenta +
                '}';
    }
}