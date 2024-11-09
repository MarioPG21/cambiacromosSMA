package pk;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Agent{

    // Constructor
    public Agent(String controlIp, int controlPort) throws IOException {
        long time = System.currentTimeMillis();
        this.id = String.valueOf(time) ; // El ID será el tiempo de creación de objeto.
        this.dir = InetAddress.getLocalHost();
        this.listenSocket = new ServerSocket();
        this.portMin = 4000;
        this.portMax = 4100;
        this.controlIp = controlIp;
        this.controlPort = controlPort;
        this.ipList = new ArrayList<>();
        // Crea el hashmap concurrente que nos permitirá gestionar nuestra lista de agentes.
        this.agentList = new ConcurrentHashMap<>();
        findDir();          // Se coloca en un socket del rango establecido
        assignSubnetIPs();  // Define la lista de IP de nuestra subred.
        // TODO: mandar mensaje heNacido al monitor antes de ponerse a escuchar
        listen();           // Se pone a escuchar
    }

    // Lista de atributos
    private String id;                  // Id del agente
    private ServerSocket listenSocket;  // Socket de escucha
    private int port;                   // Puerto del socket de escucha en la máquina
    private int portMin;                // Comienzo del rango de puertos que usaremos para agentes
    private int portMax;                // Fin del rango de puertos que usaremos para agentes
    private InetAddress dir;            // Dirección del agente
    private String controlIp;           // Dirección del controlador a cargo del agente
    private int controlPort;            // Número de puerto para conectarnos al controlador
    private List<String> ipList;        // Lista de las ips de la subred en la que está el agente
    private Map<AgentKey, String> agentList;    // Lista de los agentes que hay en la subred


    // Getters
    public InetAddress getDir() {return dir;}
    public ServerSocket getSocket() {return this.listenSocket;}
    public int getPort(){return this.port;}

    private void findDir() throws UnknownHostException {
        String ipAdd = dir.getHostAddress();
        System.out.println("IP Address of the machine: " + ipAdd);
        for(int p = portMin; p <= portMax; p += 2){
            try{
                System.out.println("Attempting to nest in port " + p);
                InetSocketAddress address = new InetSocketAddress(ipAdd, p);
                listenSocket.bind(address);
                this.port = p;
                break;
            } catch (IOException e) {
                System.out.println("Couldn't nest in port.");
            }
        }
        System.out.println("Successfully nested in port "+ port);
    }

    private void assignSubnetIPs() throws IOException {
        String localIp = dir.getHostAddress(); // Get local IP as a string
        String[] octets = localIp.split("\\.");

        // Form the subnet base using the first three octets
        String subnetPrefix = octets[0] + "." + octets[1] + "." + octets[2] + ".";

        // Generate IPs in the subnet from .1 to .254
        for (int i = 1; i < 255; i++) {
            String ip = subnetPrefix + i;
            ipList.add(ip);
        }

        System.out.println("IPs in local subnet: " + ipList);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String monitorAddress = InetAddress.getLocalHost().getHostAddress();
        int monitorPort = 4300;

        // Definimos un reader para que el agente pueda recibir entrada desde el proceso padre
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Agente creado en proceso con PID:"+ProcessHandle.current().pid());
        System.out.flush();

        Agent agent = new Agent(monitorAddress, monitorPort);
        System.out.println("Agente "+agent.id+", con dirección "+agent.getDir().toString()+" y puerto "+agent.getPort());
        System.out.flush();

        //Comenzar búsqueda de agentes
        agent.agentSearch();

        Thread threadListen = new Thread(agent::listen);
        threadListen.start();

        //System.out.println("Me muero noooo ;-;");
        //System.out.flush();
        //return;

    }

    // Búsqueda de agentes

    private void agentSearch() {
        Thread searchThread = new Thread(new AgentSearch());
        searchThread.start();
    }

    private class AgentSearch implements Runnable {
        @Override
        public void run() {
            while (true) {  // Bucle infinito para buscar siempre
                for (String ipString : ipList) {  // iterar las ips en la lista de ips
                    try {
                        InetAddress ipAddress = InetAddress.getByName(ipString);
                        for (int p = portMin; p <= portMax; p += 2) {  // Iterar puertos pares dentro del rango
                            System.out.println("Agente buscando en IP: " + ipString + ", Puerto: " + p);
                            try{
                                // Intenta conectarse al supuesto agente localizado en ip y puerto
                                Socket agentSocket = new Socket();
                                // Reducimos el timeout para que tarde menos en iterar sobre los puertos
                                InetSocketAddress address = new InetSocketAddress(ipString, p);
                                agentSocket.connect(address, 100);

                                System.out.println("Agente encontrado en IP: " + ipString + ", Puerto: " + p);
                                // Mandar el mensaje de hola al agente
                                PrintWriter out = new PrintWriter(agentSocket.getOutputStream(), true);
                                //TODO: cambiar formato del mensaje cuando lo tengamos.
                                out.println("Hola agente!");
                                out.close();
                                agentSocket.close();
                                // Mandar mensaje de hola al monitor
                                try (Socket monitorSocket = new Socket(controlIp, controlPort)) {
                                    PrintWriter monitorOut = new PrintWriter(monitorSocket.getOutputStream(), true);
                                    //TODO: cambiar formato del mensaje cuando lo tengamos.
                                    monitorOut.println("Hola monitor!");
                                    monitorOut.close();
                                    monitorSocket.close();
                                } catch (IOException e) {
                                    System.err.println("No se pudo conectar al monitor en el puerto 4300.");
                                    e.printStackTrace();
                                }
                            } catch (IOException e) {
                                // Errores de conexión si un agente no está disponible
                                System.out.println("No se pudo conectar a " + ipString + " en el puerto " + p);
                            }
                        }
                    } catch (UnknownHostException e) {
                        System.err.println("Dirección IP desconocida: " + ipString);
                        e.printStackTrace();
                    }

                    // Espera de 2 segundos entre búsquedas
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        System.err.println("BuscarAgentes thread interrupted.");
                        Thread.currentThread().interrupt();
                        break;
                    }

                }
            }
        }
    }

    // Habla

    private void speak(String msg, String ipDest, int portDest){
        Thread speakThread = new Thread(new Speaker(msg, ipDest, portDest));
        speakThread.start();
    }

    private class Speaker implements Runnable{
        private String msg;     // Mensaje que se enviará
        private String ipDest;  // IP del destinatario
        private int portDest;   // Puerto del destinatario

        public Speaker(String msg, String ipDest, int portDest){
            this.msg = msg;
            this.ipDest = ipDest;
            this.portDest = portDest;
        }

        @Override
        public void run() {
            try{
                //TODO: cambiar esto cuando tengamos XML operativo si es necesario
                Socket socket = new Socket(ipDest, portDest);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(msg);
                socket.close();
            } catch (UnknownHostException e) {
                // Esta excepción saltará si hay un problema con la IP
                e.printStackTrace();
            } catch (IOException e) {
                // Esta es la excepción que salta cuando el agente no es alcanzable
                // TODO: hacer esto para actualizar lista de agentes en los métodos que lo requieran
                System.out.println("Couldn't reach agent, removing from list of agents.");
                AgentKey missing = new AgentKey(ipDest, portDest);
                agentList.remove(missing);
            }
        }
    }

    // Escucha

    private void listen(){
        Thread listenThread = new Thread(new Listener());
        listenThread.start();
    }

    private class Listener implements Runnable{
        @Override
        public void run(){
            System.out.println("Agente en escucha en el puerto " + port + "...");
            try {
                while (true) {  // Bucle de escucha infinito
                    // Espera por conexiones
                    Socket socket = listenSocket.accept();

                    // Ejecuta la lógica en un nuevo hilo de Gestión de mensajes
                    new GestionMensaje(socket).run();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* Método exec deprecado, habrá que matar el proceso desde el controlador o con opción en main
    public void off() throws IOException {
        Runtime.getRuntime().exec("taskkill /F /IM <processname>.exe");
    }*/
}
