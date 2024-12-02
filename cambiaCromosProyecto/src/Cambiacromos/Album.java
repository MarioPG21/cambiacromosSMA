package Cambiacromos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// En la colección hay 100 cromos en total. Cada cromo tiene un valor distinto, una probabilidad de aparición en el set de inicio y pertenece a un set distinto.  Los valores deben poder ser fácilmente modificados.

// En la colección hay los siguientes sets: 5 de 10 cartas, que serán sets básicos y aportarán un valor adicional fácilmente modificable. 5 de 10 cartas que serán los raros y que aportarán un valor fácilmente modificable.

//TODO: gestión de repetidos al abrir sobre de inicio

// Sobre de inicio
// Una función en el constructor rellena el álbum con 50 cromos iniciales, obtenidos aleatoriamente en base a su probabilidad de aparición en la lista de colección.

// TODO: Definir sets
// Se debe aplicar el valor adicional de un set al álbum.

// TODO: Orden de las listas de deseados y ofrecidos

// TODO: Actualización del valor del álbum



public class Album {
    // COLECCION es la lista de cromos completa
    private static final List<Cromo> COLECCION = generarCromos();

    private List<Cromo> tengo; // lista de cromos
    private List<Cromo> lista_deseados; // lista de los 10 cromos que no forman parte de la colección y que nos aportarían un valor total mayor al actual.
    private List<Cromo> lista_ofrezco; // lista de los 10 cromos que forman parte de la colección que menos valor total nos harían perder si faltasen.
    private double valorTotal; // valor de cada carta en la lista de cromos a demás del valor de los sets completados

    public Album() {
        this.tengo = new ArrayList<>();
        this.lista_deseados = new ArrayList<>();
        this.lista_ofrezco = new ArrayList<>();
        this.valorTotal = 0;
        abrirSobreInicial(5);
    }

    // Método que genera la colección de 100 cromos en base a las especificaciones escritas.
    private static List<Cromo> generarCromos() {
        List<Cromo> cromos = new ArrayList<>();
        List<Double> probabilidades = new ArrayList<>();

        // Las primeras 50 cartas tienen una probabilidad de 15% de aparecer cada una
        for (int i = 0; i < 50; i++) {
            probabilidades.add(0.15);
        }
        // Las siguientes 50 cartas tienen una probabilidad de 5% de aparecer cada una
        for (int i = 50; i < 100; i++) {
            probabilidades.add(0.05);
        }


        // Para los primeros 5 sets...
        for(int i = 1; i<=5; i++){
            // Cada carta del set...
            for(int j = 1; j<=10;j++){
                int valor = 1;
                int id = ((i-1)*10) + j;
                // Tiene un valor de 1, pertenece a los set [1-5] y tiene como id [1-50]
                cromos.add(new Cromo(id,valor, probabilidades.get(id-1),i));
            }
        }
        // Para los siguientes 5 sets...
        for(int i = 6; i<=10; i++){
            // Cada carta del set...
            for(int j = 1; j<=10;j++){
                int valor = 1;
                int id = ((i-1)*10) + j;
                // Tiene un valor de 1, pertenece a los set [6-10] y tiene como id [51-100]
                cromos.add(new Cromo(id,valor, probabilidades.get(id-1),i));
            }
        }

        return cromos;
    }

    // Método que genera 'cantidad' cartas aleatorias de la lista de cartas
    private void abrirSobreInicial(int cantidad) {
        for (int n = 0; n < cantidad+1; n++) {
            // Lista de probabilidades acumuladas. Convierte las probabilidades individuales en un rango, formado entre su propia probabilidad y la anterior.
            // Por ejemplo, si la probabilidad de las dos primeras cartas es 1%, p_acumuladas[0] = 1 y p_acumuladas[1] = 2.
            double[] p_acumuladas = new double[COLECCION.size()];
            p_acumuladas[0] = COLECCION.get(0).getProbabilidad();

            for (int i = 1; i < COLECCION.size(); i++) {
                p_acumuladas[i] = p_acumuladas[i - 1] + COLECCION.get(i).getProbabilidad();
            }

            // Generar un número aleatorio entre 0 y el total de probabilidades
            double aleatorio = Math.random() * p_acumuladas[p_acumuladas.length - 1];

            // Encontrar el cromo correspondiente
            for (int i = 0; i < p_acumuladas.length; i++) {
                if (aleatorio <= p_acumuladas[i]) {
                    this.tengo.add(COLECCION.get(i));
                    break;
                }
            }
        }
    }

    public double getValorTotal() {
        return valorTotal;
    }

    public List<Cromo> getTengo() {
        return tengo;
    }

    public List<Cromo> getLista_deseados() {
        return lista_deseados;
    }

    public List<Cromo> getLista_ofrezco() {
        return lista_ofrezco;
    }

    @Override
    public String toString() {
        return "Album{" +
                "tengo=" + tengo +
                ", valorTotal=" + valorTotal +
                '}';
    }

    public static void main(String[] args) {
        System.out.printf(new Album().toString());
    }
}
