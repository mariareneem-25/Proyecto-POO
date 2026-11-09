import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// pantallas de consola para cuidar la cuenta: actualizar perfil, eliminar perfil y reactivar cuenta
// Main solo la crea y llama a actualizar(), eliminar() o reactivar()

public class PerfilConsola {

    private final Scanner sc;

    public PerfilConsola(Scanner sc) {
        this.sc = sc;
    }

    // ---------------------------------------------------------------
    // Actualizar perfil: se elige qué dato cambiar
    // ---------------------------------------------------------------

    // devuelve true si la sesión terminó (por ejemplo, al cambiar de rol hay que volver a iniciar sesión)

    public boolean actualizar(Perfil_usuario sesion) {
        boolean esEstudiante = sesion.getTipoUsuario() == Perfil_usuario.TipoUsuario.ESTUDIANTE;
        boolean esTutor = sesion.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR;

        // el menú se arma según el tipo de cuenta
        List<String> opciones = new ArrayList<>();
        opciones.add("Nombre");
        opciones.add("Contraseña");
        opciones.add("Foto de perfil");
        if (esEstudiante || esTutor) {
            opciones.add("Calendario");
        }
        if (esEstudiante) {
            opciones.add("Cambiar a cuenta de tutor");
        }
        if (esTutor) {
            opciones.add("Cambiar a cuenta de estudiante");
        }
        opciones.add("Volver");

        while (true) {
            System.out.println("\n--- Actualizar perfil ---");
            for (int i = 0; i < opciones.size(); i++) {
                String texto = opciones.get(i);
                if (texto.equals("Nombre")) {
                    texto += " (actual: " + valor(sesion.getNombre()) + ")";
                } else if (texto.equals("Foto de perfil")) {
                    texto += " (actual: " + valor(sesion.getFotoPerfil()) + ")";
                }
                System.out.println((i + 1) + ". " + texto);
            }

            int eleccion = leerEntero("¿Qué quieres cambiar?: ");
            if (eleccion < 1 || eleccion > opciones.size()) {
                continue;
            }

            switch (opciones.get(eleccion - 1)) {
                case "Nombre":
                    cambiarNombre(sesion);
                    break;
                case "Contraseña":
                    cambiarContrasena(sesion);
                    break;
                case "Foto de perfil":
                    cambiarFoto(sesion);
                    break;
                case "Calendario":
                    new CalendarioConsola(sc).editar(sesion);
                    break;
                case "Cambiar a cuenta de tutor":
                    if (cambiarATutor(sesion)) {
                        return true;
                    }
                    break;
                case "Cambiar a cuenta de estudiante":
                    if (cambiarAEstudiante(sesion)) {
                        return true;
                    }
                    break;
                default:
                    return false;
            }
        }
    }

    // ---------------------------------------------------------------
    // Cambio de rol: estudiante -> tutor y tutor -> estudiante (siempre con doble confirmación)
    // ---------------------------------------------------------------

    private boolean cambiarATutor(Perfil_usuario sesion) {
        System.out.println("\n--- Cambiar a cuenta de tutor ---");
        System.out.println("Para ser tutor necesitas:");
        System.out.println("  - Nombre completo, descripción, materias que impartes, foto de perfil y banner.");
        System.out.println("  - Enviar tu solicitud a un docente o director de carrera, que debe aprobarla.");
        System.out.println("Qué pasará con tu cuenta:");
        System.out.println("  - Mientras no te aprueben, no podrás entrar al menú.");
        System.out.println("  - Se cerrarán tus solicitudes de tutoría y reservas como estudiante.");
        System.out.println("  - Dejarás de buscar tutores; en su lugar recibirás solicitudes de estudiantes.");
        System.out.println("  - Podrás volver a ser estudiante desde Actualizar perfil.");

        if (!confirmar("\n¿Quieres continuar? (s/n): ")
                || !confirmar("Confirmación final: ¿cambiar tu cuenta a tutor? (s/n): ")) {
            System.out.println("Cambio cancelado. Tu cuenta sigue igual.");
            return false;
        }

        try {
            CambioRol.aTutor(sesion.getIdUsuario());
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return false;
        }

        System.out.println("Tu cuenta ahora es de tutor. Completa tu información para enviar tu solicitud.");
        new SolicitudTutorConsola(sc).gestionarAcceso(sesion);
        System.out.println("Vuelve a iniciar sesión cuando quieras continuar.");
        return true;
    }

    private boolean cambiarAEstudiante(Perfil_usuario sesion) {
        System.out.println("\n--- Cambiar a cuenta de estudiante ---");
        System.out.println("Qué pasará con tu cuenta:");
        System.out.println("  - Tu información de tutor se borrará: solicitud, materias, descripción y banner.");
        System.out.println("  - Se cerrarán tus tutorías, reservas y solicitudes con estudiantes.");
        System.out.println("  - Para volver a ser tutor tendrás que enviar una nueva solicitud y esperar aprobación.");
        System.out.println("  - Tu nombre, contraseña y foto de perfil se conservan.");

        if (!confirmar("\n¿Quieres continuar? (s/n): ")
                || !confirmar("Confirmación final: ¿cambiar tu cuenta a estudiante y borrar tu información de tutor? (s/n): ")) {
            System.out.println("Cambio cancelado. Tu cuenta sigue igual.");
            return false;
        }

        try {
            CambioRol.aEstudiante(sesion.getIdUsuario());
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return false;
        }

        System.out.println("Tu cuenta ahora es de estudiante. Vuelve a iniciar sesión para continuar.");
        return true;
    }

    private boolean confirmar(String pregunta) {
        System.out.print(pregunta);
        return sc.nextLine().trim().equalsIgnoreCase("s");
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
        System.out.print("\n¿Estás seguro de eliminar tu cuenta? (s/n): ");
        String respuesta = sc.nextLine().trim();
        if (!respuesta.equalsIgnoreCase("s")) {
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
