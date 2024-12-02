package Cambiacromos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Sobre de inicio
// Dentro del álbum se definen los 100 cromos. Una función en el constructor rellena el álbum con unas cartas iniciales.

// TODO: Definir sets
// Definir los sets del álbum y aplicar su valor adicional al mismo.

// TODO: Orden de las listas de deseados y ofrecidos

// TODO: actualización del valor del álbum


public class Album {
    private List<Cromo> coleccion;
    private List<Cromo> lista_deseados;
    private List<Cromo> lista_ofrezco;
    private double valorTotal;

    public Album() {
        this.coleccion = new ArrayList<>();
        this.lista_deseados = new ArrayList<>();
        this.lista_ofrezco = new ArrayList<>();
        this.valorTotal = 0;
    }

    public double getValorTotal() {
        return valorTotal;
    }

    public List<Cromo> getColeccion() {
        return coleccion;
    }

    public List<Cromo> getLista_deseados() {
        return lista_deseados;
    }

    public List<Cromo> getLista_ofrezco() {
        return lista_ofrezco;
    }
}
