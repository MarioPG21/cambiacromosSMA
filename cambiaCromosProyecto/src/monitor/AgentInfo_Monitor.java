package monitor;

public class AgentInfo_Monitor {

    // Atributos de información del agente
    private final String id;
    public int felicidad;
    public int numSets_Completados;
    public int numCromos;

    public boolean isLadron() {
        return ladron;
    }

    public void setLadron(boolean ladron) {
        this.ladron = ladron;
    }

    public boolean ladron;

    public AgentInfo_Monitor(String id, int felicidad, int numSets_Completados, int numCromos,boolean ladron) {
        this.id = id;
        this.felicidad = felicidad;
        this.numSets_Completados = numSets_Completados;
        this.numCromos = numCromos;
        this.ladron = ladron;
    }


    // Override toString method

    @Override
    public String toString() {
        return id;
    }

    public String getId() {
        return id;
    }

    public int getFelicidad() {
        return felicidad;
    }

    public void setFelicidad(int felicidad) {
        this.felicidad = felicidad;
    }

    public int getNumSets_Completados() {
        return numSets_Completados;
    }

    public void setNumSets_Completados(int numSets_Completados) {
        this.numSets_Completados = numSets_Completados;
    }


    public int getNumCromos() {
        return numCromos;
    }

    public void setNumCromos(int numCromos) {
        this.numCromos = numCromos;
    }
}
