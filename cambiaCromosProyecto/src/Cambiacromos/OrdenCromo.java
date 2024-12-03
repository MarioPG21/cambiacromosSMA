package Cambiacromos;

public class OrdenCromo {
    public Cromo cromo;
    public double orden;

    public OrdenCromo(Cromo cromo, double orden){
        this.cromo = cromo;
        this.orden = orden;
    }

    @Override
    public String toString() {
        return "OrdenCromo{" +
                "cromo=" + cromo +
                ", orden=" + orden +
                '}';
    }
}
