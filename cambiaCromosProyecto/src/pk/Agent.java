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

    // Método de escucha que lanza un thread para cada mensaje recibido
    public void listen() throws IOException {
        System.out.println("Agente en escucha...");
        while (true) {  // Bucle de escucha infinito
            try {
                // Espera por conexiones
                Socket socket = listenSocket.accept();

                // Creamos un nuevo thread para manejar cada conexión
                Thread threadGestion = new Thread(new GestionMensaje(socket));
                threadGestion.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Método de habla
    /*public void speak() throws IOException {
        Socket socket = new Socket(controlDir, controlPort);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Envío de un mensaje XML
        String mensajeXML = "<mensaje><contenido>Feliz viernes a quien se lo merezca (En XML)</contenido></mensaje>";
        out.println(mensajeXML);


        Respuesta si quisieramos implementar que nos respondan a los mensajes:
        String respuestaXML = in.readLine();
        System.out.println(respuestaXML);

        in.close();
        out.close();
        socket.close();
    }*/

    public static void main(String[] args) throws IOException, InterruptedException {

        String[] values = args[0].split(" ");
        System.out.println("Agente creado en proceso con PID:" + ProcessHandle.current().pid());
        System.out.flush();
        Agent agent = new Agent(Integer.parseInt(values[0]), InetAddress.getByName(values[1]), Integer.parseInt(values[2]));
        System.out.println("Agente "+agent.id+", con dirección "+agent.getDir().toString()+" y puerto "+agent.getPort());
        System.out.flush();
        agent.listen();

        //System.out.println("Me muero noooo ;-;");
        //System.out.flush();
        //return;

    }


    /* Método exec deprecado, habrá que matar el proceso desde el controlador o con opción en main
    public void off() throws IOException {
        Runtime.getRuntime().exec("taskkill /F /IM <processname>.exe");
    }*/
}
