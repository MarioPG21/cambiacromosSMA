package monitor;

import java.util.Objects;

public class AgentKey_Monitor {

    // Atributos que componen la llave
    private final String idString;

    public AgentKey_Monitor(String s){
        this.idString = s;
    }

    // Getters
    public String getIpString(){return this.idString;}


    // Sobreescribir método equals
    public boolean equals(Object o){
        // Son mismo objeto => true
        if(this == o) return true;
        // No hay objeto o no son la misma clase => false
        if(o == null || getClass() != o.getClass()) return false;
        // Casting a la clase AgentKey
        AgentKey_Monitor that = (AgentKey_Monitor) o;
        // Compara ip y puerto
        return  this.idString.equals(that.idString);
    }

    // Cambiamos método hash para que se cree a partir de ip y puerto
    public int hashCode(){
        return Objects.hash(idString);
    }

    // Cambiamos método toString para que imprima la dupla (ip, puerto)
    public String toString(){
        return "(" +idString+ ")";
    }
}