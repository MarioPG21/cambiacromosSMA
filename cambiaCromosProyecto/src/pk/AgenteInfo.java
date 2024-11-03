package pk;

public class AgenteInfo {

    private final int id;
    private String controladorIP;
    private int controladorPuerto;
    private int contadorInactividad;

    public AgenteInfo(int id, String controladorIP, int controladorPuerto) {
        this.id = id;
        this.controladorIP = controladorIP;
        this.controladorPuerto = controladorPuerto;
        this.contadorInactividad = 0;
    }

    // Métodos getter y setter para el ID
    public int getId() {
        return id;
    }

    // Métodos getter y setter para controladorIP
    public String getControladorIP() {
        return controladorIP;
    }

    public void setControladorIP(String controladorIP) {
        this.controladorIP = controladorIP;
    }

    // Métodos getter y setter para controladorPuerto
    public int getControladorPuerto() {
        return controladorPuerto;
    }

    public void setControladorPuerto(int controladorPuerto) {
        this.controladorPuerto = controladorPuerto;
    }

    // Métodos getter y setter para contadorInactividad
    public int getContadorInactividad() {
        return contadorInactividad;
    }

    public void setContadorInactividad(int contadorInactividad) {
        this.contadorInactividad = contadorInactividad;
    }

    // Método para incrementar el contador de inactividad
    public void incrementarContadorInactividad() {
        this.contadorInactividad++;
    }

    // Método para reiniciar el contador de inactividad
    public void reiniciarContadorInactividad() {
        this.contadorInactividad = 0;
    }
}
