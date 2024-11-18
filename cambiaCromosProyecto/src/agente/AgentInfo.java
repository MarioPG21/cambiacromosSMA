package agente;

public class AgentInfo {

    // Atributos de información del agente
    private final String id;
    // Este atributo simboliza el número de búsquedas que pueden pasar sin respuesta hasta borrar un agente
    private int ttl;

    public AgentInfo(String t){
        this.id = t;
        ttl = 2000;
    }

    public String getId(){
        return id;
    }

    // Se ha buscado, se baja el ttl
    public void searched(){
        ttl--;
    }

    // Hay respuesta, el ttl se resetea
    public void answered(){
        ttl = 20;
    }

    // Si el ttl es mayor que 0, consideramos el agente como vivo
    public boolean alive(){
        return ttl > 0;
    }

    // Override toString method

    @Override
    public String toString() {
        return id;
    }
}
