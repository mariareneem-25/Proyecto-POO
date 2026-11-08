import java.util.Scanner;

// pantallas de consola para cuidar la cuenta: actualizar perfil, eliminar perfil y reactivar cuenta
// Main solo la crea y llama a actualizar(), eliminar() o reactivar()

public class PerfilConsola {

    private static final String PALABRA_CONFIRMACION = "ELIMINAR";

    private final Scanner sc;

    public PerfilConsola(Scanner sc) {
        this.sc = sc;
    }

    // ---------------------------------------------------------------
    // Actualizar perfil: se elige qué dato cambiar
    // ---------------------------------------------------------------

    public void actualizar(Perfil_usuario sesion) {
        int opcion = -1;

        while (opcion != 4) {
            System.out.println("\n--- Actualizar perfil ---");
            System.out.println("1. Nombre (actual: " + valor(sesion.getNombre()) + ")");
            System.out.println("2. Contraseña");
            System.out.println("3. Foto de perfil (actual: " + valor(sesion.getFotoPerfil()) + ")");
            System.out.println("4. Volver");
            opcion = leerEntero("¿Qué quieres cambiar?: ");

            if (opcion == 1) {
                cambiarNombre(sesion);
            } else if (opcion == 2) {
                cambiarContrasena(sesion);
            } else if (opcion == 3) {
                cambiarFoto(sesion);
            }
        }
    }

    private void cambiarNombre(Perfil_usuario sesion) {
        System.out.print("Nuevo nombre: ");
        String nuevoNombre = sc.nextLine().trim();

        if (nuevoNombre.isEmpty()) {
            System.out.println("El nombre no puede estar vacío.");
            return;
        }

        if (sesion.actualizarSoloNombre(nuevoNombre)) {
            System.out.println("Nombre actualizado.");
        } else {
            System.out.println("No se pudo actualizar el nombre.");
        }
    }

    private void cambiarContrasena(Perfil_usuario sesion) {
        System.out.print("Nueva contraseña: ");
        String nuevaContrasena = sc.nextLine();

        if (!Seguridad.contrasenaValida(nuevaContrasena)) {
            System.out.println(Seguridad.requisitosContrasena());
            return;
        }

        if (sesion.actualizarSoloContrasena(nuevaContrasena)) {
            System.out.println("Contraseña actualizada.");
        } else {
            System.out.println("No se pudo actualizar la contraseña.");
        }
    }

    private void cambiarFoto(Perfil_usuario sesion) {
        System.out.print("Nueva foto de perfil: ");
        String nuevaFoto = sc.nextLine().trim();

        if (sesion.actualizarSoloFoto(nuevaFoto)) {
            System.out.println("Foto de perfil actualizada.");
        } else {
            System.out.println("No se pudo actualizar la foto de perfil.");
        }
    }

    // ---------------------------------------------------------------
    // Eliminar perfil: explica qué pasa y pide una segunda verificación
    // ---------------------------------------------------------------

    // devuelve true solo si la cuenta quedó eliminada (la sesión ya no es válida)
    // si la persona cancela, no pasa nada y sigue en el menú

    public boolean eliminar(Perfil_usuario sesion) {
        System.out.println("\n--- Eliminar perfil ---");
        System.out.println("Qué pasará con tu cuenta:");
        System.out.println("  - Tu cuenta quedará desactivada y se cerrará tu sesión ahora mismo.");
        System.out.println("  - No podrás iniciar sesión mientras esté desactivada.");
        System.out.println("  - Tu información no se borra: solo cambia el estado de la cuenta.");
        System.out.println("Cómo reactivarla:");
        System.out.println("  - En el menú de inicio elige \"Reactivar cuenta\" e ingresa tu correo y tu contraseña.");
        System.out.print("\nPara confirmar, escribe " + PALABRA_CONFIRMACION
                + " (cualquier otra cosa cancela): ");

        String respuesta = sc.nextLine().trim();
        if (!respuesta.equals(PALABRA_CONFIRMACION)) {
            System.out.println("Eliminación cancelada. Tu cuenta sigue activa.");
            return false;
        }

        if (sesion.eliminarPerfil()) {
            System.out.println("Perfil eliminado. Puedes reactivarlo desde el menú de inicio.");
            return true;
        }

        System.out.println("No se pudo eliminar el perfil.");
        return false;
    }

    // ---------------------------------------------------------------
    // Reactivar una cuenta eliminada (desde el menú de inicio, sin sesión)
    // ---------------------------------------------------------------

    public void reactivar() {
        System.out.println("\n--- Reactivar cuenta ---");
        System.out.print("Correo: ");
        String correo = sc.nextLine();

        System.out.print("Contraseña: ");
        String contrasena = sc.nextLine();

        if (Perfil_usuario.reactivarCuenta(correo, contrasena)) {
            System.out.println("Tu cuenta fue reactivada. Ya puedes iniciar sesión.");
        }
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? "(vacío)" : texto;
    }

    private int leerEntero(String mensaje) {
        System.out.print(mensaje);
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
