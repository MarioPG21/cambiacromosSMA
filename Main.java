import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Monitor monitor = new Monitor();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nSeleccione una opción:");
            System.out.println("1. Añadir agente");
            System.out.println("2. Eliminar agente");
            System.out.println("3. Mostrar todos los agentes");
            System.out.println("4. Mostrar información de un agente");
            System.out.println("5. Salir");
            System.out.print("Opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine(); // Consumir el salto de línea

            switch (opcion) {
                case 1 -> {
                    // Añadir agente
                    System.out.print("Ingrese el ID del agente: ");
                    int idAgregar = scanner.nextInt();
                    scanner.nextLine(); // Consumir el salto de línea
                    System.out.print("Ingrese la IP del controlador: ");
                    String ip = scanner.nextLine();
                    System.out.print("Ingrese el puerto del controlador: ");
                    int puerto = scanner.nextInt();
                    scanner.nextLine(); // Consumir el salto de línea
                    monitor.agregarAgente(idAgregar, ip, puerto);
                    System.out.println("Agente añadido correctamente.");
                }
                case 2 -> {
                    // Eliminar agente
                    System.out.print("Ingrese el ID del agente a eliminar: ");
                    int idEliminar = scanner.nextInt();
                    scanner.nextLine(); // Consumir el salto de línea
                    monitor.eliminarAgente(idEliminar);
                    System.out.println("Agente eliminado si existía.");
                }
                case 3 -> {
                    // Mostrar todos los agentes
                    Map<Integer, AgenteInfo> agentes = monitor.obtenerTodosLosAgentes();
                    if (agentes.isEmpty()) {
                        System.out.println("No hay agentes registrados.");
                    } else {
                        System.out.println("Lista de agentes:");
                        for (AgenteInfo agente : agentes.values()) {
                            System.out.println("ID: " + agente.getId() +
                                    ", IP: " + agente.getControladorIP() +
                                    ", Puerto: " + agente.getControladorPuerto());
                        }
                    }
                }
                case 4 -> {
                    // Mostrar información de un agente
                    System.out.print("Ingrese el ID del agente: ");
                    int idConsultar = scanner.nextInt();
                    scanner.nextLine(); // Consumir el salto de línea
                    AgenteInfo agente = monitor.obtenerAgente(idConsultar);
                    if (agente != null) {
                        System.out.println("Información del agente:");
                        System.out.println("ID: " + agente.getId());
                        System.out.println("IP: " + agente.getControladorIP());
                        System.out.println("Puerto: " + agente.getControladorPuerto());
                        System.out.println("Contador de inactividad: " + agente.getContadorInactividad());
                    } else {
                        System.out.println("Agente no encontrado.");
                    }
                }
                case 5 -> {
                    // Salir
                    System.out.println("Saliendo del programa.");
                    scanner.close();
                    System.exit(0);
                }
                default -> System.out.println("Opción no válida. Intente de nuevo.");
            }
        }
    }
}

