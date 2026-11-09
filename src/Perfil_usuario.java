import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Locale;

public class Perfil_usuario {

    public enum TipoUsuario {
        ESTUDIANTE, TUTOR, DIRECTOR, DOCENTE
    }

    public enum EstadoCuenta {
        ACTIVA, SUSPENDIDA, ELIMINADA
    }

    private int idUsuario;
    private String nombre;
    private String correoElectronico;
    private String contrasena;
    private String fotoPerfil;
    private String descripcion;
    private String banner;
    private TipoUsuario tipoUsuario;
    private EstadoCuenta estadoCuenta;

    private boolean sesionActiva = false;

    public Perfil_usuario() {
    }
    
    private Perfil_usuario(int idUsuario, String nombre, String correoElectronico, String contrasena,
                       String fotoPerfil, String descripcion, String banner,
                       TipoUsuario tipoUsuario, EstadoCuenta estadoCuenta) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.correoElectronico = correoElectronico;
        this.contrasena = contrasena;
        this.fotoPerfil = fotoPerfil;
        this.descripcion = descripcion;
        this.banner = banner;
        this.tipoUsuario = tipoUsuario;
        this.estadoCuenta = estadoCuenta;
    }

    public int getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getCorreoElectronico() { return correoElectronico; }
    public String getFotoPerfil() { return fotoPerfil; }
    public String getDescripcion() { return descripcion; }
    public String getBanner() { return banner; }
    public TipoUsuario getTipoUsuario() { return tipoUsuario; }
    public EstadoCuenta getEstadoCuenta() { return estadoCuenta; }
    public boolean isSesionActiva() { return sesionActiva; }

    

    public static boolean verificarCorreo(String correo) {
        if (!Seguridad.esCorreoInstitucional(correo)) return false;
        String sql = "SELECT 1 FROM usuarios WHERE lower(correoElectronico) = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Error al verificar correo: " + e.getMessage());
            return false;
        }
    }

    

    public static TipoUsuario seleccionarRolEstudiante(Scanner sc) {
        int opcion = -1;
        while (opcion != 1 && opcion != 2) {
            System.out.println("1. Estudiante (solicita tutorías)");
            System.out.println("2. Tutor (brinda tutorías)");
            System.out.print("Selecciona una opción: ");
            try { opcion = Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { opcion = -1; }
        }
        return opcion == 2 ? TipoUsuario.TUTOR : TipoUsuario.ESTUDIANTE;
    }

    public static Perfil_usuario crearUsuario(String nombre, String correo, String contrasena, String fotoPerfil, TipoUsuario rolElegido) {

        if (!verificarCorreo(correo)) {
            rechazarRegistro();
            return null;
        }

        // El registro público solo permite estudiante o tutor.
        if (rolElegido != TipoUsuario.ESTUDIANTE && rolElegido != TipoUsuario.TUTOR) {
            rechazarRegistro();
            return null;
        }
        TipoUsuario tipo = rolElegido;

        if (!Seguridad.contrasenaValida(contrasena)) {
            System.out.println(Seguridad.requisitosContrasena());
            return null;
        }
        Perfil_usuario usuario = new Perfil_usuario(
            0,
            nombre,
            correo.trim().toLowerCase(Locale.ROOT),
            Seguridad.hashContrasena(contrasena),
            fotoPerfil,
            "",
            "",
            tipo,
            EstadoCuenta.ACTIVA
        );

        if (!usuario.guardarEnBD()) {
            return null;
        }

        return usuario;
    }

    public static void rechazarRegistro() { System.out.println("Registro rechazado: utiliza un correo @uvg.edu.gt que no esté registrado."); }

    public static Perfil_usuario iniciarSesion(String correo, String contrasena) {
        if (!Seguridad.esCorreoInstitucional(correo)) {
            return null;
        }

        String sql = "SELECT * FROM usuarios WHERE correoElectronico = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, correo.trim().toLowerCase(Locale.ROOT));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Correo o contraseña incorrectos.");
                    return null;
                }

                String guardada = rs.getString("contrasena");
                if (!Seguridad.verificarContrasena(contrasena, guardada)) {
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
                    rs.getString("descripcion"),
                    rs.getString("banner"),
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
    
    // ---------------------------------------------------------------
    // Consultas usadas por el chat y la validación de tutores
    // ---------------------------------------------------------------

    private static Perfil_usuario desdeResultSet(ResultSet rs) throws SQLException {
        return new Perfil_usuario(
            rs.getInt("idUsuario"),
            rs.getString("nombre"),
            rs.getString("correoElectronico"),
            rs.getString("contrasena"),
            rs.getString("fotoPerfil"),
            rs.getString("descripcion"),
            rs.getString("banner"),
            TipoUsuario.valueOf(rs.getString("tipoUsuario")),
            EstadoCuenta.valueOf(rs.getString("estadoCuenta"))
        );
    }

    public static Perfil_usuario buscarPorId(int id) {
        String sql = "SELECT * FROM usuarios WHERE idUsuario = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return desdeResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar el usuario: " + e.getMessage());
        }
        return null;
    }

    public static List<Perfil_usuario> listarPorTipo(TipoUsuario tipo) {
        List<Perfil_usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE tipoUsuario = ? AND estadoCuenta = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, tipo.name());
            ps.setString(2, EstadoCuenta.ACTIVA.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(desdeResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al listar usuarios: " + e.getMessage());
        }
        return lista;
    }

    public static List<Perfil_usuario> listarTutoresPendientes() {
        List<Perfil_usuario> lista = new ArrayList<>();
        String sql = "SELECT u.* FROM usuarios u "
                + "LEFT JOIN solicitudes_tutor s ON s.idTutor = u.idUsuario "
                + "WHERE u.tipoUsuario = ? AND u.estadoCuenta = ? "
                + "AND (s.estado IS NULL OR s.estado = ?)";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, TipoUsuario.TUTOR.name());
            ps.setString(2, EstadoCuenta.ACTIVA.name());
            ps.setString(3, EstadoSolicitud.PENDIENTE.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(desdeResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al listar solicitudes de tutor: " + e.getMessage());
        }
        return lista;
    }

    // si el tutor no tiene registro todavía, su solicitud está PENDIENTE
    public static EstadoSolicitud obtenerEstadoSolicitudTutor(int idTutor) {
        String sql = "SELECT estado FROM solicitudes_tutor WHERE idTutor = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idTutor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return EstadoSolicitud.valueOf(rs.getString("estado"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener el estado de la solicitud: " + e.getMessage());
        }
        return EstadoSolicitud.PENDIENTE;
    }

    public static boolean guardarEstadoSolicitudTutor(int idTutor, EstadoSolicitud estado, int idAprobador) {
        String sql = "INSERT OR REPLACE INTO solicitudes_tutor (idTutor, estado, aprobadoPor) VALUES (?, ?, ?)";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, idTutor);
            ps.setString(2, estado.name());
            ps.setInt(3, idAprobador);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al guardar la solicitud: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------

    public void cerrarSesion() {
        this.sesionActiva = false;
    }

    //actualizan un solo dato y deja los demas como estaban 

    public boolean actualizarSoloNombre(String nuevoNombre) {
       return actualizarPerfil(nuevoNombre, null, this.fotoPerfil);
    }
    public boolean actualizarSoloContrasena(String nuevaContrasena) {
        return actualizarPerfil(this.nombre, nuevaContrasena, this.fotoPerfil);
    }
    public boolean actualizarSoloFoto(String nuevaFotoPerfil) {
        return actualizarPerfil(this.nombre, null, nuevaFotoPerfil);
    }

    public boolean actualizarPerfil(String nuevoNombre, String nuevaContrasena, String nuevaFotoPerfil) {
        String sql = "UPDATE usuarios SET nombre = ?, contrasena = COALESCE(?, contrasena), fotoPerfil = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, nuevoNombre);
            ps.setString(2, nuevaContrasena == null ? null : Seguridad.hashContrasena(nuevaContrasena));
            ps.setString(3, nuevaFotoPerfil);
            ps.setInt(4, this.idUsuario);

            int filas = ps.executeUpdate();
            if (filas > 0) {
                this.nombre = nuevoNombre;
                this.contrasena = "[protegida]";
                this.fotoPerfil = nuevaFotoPerfil;
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.out.println("Error al actualizar el perfil: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarInformacionPerfil(String nuevaDescripcion, String nuevoBanner) {

    String sql = "UPDATE usuarios SET descripcion = ?, banner = ? WHERE idUsuario = ?";

    try (Connection conexion = ConexionDB.conectar();
         PreparedStatement ps = conexion.prepareStatement(sql)) {

        ps.setString(1, nuevaDescripcion);
        ps.setString(2, nuevoBanner);
        ps.setInt(3, this.idUsuario);

        int filas = ps.executeUpdate();

        if (filas > 0) {
            this.descripcion = nuevaDescripcion;
            this.banner = nuevoBanner;
            return true;
        }

        return false;

    } catch (SQLException e) {
        System.out.println(
                "Error al actualizar descripcion y banner: " + e.getMessage()
        );
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

    // vuelve a activar una cuenta que el usuario elimino 
    // pide el correo y la contrasena 
    // una cuenta SUSPENDIDA no se puede reactivar desde aqui

    public static boolean reactivarCuenta(String correo, String contrasena) {
        if (!Seguridad.esCorreoInstitucional(correo)) {
            System.out.println("Correo o contraseña incorrectos.");
            return false;
        }

        String consulta = "SELECT idUsuario, estadoCuenta, contrasena FROM usuarios WHERE correoElectronico = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(consulta)) {

            ps.setString(1, correo.trim().toLowerCase(Locale.ROOT));
            
            int idUsuario;
            EstadoCuenta estado;
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !Seguridad.verificarContrasena(contrasena, rs.getString("contrasena"))) {
                    System.out.println("Correo o contraseña incorrectos.");
                    return false;
                }
                idUsuario = rs.getInt("idUsuario");
                estado = EstadoCuenta.valueOf(rs.getString("estadoCuenta"));
            }

            if (estado == EstadoCuenta.ACTIVA) {
                System.out.println("Tu cuenta ya está activa. Puedes iniciar sesión.");
                return false;
            }
            if (estado == EstadoCuenta.SUSPENDIDA) {
                System.out.println("Tu cuenta está suspendida y no se puede reactivar desde aquí.");
                return false;
            }

            String actualizar = "UPDATE usuarios SET estadoCuenta = ? WHERE idUsuario = ?";
            try (PreparedStatement ps2 = conexion.prepareStatement(actualizar)) {
                ps2.setString(1, EstadoCuenta.ACTIVA.name());
                ps2.setInt(2, idUsuario);
                return ps2.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            System.out.println("Error al reactivar la cuenta: " + e.getMessage());
            return false;
        }
    }

    private boolean guardarEnBD() {
        String sql = "INSERT INTO usuarios "
                + "(nombre, correoElectronico, contrasena, fotoPerfil, descripcion, banner, tipoUsuario, estadoCuenta) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, nombre);
            ps.setString(2, correoElectronico);
            ps.setString(3, contrasena);
            ps.setString(4, fotoPerfil);
            ps.setString(5, descripcion);
            ps.setString(6, banner);
            ps.setString(7, tipoUsuario.name());
            ps.setString(8, estadoCuenta.name());

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
