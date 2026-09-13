public class Main {

    public static void main(String[] args) {
        ConexionDB.crearTablas();
        Perfil_usuario.registrarCorreo("estudiante1@uvg.edu.gt", "estudiante");
        Perfil_usuario.registrarCorreo("director1@uvg.edu.gt", "director");

        Menu_inicio_sesion.iniciar();
    }
}