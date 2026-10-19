import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
 
public class ConexionDB {
    private static final String URL = "jdbc:sqlite:proyecto.db";
 
    public static Connection conectar() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Error al conectar con SQLite: " + e.getMessage());
        }
        return conexion;
    }
 
    public static void crearTablas() {
        String sqlCorreos = "CREATE TABLE IF NOT EXISTS correos_autorizados (" +
                "correo TEXT PRIMARY KEY, " +
                "tipo TEXT NOT NULL)";
     
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "idUsuario INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "correoElectronico TEXT NOT NULL UNIQUE, " +
                "contrasena TEXT NOT NULL, " +
                "fotoPerfil TEXT, " +
                "descripcion TEXT, " +
                "banner TEXT, " +
                "tipoUsuario TEXT NOT NULL, " +
                "estadoCuenta TEXT NOT NULL)";
     
        String sqlSolicitudesTutor = "CREATE TABLE IF NOT EXISTS solicitudes_tutor (" +
                "idTutor INTEGER PRIMARY KEY, " +
                "estado TEXT NOT NULL, " +
                "aprobadoPor INTEGER)";
 
        String sqlSolicitudes = "CREATE TABLE IF NOT EXISTS solicitudes (" +
                "idSolicitud INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idEstudiante INTEGER NOT NULL, " +
                "idTutor INTEGER NOT NULL, " +
                "materia TEXT NOT NULL, " +
                "mensaje TEXT NOT NULL, " +
                "estado TEXT NOT NULL, " +
                "motivoRechazo TEXT)";

        String sqlMensajes = "CREATE TABLE IF NOT EXISTS mensajesTutoria (" +
                "idMensaje INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idSolicitud INTEGER NOT NULL, " +
                "idEmisor INTEGER NOT NULL, " +
                "texto TEXT NOT NULL, " +
                "fecha TEXT NOT NULL)";

        String sqlPropuestas = "CREATE TABLE IF NOT EXISTS propuestasFecha (" +
                "idPropuesta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idSolicitud INTEGER NOT NULL, " +
                "idProponente INTEGER NOT NULL, " +
                "fechaHora TEXT NOT NULL, " +
                "estado TEXT NOT NULL)";

        String sqlLogros = "CREATE TABLE IF NOT EXISTS logros (" +
                "idLogro INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idUsuario INTEGER NOT NULL, " +
                "tema TEXT NOT NULL, " +
                "completado INTEGER NOT NULL)";

        try (Connection conexion = conectar();
             Statement st = conexion.createStatement()) {
 
            st.execute(sqlCorreos);
            st.execute(sqlUsuarios);
            st.execute(sqlSolicitudesTutor);
            st.execute(sqlSolicitudes);
            st.execute(sqlMensajes);
            st.execute(sqlPropuestas);
            st.execute(sqlLogros);
        
         try { 
          st.execute("ALTER TABLE usuarios ADD COLUMN descripcion TEXT");
         } catch (SQLException ignored) {
          
         }
         try {
          st.execute("ALTER TABLE usuarios ADD COLUMN banner TEXT");
         } catch (SQLException ignored) {
          
         }
 
        } catch (SQLException e) {
            System.out.println("Error al crear las tablas: " + e.getMessage());
        }
    }

    public static boolean existe(String sql, Object... parametros) {
        try (Connection conexion = conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Error en la base de datos: " + e.getMessage());
        }
    }
}
