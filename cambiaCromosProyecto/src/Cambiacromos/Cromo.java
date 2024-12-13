package Cambiacromos;

import java.util.Objects;

public class Cromo {
    private int id; // Entre 1 y 100
    private int valor; // valor de una carta por su rareza
    private Double probabilidad; // probabilidad de aparición
    private int set; // Conjunto de cartas dentro de la colección al que pertenece. Cuando se tienen las 5 cartas de un mismo set, el álbum obtiene un valor adicional.

    public Cromo(int id, int valor, Double probabilidad, int set) {
        this.id = id;
        this.valor = valor;
        this.probabilidad = probabilidad;
        this.set = set;
    }

    public int getId() {
        return id;
    }

    public int getValor() {
        return valor;
    }

    public Double getProbabilidad() {
        return probabilidad;
    }

    public int getSet() {
        return this.set;
    }

    @Override
    public String toString() {
        return ""+id;
        /*return "Cromo{" +
                "id=" + id +
                ", valor=" + valor +
                ", probabilidad=" + probabilidad +
                ", set=" + set +
                '}';*/
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cromo cromo = (Cromo) o;
        return id == cromo.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

