package pk;

import java.net.*;
import java.io.*;

public class Agent{

    // Constructor
    public Agent(int id, InetAddress control, int controlPort) throws IOException {
        this.id = id; // Asignada en construcción de objeto, se puede plantear asignacón automática
        this.dir = InetAddress.getLocalHost();
        this.listenSocket = new ServerSocket(0);  //port = 0 => busca automáticamente un puerto disponible
        this.port = this.listenSocket.getLocalPort();
        this.controlDir = control;
        this.controlPort = controlPort;
    }

    // Lista de atributos
    public int id;                      // Id del agente
    private ServerSocket listenSocket;  // Socket de escucha
    private int port;                   // Puerto del socket de escucha en la máquina
    private InetAddress dir;            // Dirección del agente
    private InetAddress controlDir;     // Dirección del controlador a cargo del agente
    private int controlPort;            // Número de puerto para conectarnos al controlador

    // Getters
    public InetAddress getDir() {return dir;}
    public ServerSocket getSocket() {return this.listenSocket;}
    public int getPort(){return this.port;}

    // Método de escucha
    public void listen() throws IOException {
        // Debería ser while(true) en un thread dedicado, provisionalmente implementado con 5 iteraciones
        for(int i = 0; i < 5; i++){
            Socket socket = listenSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Recepción de un mensaje XML
            // Iteración sobre las líneas recibidas hasta obtener una nula
            StringBuilder msg = new StringBuilder(); // Para crear una string con el mensaje completo a partir de lineas
            for(String line = in.readLine(); line != null; line = in.readLine()){
                msg.append(line);
            }

            System.out.println(msg);

            // Envío de un mensaje respuesta si fuera necesario


            in.close();
            out.close();
            socket.close();
        }
        listenSocket.close();
    }

    // Método de habla
    public void speak() throws IOException {
        Socket socket = new Socket(controlDir, controlPort);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Envío de un mensaje XML
        String mensajeXML = "<mensaje><contenido>Feliz viernes a quien se lo merezca (En XML)</contenido></mensaje>";
        out.println(mensajeXML);

        /*
        Respuesta si quisieramos implementar que nos respondan a los mensajes:
        String respuestaXML = in.readLine();
        System.out.println(respuestaXML);
         */
        in.close();
        out.close();
        socket.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Definimos un reader para que el agente pueda recibir entrada desde el proceso padre
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Estoy funcionando, mira que bien, PID:"+ProcessHandle.current().pid());
        System.out.println("1: Crea un agente, 2: mátame");
        System.out.flush();

        String opStr = reader.readLine();
        int op = Integer.parseInt(opStr);

        if(op == 1){
            Agent agent = new Agent(1, InetAddress.getLocalHost(), 2);
            System.out.println("Agente "+agent.id+", con dirección "+agent.getDir().toString()+" y puerto "+agent.getPort());
            System.out.flush();

        }else{
            System.out.println("Me muero noooo ;-;");
            System.out.flush();
            return;
        }
    }


    /* Método exec deprecado, habrá que matar el proceso desde el controlador o con opción en main
    public void off() throws IOException {
        Runtime.getRuntime().exec("taskkill /F /IM <processname>.exe");
    }*/
}
