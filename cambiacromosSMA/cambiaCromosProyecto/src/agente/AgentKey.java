package agente;

import java.util.Objects;

public class AgentKey {

    // Atributos que componen la llave
    private final String ipString;
    private final int port;

    public AgentKey(String s, int p){
        this.ipString = s; this.port = p;
    }

    // Getters
    public String getIpString(){return this.ipString;}
    public int getPort(){return this.port;}

    // Sobreescribir método equals
    public boolean equals(Object o){
        // Son mismo objeto => true
        if(this == o) return true;
        // No hay objeto o no son la misma clase => false
        if(o == null || getClass() != o.getClass()) return false;
        // Casting a la clase AgentKey
        AgentKey that = (AgentKey) o;
        // Compara ip y puerto
        return this.port == that.port && this.ipString.equals(that.ipString);
    }

    // Cambiamos método hash para que se cree a partir de ip y puerto
    public int hashCode(){
        return Objects.hash(ipString, port);
    }

    // Cambiamos método toString para que imprima la dupla (ip, puerto)
    public String toString(){
        return "("+ipString+", "+port+")";
    }
}
