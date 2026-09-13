import java.util.Scanner;

public class Menu_inicio_sesion {

    private static final Scanner sc = new Scanner(System.in);

    public static void iniciar() {
        int opcion = -1;

        while (opcion != 4) {
            System.out.println("\n=== Menú ===");
            System.out.println("1. Agregar correo autorizado");
            System.out.println("2. Registrarse");
            System.out.println("3. Iniciar sesión");
            System.out.println("4. Salir");
            System.out.print("Selecciona una opción: ");

            if (sc.hasNextInt()) {
                opcion = sc.nextInt();
            } else {
                sc.next();
                continue;
            }
            sc.nextLine();

            if (opcion == 1) {
                agregarCorreoAutorizado();
            } else if (opcion == 2) {
                registrar();
            } else if (opcion == 3) {
                iniciarSesion();
            }
        }

        System.out.println("Programa finalizado.");
    }

    private static void agregarCorreoAutorizado() {
        System.out.print("Correo a autorizar: ");
        String correo = sc.nextLine();

        System.out.println("Tipo de usuario (estudiante / director): ");
        String tipo = sc.nextLine();

        if (Perfil_usuario.registrarCorreo(correo, tipo)) {
            System.out.println("Correo autorizado correctamente.");
        }
    }

    private static void registrar() {
        System.out.print("Nombre: ");
        String nombre = sc.nextLine();

        System.out.print("Correo institucional: ");
        String correo = sc.nextLine();

        System.out.print("Contraseña: ");
        String contrasena = sc.nextLine();

        System.out.print("Foto de perfil (ruta o nombre de archivo): ");
        String fotoPerfil = sc.nextLine();

        Perfil_usuario usuario = Perfil_usuario.crearUsuario(nombre, correo, contrasena, fotoPerfil);

        if (usuario != null) {
            System.out.println("Cuenta creada correctamente:");
            System.out.println(usuario);
        }
    }

    private static void iniciarSesion() {
        System.out.print("Correo: ");
        String correo = sc.nextLine();

        System.out.print("Contraseña: ");
        String contrasena = sc.nextLine();

        Perfil_usuario sesion = Perfil_usuario.iniciarSesion(correo, contrasena);

        if (sesion == null) {
            return;
        }

        System.out.println("Sesión iniciada como: " + sesion.getNombre());

        int subopcion = -1;
        while (subopcion != 3) {
            System.out.println("\n1. Actualizar perfil");
            System.out.println("2. Eliminar perfil");
            System.out.println("3. Cerrar sesión");
            System.out.print("Selecciona una opción: ");

            if (sc.hasNextInt()) {
                subopcion = sc.nextInt();
            } else {
                sc.next();
                continue;
            }
            sc.nextLine();

            if (subopcion == 1) {
                System.out.print("Nuevo nombre: ");
                String nuevoNombre = sc.nextLine();
                System.out.print("Nueva contraseña: ");
                String nuevaContrasena = sc.nextLine();
                System.out.print("Nueva foto de perfil: ");
                String nuevaFoto = sc.nextLine();

                if (sesion.actualizarPerfil(nuevoNombre, nuevaContrasena, nuevaFoto)) {
                    System.out.println("Perfil actualizado.");
                }
            } else if (subopcion == 2) {
                if (sesion.eliminarPerfil()) {
                    System.out.println("Perfil eliminado.");
                }
                subopcion = 3;
            }
        }

        sesion.cerrarSesion();
    }
}