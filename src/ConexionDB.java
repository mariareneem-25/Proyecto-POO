import java.sql.Connection;
import java.sql.DriverManager;
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
                "tipoUsuario TEXT NOT NULL, " +
                "estadoCuenta TEXT NOT NULL)";
 
        try (Connection conexion = conectar();
             Statement st = conexion.createStatement()) {
 
            st.execute(sqlCorreos);
            st.execute(sqlUsuarios);
 
        } catch (SQLException e) {
            System.out.println("Error al crear las tablas: " + e.getMessage());
        }
    }
}
 