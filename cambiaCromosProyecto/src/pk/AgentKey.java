package pk;

import java.util.Objects;

public class AgentKey {

    // La llave serán estos dos atributos
    private final String ipString;
    private final int port;

    public AgentKey(String s, int p){
        this.ipString = s;
        this.port = p;
    }

    // Cambiamos método equals
    public boolean equals(Object o){
        // Si son el mismo objeto devuelve true
        if(this == o) return true;
        // Si no hay objeto o no son la misma clase devuelve false
        if(o == null || getClass() != o.getClass()) return false;

        // Compara string y puerto de ambos objetos
        AgentKey that = (AgentKey) o; // Casting a la misma clase
        return this.port == that.port && this.ipString.equals(that.ipString);
    }

    // Cambiamos método hash para que lo cree a partir de ip y puerto
    public int hashCode(){
        return Objects.hash(ipString, port);
    }

    // Cambiamos método toString para que imprima la dupla ip puerto
    public String toString(){
        return "("+ipString+", "+port+")";
    }
}
