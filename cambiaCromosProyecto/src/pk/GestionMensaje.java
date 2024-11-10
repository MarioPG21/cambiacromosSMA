package pk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GestionMensaje implements Runnable {


    private Socket socket;
    public GestionMensaje(Socket socket, Agent agent) {this.socket = socket;}

    @Override
    public void run() {
        try {
            // Lector para recibir los datos del socket
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            StringBuilder mensaje = new StringBuilder();
            String respuesta = null;
            // Esta parte lee el mensaje línea por línea. Esto puede cambiar una vez se sepa el formato de mensajes recibidos.
            String linea;

            while ((linea = in.readLine()) != null) {
                mensaje.append(linea);
            }

            String msg = mensaje.toString();
            System.out.println("Mensaje recibido: " + msg);


            //                                         //
            //                                         //
            // Hueco para el procesamiento del mensaje //
            //                                         //
            //                                         //

            // Los métodos de gestión de mensaje devolverán en la variable "respuesta" el string XML necesario.
            // out.println(respuesta);


            // Cuando el mensaje es de muerte, comunicar la muerte al monitor y morir.
            // Se deberá editar para que consiga el mensaje del XML.
            if (msg.equals("Muere")){
                respuesta = "Proceso muerto";
                out.println(respuesta);
                in.close();
                out.close();
                socket.close();
                System.exit(0);
            }

            //                                         //
            //                                         //
            // Fin para el procesamiento del mensaje   //
            //                                         //
            //                                         //

            // Cierre del lector y del socket
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
