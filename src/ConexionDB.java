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
        String sqlTutorias = "CREATE TABLE IF NOT EXISTS tutorias (" +
                "idTutoria INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idEstudiante INTEGER NOT NULL, " +
                "idTutor INTEGER NOT NULL, " +
                "materia TEXT NOT NULL, " +
                "dia TEXT NOT NULL, " +
                "horaInicio TEXT NOT NULL, " +
                "horaFin TEXT NOT NULL, " +
                "activa INTEGER NOT NULL DEFAULT 1, " +
                "FOREIGN KEY (idEstudiante) REFERENCES usuarios(idUsuario), " +
                "FOREIGN KEY (idTutor) REFERENCES usuarios(idUsuario))";
     
       String sqlTutores = "CREATE TABLE IF NOT EXISTS tutores (" +
                "idTutor INTEGER PRIMARY KEY, " +
                "estadoSolicitud TEXT NOT NULL, " +
                "FOREIGN KEY (idTutor) REFERENCES usuarios(idUsuario))";

       String sqlMateriasTutor = "CREATE TABLE IF NOT EXISTS materias_tutor (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idTutor INTEGER NOT NULL, " +
                "materia TEXT NOT NULL, " +
                "FOREIGN KEY (idTutor) REFERENCES usuarios(idUsuario))";

       String sqlHorariosTutor = "CREATE TABLE IF NOT EXISTS horarios_tutor (" +
               "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
               "idTutor INTEGER NOT NULL, " +
               "dia TEXT NOT NULL, " +
               "horaInicio TEXT NOT NULL, " +
               "horaFin TEXT NOT NULL, " +
               "FOREIGN KEY (idTutor) REFERENCES usuarios(idUsuario))";
 
        try (Connection conexion = conectar();
             Statement st = conexion.createStatement()) {
 
            st.execute(sqlCorreos);
            st.execute(sqlUsuarios);
            st.execute(sqlTutorias);
            st.execute(sqlTutores);
            st.execute(sqlMateriasTutor);
            st.execute(sqlHorariosTutor);
 
        } catch (SQLException e) {
            System.out.println("Error al crear las tablas: " + e.getMessage());
        }
    }
}
 
