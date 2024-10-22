package pk;

import java.io.*;
import java.net.InetAddress;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("PID: "+ProcessHandle.current().pid());
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", "out/production/cambiaCromosProyecto", "pk.Agent");
        Process process = processBuilder.start();
        //Recibir salida del proceso
        BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
        //Enviar entrada al proceso
        BufferedWriter in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        Scanner scanner = new Scanner(System.in);
        System.out.println("1: Ver agente, 2: MATAR AGENTE >:(");
        String opStr = scanner.nextLine();
        int op = Integer.parseInt(opStr);
        if(op == 1){
            System.out.println("Viendo agente");
            System.out.println("Salida del agente: "+out.readLine());
            System.out.println("Salida del agente: "+out.readLine());
            System.out.println("Escribe tu respuesta: ");
            String input = scanner.nextLine();
            in.write(input + "\n");
            in.flush();
            System.out.println("Leyendo salida:");
            System.out.println(out.readLine());
            // Esperar a que el subproceso termine
            int exitCode = process.waitFor();
            System.out.println("Proceso agente termina con código de salida: " + exitCode);

        }else{
            System.out.println("MORID CORRUPTOS");
            process.destroyForcibly();
            System.out.println("POCIÓN DE VENENO 2");
        }

        // Cerramos flujos
        in.close();
        out.close();
    }
}