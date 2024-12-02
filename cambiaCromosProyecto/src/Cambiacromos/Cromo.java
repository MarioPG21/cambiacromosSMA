package Cambiacromos;

public class Cromo {
    private int id; // Entre 1 y 100
    private double valor;
    private double probabilidad;

    public Cromo(int id, int valor, int probabilidad) {
        if (id < 1 || id > 100) {
            throw new IllegalArgumentException("El ID debe estar entre 1 y 100.");
        }
        this.id = id;
        this.valor = valor;
        this.probabilidad = probabilidad;
    }

    public int getId() {
        return id;
    }

    public double getValor() {
        return valor;
    }

    public double getProbabilidad() {
        return probabilidad;
    }
}

