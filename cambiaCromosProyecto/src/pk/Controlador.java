package pk;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Controlador {

    //HashMap que nos ayudará a tener un listado de los procesos creados en la máquina y que tendrá como clave el id del agente
    Map<Integer, Process> mapaProcesos = new HashMap<Integer, Process>();



    //Método para lanzar agentes
    public void lanzar_agentes(int id, InetAddress control, int controlPort) throws IOException {
        String[] args = new String[3];
        args[0] =  Integer.toString(id);
        args[1] =  control.getHostAddress();
        args[2] =  Integer.toString(controlPort);
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", "out/production/cambiaCromosProyecto", "pk.Agent", String.join(" ", args));
        Process process = processBuilder.start();
        mapaProcesos.put(id, process);

    }

    //Método para detener un proceso
    public void detener_agente(int id){
        Process process = mapaProcesos.get(id);
        process.destroy();
        mapaProcesos.remove(id);
    }

    //Main de ejemplo
    public static void main(String[] args) throws IOException {
        Controlador controlador = new Controlador();
        controlador.lanzar_agentes(30, InetAddress.getLocalHost(), 3);
        controlador.lanzar_agentes(32, InetAddress.getLocalHost(), 4);
    }




}
