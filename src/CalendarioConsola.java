import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// pantallas del calendario de estudiantes y tutores
// ver(): muestra el calendario (menú principal) | editar(): marcar días como ocupado o fecha de preferencia
// (se abre desde "Actualizar perfil")
//
// las fechas RESERVADAS no se editan aquí: solo se crean y eliminan con solicitudes aceptadas en el chat

public class CalendarioConsola {

    private static final int MAXIMO_DIAS_POR_RANGO = 62;

    private final Scanner sc;
    private final CalendarioDAO dao = new CalendarioDAO();

    public CalendarioConsola(Scanner sc) {
        this.sc = sc;
    }

    public void ver(Perfil_usuario sesion) {
        try {
            System.out.println("\n--- Mi calendario ---");
            System.out.print(dao.resumen(sesion.getIdUsuario(), true));
            System.out.println("(Para cambiar tus días ocupados o de preferencia: Actualizar perfil > Calendario."
                    + " Las reservas se acuerdan por el chat.)");
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    public void editar(Perfil_usuario sesion) {
        int opcion = -1;

        while (opcion != 5) {
            System.out.println("\n--- Editar calendario ---");
            System.out.println("1. Ver mi calendario");
            System.out.println("2. Marcar días como ocupado");
            System.out.println("3. Marcar días como fecha de preferencia");
            System.out.println("4. Eliminar días marcados");
            System.out.println("5. Volver");
            opcion = leerEntero("Selecciona una opción: ");

            try {
                if (opcion == 1) {
                    System.out.println();
                    System.out.print(dao.resumen(sesion.getIdUsuario(), true));
                } else if (opcion == 2) {
                    marcar(sesion, CalendarioDAO.TipoMarca.OCUPADO);
                } else if (opcion == 3) {
                    marcar(sesion, CalendarioDAO.TipoMarca.PREFERENCIA);
                } else if (opcion == 4) {
                    eliminar(sesion);
                }
            } catch (RuntimeException e) {
                System.out.println("No se pudo completar la acción: " + e.getMessage());
            }
        }
    }

    private void marcar(Perfil_usuario sesion, CalendarioDAO.TipoMarca tipo) {
        String nombreTipo = (tipo == CalendarioDAO.TipoMarca.OCUPADO) ? "ocupado" : "fecha de preferencia";

        System.out.println("Escribe los días en formato aaaa-MM-dd, separados por coma.");
        System.out.println("Para un rango usa \"a\": 2026-12-01 a 2026-12-05");
        System.out.print("Días a marcar como " + nombreTipo + " (Enter para cancelar): ");
        String texto = sc.nextLine().trim();
        if (texto.isEmpty()) {
            return;
        }

        List<LocalDate> fechas = leerFechas(texto);
        System.out.println("Se marcarán como " + nombreTipo + ": " + fechas.size() + " día(s) ("
                + fechas.get(0) + (fechas.size() > 1 ? " ... " + fechas.get(fechas.size() - 1) : "") + ").");
        System.out.print("¿Guardar? (s/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("s")) {
            System.out.println("No se guardó nada.");
            return;
        }

        dao.guardarMarcas(sesion.getIdUsuario(), fechas, tipo);
        System.out.println("Calendario actualizado.");
    }

    private void eliminar(Perfil_usuario sesion) {
        List<CalendarioDAO.Marca> marcas = dao.listarMarcas(sesion.getIdUsuario());
        if (marcas.isEmpty()) {
            System.out.println("No tienes días marcados.");
            return;
        }

        for (int i = 0; i < marcas.size(); i++) {
            CalendarioDAO.Marca m = marcas.get(i);
            System.out.println((i + 1) + ". " + m.fecha + " | "
                    + (m.tipo == CalendarioDAO.TipoMarca.OCUPADO ? "Ocupado" : "Fecha de preferencia"));
        }
        System.out.println("(Las fechas reservadas no aparecen aquí: se eliminan con una solicitud en el chat.)");
        System.out.print("Números a eliminar separados por coma, T para todos, Enter para cancelar: ");
        String texto = sc.nextLine().trim();
        if (texto.isEmpty()) {
            return;
        }

        List<LocalDate> elegidas = new ArrayList<>();
        if (texto.equalsIgnoreCase("t")) {
            for (CalendarioDAO.Marca m : marcas) {
                elegidas.add(m.fecha);
            }
        } else {
            for (String parte : texto.split(",")) {
                int numero;
                try {
                    numero = Integer.parseInt(parte.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("\"" + parte.trim() + "\" no es un número válido.");
                }
                if (numero < 1 || numero > marcas.size()) {
                    throw new IllegalArgumentException("El número " + numero + " no está en la lista.");
                }
                elegidas.add(marcas.get(numero - 1).fecha);
            }
        }

        System.out.print("¿Eliminar " + elegidas.size() + " día(s) marcado(s)? (s/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("s")) {
            System.out.println("No se eliminó nada.");
            return;
        }

        dao.eliminarMarcas(sesion.getIdUsuario(), elegidas);
        System.out.println("Días eliminados.");
    }

    // acepta "2026-12-01, 2026-12-05" y rangos "2026-12-01 a 2026-12-05"
    static List<LocalDate> leerFechas(String texto) {
        List<LocalDate> fechas = new ArrayList<>();

        for (String parte : texto.split(",")) {
            String limpia = parte.trim();
            if (limpia.isEmpty()) {
                continue;
            }

            String[] rango = limpia.split("\\s+a\\s+");
            LocalDate inicio = parsear(rango[0]);
            LocalDate fin = (rango.length > 1) ? parsear(rango[1]) : inicio;

            if (fin.isBefore(inicio)) {
                throw new IllegalArgumentException("El rango " + limpia + " está al revés.");
            }
            if (inicio.plusDays(MAXIMO_DIAS_POR_RANGO).isBefore(fin)) {
                throw new IllegalArgumentException("Un rango puede tener máximo " + MAXIMO_DIAS_POR_RANGO + " días.");
            }

            for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
                if (!fechas.contains(dia)) {
                    fechas.add(dia);
                }
            }
        }

        if (fechas.isEmpty()) {
            throw new IllegalArgumentException("No escribiste ninguna fecha.");
        }
        return fechas;
    }

    static LocalDate parsear(String texto) {
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("\"" + texto.trim() + "\" no es una fecha válida (usa aaaa-MM-dd).");
        }
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
