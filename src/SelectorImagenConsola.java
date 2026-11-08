import java.util.Scanner;

// versión temporal: la persona escribe la ruta o el nombre del archivo y se guarda como texto
// todavía no se copia ni se valida el archivo

public class SelectorImagenConsola implements SelectorImagen {

    private final Scanner sc;

    public SelectorImagenConsola(Scanner sc) {
        this.sc = sc;
    }

    @Override
    public String elegir(GestorArchivos.Categoria categoria, String etiqueta, String valorActual) {
        System.out.print(etiqueta + " (ruta o nombre de archivo, Enter para dejarla igual): ");
        String entrada = sc.nextLine().trim();
        return entrada.isEmpty() ? valorActual : entrada;
    }
}
