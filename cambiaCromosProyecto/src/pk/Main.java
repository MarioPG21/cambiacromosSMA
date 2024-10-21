package pk;

import java.io.*;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", "cambiacromosSMA\\cambiaCromosProyecto\\src\\pk", "Agent.java");
        Process process = processBuilder.start();
        //Recibir salida del proceso
        BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
        //Enviar entrada al proceso
        BufferedWriter in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        System.out.println(process.isAlive());
        String line;
        while ((line = out.readLine()) != null) {  // Leer todas las líneas de salida del subproceso
            System.out.println("Salida del agente: " + line);
        }


        Scanner scanner = new Scanner(System.in);
        System.out.println("1: Ver agente, 2: MATAR AGENTE >:(");
        String opStr = scanner.nextLine();
        int op = Integer.parseInt(opStr);
        if(op == 1){
            System.out.println("Viendo agente");


            while ((line = out.readLine()) != null) {  // Leer todas las líneas de salida del subproceso
                System.out.println("Salida del agente: " + line);
            }
            System.out.println(process.isAlive());
            System.out.println("Escribe tu respuesta: ");
            System.out.println(process.isAlive());
            String input = scanner.nextLine();
            System.out.println(process.isAlive());
            in.write(input + "\n");
            in.flush();
            System.out.println("Leyendo salida:");
            while ((line = out.readLine()) != null) {  // Leer todas las líneas de salida del subproceso
                System.out.println("Salida del agente: " + line);
            }

            // Esperar a que el subproceso termine
            int exitCode = process.waitFor();
            System.out.println("Subproceso terminó con código de salida: " + exitCode);

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