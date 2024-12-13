package Cambiacromos;

import java.util.*;

// En la colección hay 100 cromos en total. Cada cromo tiene un valor distinto, una probabilidad de aparición en el set de inicio y pertenece a un set distinto.  Los valores deben poder ser fácilmente modificados.

// En la colección hay los siguientes sets: 5 de 10 cartas, que serán sets básicos y aportarán un valor adicional fácilmente modificable. 5 de 10 cartas que serán los raros y que aportarán un valor fácilmente modificable.

// Sobre de inicio
// Una función en el constructor rellena el álbum con 50 cromos iniciales, obtenidos aleatoriamente en base a su probabilidad de aparición en la lista de colección.

// Definir sets
// Se debe aplicar el valor adicional de un set al álbum.

// Orden de las listas de deseados y ofrecidos

// Método de añadir y quitar cromo

// Actualización del valor del álbum



public class Album {
    // COLECCION es la lista de cromos completa.
    public static final List<Cromo> COLECCION = generarCromos();

    public ArrayList<Cromo> tengo; // lista de cromos
    public ArrayList<Cromo> lista_deseados; // lista de los 10 cromos que no forman parte de la colección y que nos aportarían un valor total mayor al actual.
    public ArrayList<Cromo> lista_ofrezco; // lista de los 10 cromos que forman parte de la colección que menos valor total nos harían perder si faltasen.

    public double valorTotal; // valor de cada carta en la lista de cromos a demás del valor de los sets completados

    public boolean[][] sets; // Matriz de tamaño 10x10. La primera driección se refiere al número de set y la segunda a la carta en el set. True si la tienes, False si no.
    public int[] valorSet; // Array con el valor del set i+1 (los sets empiezan en 1, el array en 0).

    private int S;

    // Constructor que inicializa las variables que contienen y determinan el dominio propio del agente.
    public Album(int sobreinicial,int S) {
        this.tengo = new ArrayList<>();
        this.lista_deseados = new ArrayList<>();
        this.lista_ofrezco = new ArrayList<>();

        this.sets = new boolean[10][10];
        this.valorSet = new int[10];

        // Valor adicional de los 5 primeros sets
        for (int i = 0; i < 5; i++) {
            this.valorSet[i] = 5;
        }
        // Valor adicional de los 5 siguientes sets
        for (int i = 5; i < 10; i++) {
            this.valorSet[i] = 10;
        }

        this.valorTotal = 0;

        this.S = S;

        abrirSobreInicial(sobreinicial);
        actualizarValor();
        ordenarDeseados();
        ordenarOfrecidos();
    }

    // Método que genera la colección de 100 cromos en base a las especificaciones.
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
                int valor = 2;
                int id = ((i-1)*10) + j;
                // Tiene un valor de 2, pertenece a los set [6-10] y tiene como id [51-100]
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

            boolean cromoValido = false;
            while (!cromoValido) {
                // Genera un número aleatorio entre 0 y el máximo de las variables acumuladas.
                double aleatorio = Math.random() * p_acumuladas[p_acumuladas.length - 1];

                // Para cada probabilidad...
                for (int i = 0; i < p_acumuladas.length; i++) {
                    // Si el número aleatorio entra dentro de la probabilidad...
                    if (aleatorio <= p_acumuladas[i]) {
                        Cromo cromo = COLECCION.get(i);
                        // Verifica si el cromo ya está en la colección.
                        if (!this.tengo.contains(cromo)) {
                            this.tengo.add(cromo);
                            this.sets[cromo.getSet() - 1][(cromo.getId() - (cromo.getSet() - 1) * 10) - 1] = true;
                            cromoValido = true; // Cromo válido encontrado y agregado, salir del bucle.
                        }
                        break;
                    }
                }
            }
        }
    }

    // Método que actualiza el valorTotal de álbum. Debe llamarse cada vez que se modifique el álbum.
    private void actualizarValor(){
        int valor_aux = 0;
        // Sumar los valores de cada carta individual
        for (int i = 0; i < this.tengo.size(); i++) {
            valor_aux += this.tengo.get(i).getValor();
        }

        // Sumar el valor de los sets completos
        for (int i = 0; i < 10; i++) {
            int count = 0;
            for (int j = 0; j < 10; j++) {
                if(this.sets[i][j]){
                    count++;
                }
            }
            if(count == 10){
                System.out.println("Set número "+ i + " completo.");
                this.valorTotal += this.valorSet[i];
            }
        }
        this.valorTotal = valor_aux;
    }

    // TODO: ARREGLAR ESTE MÉTODO
    // Método que actualiza la lista_deseados del álbum. Debe llamarse cada vez que se modifique el álbum.
    private void ordenarDeseados(){
        List<Cromo> no_tengo = new ArrayList<>();
        List<OrdenCromo> lista_ordenada = new ArrayList<>();

        // Obtengo la lista de cartas que no tengo.
        for (int i = 0; i < COLECCION.size(); i++) {
            if(!this.tengo.contains(COLECCION.get(i))){
                no_tengo.add(COLECCION.get(i));
            }
        }

        // Para cada cromo que no tengo...
        for (int i = 0; i < no_tengo.size(); i++) {
            Cromo cromo = no_tengo.get(i);
            int valorCromo = cromo.getValor();
            int set = cromo.getSet();
            // Obtengo el valor que me daría completar su set.
            int valorAdicional = this.valorSet[set-1];
            // Obtengo la cantidad de cromos restantes para el set.
            int restantes = 0;
            for (int cr = 0; cr < 10; cr++) {
                if(!this.sets[set-1][cr]) restantes++;
            }

            // Para evitar dividir por 0. No debería darse el caso, pero por si ocurriese etá gestionado.
            if(restantes!=0) {
                double orden = valorCromo + (valorAdicional/restantes);
                lista_ordenada.add(new OrdenCromo(cromo,orden));
            }
            else{
                // Aquí no debería entrar nunca, ya que si el set está completo, no estará evaluando la carta ya que sí que la tiene.
                double orden = 0;
                lista_ordenada.add(new OrdenCromo(cromo,orden));
            }
        }
        // Ordenar la lista usando Comparator
        Collections.sort(lista_ordenada, new Comparator<OrdenCromo>() {
            @Override
            public int compare(OrdenCromo o1, OrdenCromo o2) {
                return Double.compare(o2.orden, o1.orden); // Orden descendente
            }
        });

        //
        for (int i = 0; i < 10; i++) {
            //System.out.println(lista_ordenada.get(i));
            this.lista_deseados.add(lista_ordenada.get(i).cromo);
        }
    }

    // TODO: ARREGLAR ESTE MÉTODO
    // Método que actualiza la lista_ofrezco del álbum. Debe llamarse cada vez que se modifique el álbum.
    private void ordenarOfrecidos(){
        List<OrdenCromo> lista_ordenada = new ArrayList<>();
        // Para cada cromo que tengo...
        for (int i = 0; i < this.tengo.size(); i++) {
            Cromo cromo = this.tengo.get(i);
            int valorCromo = cromo.getValor();
            int set = cromo.getSet();
            int valorAdicional = this.valorSet[set-1]; // Obtengo el valor que me daría completar su set
            int restantes = 0;
            for (int cr = 0; cr < 10; cr++) {
                if(!this.sets[set-1][cr]) restantes++; // Obtengo la cantidad de cromos restantes para el set
            }

            // Misma lógica que antes
            if(restantes!=0) {
                double orden = valorCromo + (valorAdicional/restantes);
                lista_ordenada.add(new OrdenCromo(cromo,orden));
            }
            else{
                // Cuando tiene el set de esta carta completo, es más dificil que quiera darla.
                double orden = valorCromo + valorAdicional;
                lista_ordenada.add(new OrdenCromo(cromo,orden));
            }


        }
        // Ordenar la lista usando Comparator
        Collections.sort(lista_ordenada, new Comparator<OrdenCromo>() {
            @Override
            public int compare(OrdenCromo o1, OrdenCromo o2) {
                return Double.compare(o1.orden, o2.orden); // Orden descendente
            }
        });

        for (int i = 0; i < 10; i++) {
            //System.out.println(lista_ordenada.get(i));
            this.lista_ofrezco.add(lista_ordenada.get(i).cromo);
        }
    }

    // Método que recibe un cromo que eliminar del álbum.
    public void quito(Cromo cromo){
        this.tengo.remove(cromo);
        this.sets[cromo.getSet()-1][(cromo.getId() - (cromo.getSet() - 1) * 10) - 1] = false;
        actualizarValor();
        ordenarOfrecidos();
        ordenarDeseados();
    }

    // Método que recibe un cromo que añadir al álbum. No permite introducir un cromo repetido.
    public void consigo(Cromo cromo){
        if(!this.tengo.contains(cromo)){
            this.tengo.add(cromo);
            this.sets[cromo.getSet()-1][(cromo.getId() - (cromo.getSet() - 1) * 10) - 1] = true;
            actualizarValor();
            ordenarOfrecidos();
            ordenarDeseados();
        } else {
            System.out.println("Ya tienes ese cromo");
        }

    }
    @Override
    public String toString() {
        return "Album{" +
                "tengo=" + tengo +
                ", valorTotal=" + valorTotal +
                '}';
    }
    public static void main(String[] args) {
        Random random = new Random();
        Album album1 = new Album(10, 1);
        Album album2 = new Album(10, 1);


        Cromo cromo1 = album1.COLECCION.get(0);
        Cromo cromo2 = album1.COLECCION.get(1);
        Cromo cromo3 = album1.COLECCION.get(2);
        Cromo cromo4 = album1.COLECCION.get(3);
        Cromo cromo5 = album1.COLECCION.get(4);
        Cromo cromo6 = album1.COLECCION.get(5);
        Cromo cromo7 = album1.COLECCION.get(6);
        Cromo cromo8 = album1.COLECCION.get(7);
        Cromo cromo9 = album1.COLECCION.get(8);
        Cromo cromo10 = album1.COLECCION.get(9);
        Cromo cromo15 = album1.COLECCION.get(14);

        album1.consigo(cromo1);
        album1.consigo(cromo2);
        album1.consigo(cromo3);
        album1.consigo(cromo15);

        album1.ordenarDeseados();
        album1.ordenarDeseados();
        album1.ordenarDeseados();
        album1.ordenarOfrecidos();
        album1.ordenarOfrecidos();
        album1.ordenarOfrecidos();

        System.out.println("DESEADOS: ");
        System.out.println(album1.lista_deseados);

        System.out.println("OFRECIDOS: ");
        System.out.println(album1.lista_ofrezco);

       System.out.println(album1.toString());

       album1.evaluarIntercambio(cromo15, cromo10);

    }


    //////////////////////////////////

    ///   PARTE DE INTELIGENCIA   //

    ////////////////////////////////


    // Recibe dos objetos carta, suponemos que el intercambio es cambiar carta A por carta B.
    // Devuelve un TRUE si se recomienda realizar el intercambio y false si no.
    // Se calcula teniendo en cuenta la diferencia de valor en cada carta y la diferencia esperada de valor en los sets
    public boolean evaluarIntercambio(Cromo cartaA, Cromo cartaB) {

        // Paso 2: Calcular el cambio en el valor individual de las cartas
        double diferencia_valor_cartas = cartaB.getValor() - cartaA.getValor();

        // Paso 3: Identificar los sets afectados por el intercambio
        ArrayList<Integer> setsAfectados = new ArrayList<Integer>();
        setsAfectados.add(cartaA.getSet());
        setsAfectados.add(cartaB.getSet());

        // Paso 4: Calcular el cambio en el valor esperado por sets
        double diferencia_valor_sets = 0.0;
        for (int setId : setsAfectados) {
            //System.out.println("Considerando " + setId);
            // Situación antes del intercambio
           // System.out.println("Probabilidad antes:");
            double probAntes = calcularProbabilidadCompletarSet(this.sets, setId);
           // System.out.println(probAntes);


            boolean tieneCartaA = (cartaA.getSet() == setId);
            boolean tieneCartaB = (cartaB.getSet() == setId);

            // Situación después del intercambio
            boolean[][] setDespues = new boolean[sets.length][];
            for (int i = 0; i < sets.length; i++) {
                setDespues[i] = sets[i].clone(); // Esto copia las filas de forma independiente
            }


            if (tieneCartaA) {
                setDespues[setId - 1][(cartaA.getId() - (setId-1)*10) - 1] = false;
            }
            if (tieneCartaB) {
                setDespues[setId -1][(cartaB.getId() - (setId-1)*10) - 1] = true;
            }


            // Calcular la probabilidad de completar el set antes y después

            double probDespues = calcularProbabilidadCompletarSet(setDespues, setId);
//            System.out.println("Probabilidad después:");
//            System.out.println(probDespues);

            // Calcular el cambio en el valor esperado del set
            double deltaProb = probDespues - probAntes;
            double valorAdicionalSet = valorSet[setId - 1];
            double deltaValorEsperadoSet = deltaProb * valorAdicionalSet;

//            System.out.println("Valor potencial set: ");
//            System.out.println(deltaValorEsperadoSet);

            diferencia_valor_sets += deltaValorEsperadoSet;
        }

        // Paso 5: Calcular el cambio total en el valor esperado del álbum
        double diferencia_valor_total_album = diferencia_valor_cartas + diferencia_valor_sets;

        // Paso 6: Decidir si aceptar el intercambi
        double umbral = (1 - (S / 10.0)) * 5; // Por ejemplo 5 unidades de valor de margen
        boolean aceptarIntercambio_optimo = diferencia_valor_total_album > 0;

        //Paso 7: Introducimos la posibilidad de equivocarse
        // Introducimos la probabilidad de error según la "inteligencia" S (1 a 100)
        double p_error = 1.0 - (S / 100.0);

        // Generamos un número aleatorio
        double randomValue = Math.random(); // entre 0 y 1

        boolean decisionFinal;
        if (randomValue < p_error) {
            // Toma la decisión contraria a la óptima
            decisionFinal = !aceptarIntercambio_optimo;
        } else {
            // Sigue la decisión óptima
            decisionFinal = aceptarIntercambio_optimo;
        }


        // Paso 8: Imprimir los resultados
//        System.out.println("Cambio en el valor individual de las cartas: " + diferencia_valor_cartas);
//        System.out.println("Cambio en el valor esperado por sets: " + diferencia_valor_sets);
//        System.out.println("Incremento total en el valor del álbum: " + diferencia_valor_total_album);
//        if (aceptarIntercambio_optimo) {
//            System.out.println("Decisión: Se recomienda realizar el intercambio.");
//        } else {
//            System.out.println("Decisión: No se recomienda realizar el intercambio.");
//        }

        return   decisionFinal;
    }


    // Dado una estructura de set  boolean[][] set, y un setID, calcula la probabilidad de completar ese set.
    private double calcularProbabilidadCompletarSet(boolean[][] set, int setId) {
        double probabilidad = 1.0;
        int index_start = (setId - 1)*10;
        for (int i = 0; i < 10; i++) {
            if (!set[setId - 1][i]) {
                Cromo cromoFaltante = COLECCION.get(index_start + i);
                probabilidad = probabilidad * cromoFaltante.getProbabilidad();
            }
        }

        return probabilidad;
    }
}
