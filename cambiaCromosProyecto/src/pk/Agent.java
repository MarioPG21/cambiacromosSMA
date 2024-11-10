package pk;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Agent{

    // Constructor
    public Agent(int id, InetAddress control, int controlPort) throws IOException {
        this.id = id; // Asignada en construcción de objeto, se puede plantear asignacón automática
        this.dir = InetAddress.getLocalHost();
        this.listenSocket = new ServerSocket(0);  //port = 0 => busca automáticamente un puerto disponible
        this.port = this.listenSocket.getLocalPort();
        this.controlDir = control;
        this.controlPort = controlPort;
        this.listaIPs = new ArrayList<>();
        assignSubnetIPs();
    }

    // Lista de atributos
    public int id;                      // Id del agente
    private ServerSocket listenSocket;  // Socket de escucha
    private int port;                   // Puerto del socket de escucha en la máquina
    private InetAddress dir;            // Dirección del agente
    private InetAddress controlDir;     // Dirección del controlador a cargo del agente
    private int controlPort;            // Número de puerto para conectarnos al controlador
    private List<String> listaIPs;
    private InetAddress ipMonitor;
    private int portMonitor = 4300;
    private static volatile boolean isPaused = false;
    // Getters
    public InetAddress getDir() {return dir;}
    public ServerSocket getSocket() {return this.listenSocket;}
    public int getPort(){return this.port;}


    public double comprobar_carga(){
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        return ((double) free / total) * 100;
    }


    public void reproducete(int id, InetAddress control, int controlPort) throws IOException {

        String[] args = new String[3];
        args[0] =  Integer.toString(id);
        args[1] =  control.getHostAddress();
        args[2] =  Integer.toString(controlPort);
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", "out/production/cambiaCromosProyecto", "pk.Agent", String.join(" ", args));
        Process process = processBuilder.start();

    }

    public void heNacido(){
        DataInputStream in;
        DataOutputStream out;
        String data = "HeNacido"; //TODO cambiar a nuevo mensaje
        try {
            Socket Agentsocket = new Socket(ipMonitor,portMonitor);
            out = new DataOutputStream(Agentsocket.getOutputStream());
            in = new DataInputStream(Agentsocket.getInputStream());
            out.writeUTF(data);
            data = in.readUTF();
            System.out.println("Respuesta: " + data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void parado(InetAddress agenteEmisor, int portEmisor){
        DataInputStream in;
        DataOutputStream out;
        isPaused = true;
        String data = "parado"; //TODO cambiar a nuevo mensaje
        try {
            Socket Agentsocket = new Socket(agenteEmisor,portEmisor);
            out = new DataOutputStream(Agentsocket.getOutputStream());
            in = new DataInputStream(Agentsocket.getInputStream());
            out.writeUTF(data);
            data = in.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void continua(){
        isPaused = false;
    }



    private void assignSubnetIPs() throws IOException {
        String localIp = dir.getHostAddress(); // Get local IP as a string
        String[] octets = localIp.split("\\.");

        // Form the subnet base using the first three octets
        String subnetPrefix = octets[0] + "." + octets[1] + "." + octets[2] + ".";

        // Generate IPs in the subnet from .1 to .254
        for (int i = 1; i < 255; i++) {
            String ip = subnetPrefix + i;
            listaIPs.add(ip);
        }

        System.out.println("IPs in local subnet: " + listaIPs);
    }

    // Método de escucha que lanza un thread para cada mensaje recibido
    public void listen() {
        System.out.println("Agente en escucha en el puerto " + port + "...");
        try {
            while (true) {  // Bucle de escucha infinito
                // Espera por conexiones
                Socket socket = listenSocket.accept();
                // Ejecuta la lógica en un nuevo hilo de Gestión de mensajes
                new GestionMensaje(socket,this).run();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        // Definimos un reader para que el agente pueda recibir entrada desde el proceso padre
        String[] values = args[0].split(" ");
        System.out.println("Agente creado en proceso con PID:"+ProcessHandle.current().pid());
        System.out.flush();
        Agent agent = new Agent(Integer.parseInt(values[0]), InetAddress.getByName(values[1]), Integer.parseInt(values[2]));
        System.out.println("Agente "+agent.id+", con dirección "+agent.getDir().toString()+" y puerto "+agent.getPort());
        System.out.flush();
        //Comenzar búsqueda de agentes
        agent.buscarAgentes();

        Thread threadListen = new Thread(agent::listen);
        threadListen.start();

        //System.out.println("Me muero noooo ;-;");
        //System.out.flush();
        //return;

    }

    public void buscarAgentes() {
        Thread searchThread = new Thread(new BuscarAgentes());
        searchThread.start();
    }
    private class BuscarAgentes implements Runnable {
        @Override
        public void run() {
            while (!isPaused) {  // Bucle infinito para buscar todo el rato
                for (String ipString : listaIPs) {  // iterar las ips en la lista de ips
                    try {
                        InetAddress ipAddress = InetAddress.getByName(ipString);
                        for (int port = 4000; port <= 4100; port += 2) {  // Iterar puertos pares dentro del rango

                            // Espera de 2 segundos entre comprobaciones
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                System.err.println("BuscarAgentes thread interrupted.");
                                Thread.currentThread().interrupt();
                                break;
                            }

                            try (Socket agentSocket = new Socket(ipAddress, port)) {  // Intenta conectarse al supuesto agente localizado en ip y puerto
                                System.out.println("Agente encontrado en IP: " + ipString + ", Puerto: " + port);
                                // Mandar el mensaje de hola al agente
                                PrintWriter out = new PrintWriter(agentSocket.getOutputStream(), true);
                                //TODO: cambiar formato del mensaje cuando lo tengamos.
                                out.println("Hola agente!");

                                // Mandar mensaje de hola al monitor
                                try (Socket monitorSocket = new Socket(controlDir, 4300)) {
                                    PrintWriter monitorOut = new PrintWriter(monitorSocket.getOutputStream(), true);
                                    //TODO: cambiar formato del mensaje cuando lo tengamos.
                                    monitorOut.println("Hola monitor!");
                                } catch (IOException e) {
                                    System.err.println("No se pudo conectar al monitor en el puerto 4300.");
                                    e.printStackTrace();
                                }
                            } catch (IOException e) {
                                // Errores de conexión si un agente no está disponible
                                System.out.println("No se pudo conectar a " + ipString + " en el puerto " + port);
                            }
                        }
                    } catch (UnknownHostException e) {
                        System.err.println("Dirección IP desconocida: " + ipString);
                        e.printStackTrace();
                    }

                }
            }
        }
    }
    /* Método exec deprecado, habrá que matar el proceso desde el controlador o con opción en main
    public void off() throws IOException {
        Runtime.getRuntime().exec("taskkill /F /IM <processname>.exe");
    }*/
}
