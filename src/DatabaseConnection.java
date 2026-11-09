import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// maneja una única conexión a la base de datos SQLite del proyecto y crea las tablas del foro si todavía no existen

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:tutorias.db";
    private static Connection connection;

    private DatabaseConnection() {
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON");
                crearTablas(connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo conectar a la base de datos SQLite", e);
        }
        return connection;
    }

    private static void crearTablas(Connection conn) throws SQLException {
        String tablaPublicaciones =
                "CREATE TABLE IF NOT EXISTS publicaciones (" +
                "  id TEXT PRIMARY KEY," +
                "  autor_id TEXT NOT NULL," +
                "  anonimo INTEGER NOT NULL," +
                "  titulo TEXT NOT NULL," +
                "  cuerpo TEXT NOT NULL," +
                "  fecha TEXT NOT NULL" +
                ")";

        String tablaComentarios =
                "CREATE TABLE IF NOT EXISTS comentarios (" +
                "  id TEXT PRIMARY KEY," +
                "  publicacion_id TEXT NOT NULL," +
                "  comentario_padre_id TEXT," +
                "  autor_id TEXT NOT NULL," +
                "  anonimo INTEGER NOT NULL," +
                "  cuerpo TEXT NOT NULL," +
                "  fecha TEXT NOT NULL," +
                "  FOREIGN KEY (publicacion_id) REFERENCES publicaciones(id)," +
                "  FOREIGN KEY (comentario_padre_id) REFERENCES comentarios(id)" +
                ")";

        String tablaMultimedia =
                "CREATE TABLE IF NOT EXISTS multimedia (" +
                "  id TEXT PRIMARY KEY," +
                "  publicacion_id TEXT NOT NULL," +
                "  tipo TEXT NOT NULL," +
                "  ruta_archivo TEXT NOT NULL," +
                "  FOREIGN KEY (publicacion_id) REFERENCES publicaciones(id)" +
                ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(tablaPublicaciones);
            stmt.execute(tablaComentarios);
            stmt.execute(tablaMultimedia);
        }
    }
}