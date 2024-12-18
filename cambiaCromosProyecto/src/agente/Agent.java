package agente;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

import java.lang.management.ManagementFactory;

import Cambiacromos.Cromo;
import com.sun.management.OperatingSystemMXBean;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.util.*;
import java.util.Random;

import java.time.LocalTime;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import Cambiacromos.Album;



public class Agent {

    // Atributos o propiedades de la clase
    private String id;
    private String ip;
    private int num_sons = 0;
    private int serverPort = 0;
    private int udpPort = 0;
    private long ts;
    private ServerSocket serverSocket;
    private DatagramSocket datagramSocket;
    private ConcurrentHashMap<AgentKey, AgentInfo> discoveredAgents = new ConcurrentHashMap<>();
    private ArrayList<String> ipList = new ArrayList<>(List.of("127.0.0.1"));
    //Monitor info
    private final String monitorIP = "127.0.0.1";
    private final int monitorPort = 4300;
    private AgentKey monitor_key;

    //Para parar el agente
    private final Object monitor_stop = new Object();
    private boolean pausado;

    // Para controlar el intercambio
    private ReentrantLock tradeLock = new ReentrantLock();          // Candado para controlar los intercambios
    private AtomicBoolean busy = new AtomicBoolean(false); // Bool que define si el agente está ocupado
    private AgentKey negotiationId = null;  // Id del agente con el que se está negociando

    // VAMOS A DEFINIR DISTINTAS COLAS
    int timeout = 20000;                                                         // TIMEOUT QUE USAREMOS PARA LAS COLAS
    LinkedBlockingQueue<Message> tradeQ = new LinkedBlockingQueue<>();          // PARA OFERTAS INICIALES
    LinkedBlockingQueue<Message> responseQ = new LinkedBlockingQueue<>();       // PARA RESPUESTAS
    LinkedBlockingQueue<Message> negotiationQ = new LinkedBlockingQueue<>();    // PARA MENSAJES DE INTERCAMBIO

    //Atributos funcion del agente
    Random random = new Random();
    private int S = random.nextInt(41) + 60;
    private boolean G = false;
    private Album album = new Album(60,S);
    private double initial_album_value = album.valorTotal;
    private double felicidad = 0;

    //Subir cada vez que se realice un intercambio
    // TODO: concretar funcionamiento de esto, ahora mismo está sólo con los intercambios exitosos
    private int trade_counter = 0;

    private double regularizacion_incremento_album = 0.2;
    private double regularizacion_numero_intercambios = 0.2;

    // Constructor
    public Agent(String id) throws UnknownHostException {
        // Pillamos nuestra IP local
        this.ip = getLocalIpAddress();

        //Encuentra puertos y los asigna automaticamente
        findPorts(); 

        // Pillamos un timestamp
        this.ts = System.currentTimeMillis();
        // El ID se recibe como parámetro
        this.id = id;
        // Inicializamos el socket de servidor
        initializeServerSocket();
        
        // Inicializamos el datagram socket
        initializeDatagramSocket();

        this.monitor_key = new AgentKey(this.monitorIP, this.monitorPort);

        // Avisar al Monitor de que el agente ha nacido;
        Message message = createMessage(null, "1","heNacido", 1, "TCP", monitor_key);
        message.addInfoMonitor((int)felicidad,album.getSetsCompletados(),album.tengo.size());
        sendToMonitor(message.toXML());
        // Por ahora lanzamos los hilos independientes asi para poder hacerlo
        new Thread(this::listenForMessages).start();
        new Thread(this::findAgents).start();
        new Thread(this::listenForUdpMessages).start();
    }

    // Método para obtener la IP local
    private String getLocalIpAddress() {
        // NOTA: VA COMO UN TIRO PERO PARA LAS PRUEBAS VAMOS A HACERLO CON LA IP LOCAL POR DEFECTO
        /*try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
         } catch (UnknownHostException e) {
            e.printStackTrace();
            return "127.0.0.1"; // IP por defecto en caso de error
        }*/
        return "127.0.0.1";
    }

    // Método para encontrar puertos disponibles y asignarlos
    private void findPorts() {
        int serverPort = 4000;  // Inicio en el rango de puertos especificado
        while (serverPort <= 4100) {  // Hasta el final del rango especificado
            if (isPortAvailable(serverPort) && isPortAvailable(serverPort + 1)) {
                // Asigna el puerto par para el cliente y el siguiente para el servidor
                this.serverPort = serverPort;// Cliente (par)
                this.udpPort = serverPort + 1;
                break;
            }
            serverPort += 2;  // Avanza al siguiente par
        }
        if (this.serverPort == 0 || this.udpPort == 0) {
            throw new RuntimeException("No se encontraron puertos disponibles.");
        }
    }

    // Comprueba si un puerto está disponible
    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;  
        } catch (IOException e) {
            return false; 
        }
    }

    // Método para inicializar el socket de escucha (servidor)
    public void initializeServerSocket() {
        try {
            serverSocket = new ServerSocket(this.serverPort);
            System.out.println("Server socket initialized on port " + this.serverPort);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para inicializar el socket de escucha (descubrimiento)
    public void initializeDatagramSocket() {
    try {
        // Inicializa el DatagramSocket en el puerto específico
        this.datagramSocket = new DatagramSocket(this.udpPort);
        System.out.println("DatagramSocket initialized on port " + this.udpPort);
    } catch (SocketException e) {
        System.err.println("Error initializing DatagramSocket on port " + this.udpPort);
        e.printStackTrace();
    }
}

    // Método para enviar un mensaje a otro agente usando TCP
    public void sendMessage(String targetIp, int targetPort, String message) {
        try {
            // Inicializar el socket de cliente conectado al puerto de destino
            Socket clientSocket = new Socket(targetIp, targetPort);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println(message);
            System.out.println("Message sent to " + targetIp + ":" + targetPort + " -> " + message);
            clientSocket.close();  // Cerrar el socket después de enviar el mensaje
        } catch (IOException e) {
            System.out.println("Agent unreachable, removing from agent list");
            AgentKey k = new AgentKey(targetIp, targetPort);
            discoveredAgents.remove(k);
            //Poner ERROR DEL AGENTE
        }
    }

    // Metodo ejecutado en un hilo para escuchar todos los mensajes TCP entrantes con el socket inicializado
    // Valida que cumplan con la especificacion XML
    public void listenForMessages() {

        while (true) {

            try {

                // Acepta una conexión entrante
                Socket incomingConnection = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(incomingConnection.getInputStream()));
                String receivedMessage = in.readLine();
                // System.out.println(receivedMessage);

                // Validar el mensaje XML
                if (validate(receivedMessage)) {
                    // System.out.println("Received valid message:\n" + receivedMessage);
                    Message m = new Message(receivedMessage);

                    //Mostramos el tipo de mensaje para que se gestione correctamente
                    //System.out.println("Tipo de mensaje:" + getTypeProtocol(receivedMessage));

                    this.interpretarTipoMensaje(m.getProtocol(), m.getOriginId(), m);
                } else {
                    System.out.println("Mensaje descartado: No cumple con la estructura XML definida.");
                }

                // Cierra la conexión una vez procesado el mensaje
                incomingConnection.close();
            } catch (IOException e) {
                //e.printStackTrace();
                break;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para enviar un mensaje al Monitor
    private void sendToMonitor(String msg){
        Socket socket = null;
        PrintWriter out = null;
        try{
            socket = new Socket(monitorIP, monitorPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(msg);
            out.flush(); // Asegurar que el mensaje se envía
        } catch(Exception e) {
            System.out.println("ERROR: UNREACHABLE MONITOR");
            e.printStackTrace();
        } finally {
            // Cerrar recursos
            try {
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Print información principal sobre el agente
    public void reporteEstado() {
        System.out.println("=== Agent Status Report ===");
        System.out.println("ID: " + this.id);
        System.out.println("IP: " + this.ip);
        System.out.println("Server Port (TCP): " + this.serverPort);
        System.out.println("UDP Port: " + this.udpPort);
        System.out.println("Timestamp: " + this.ts);
    
        System.out.println("Discovered Agents:");
        if (discoveredAgents.isEmpty()) {
            System.out.println("  No agents discovered yet.");
        } else {
            for (Map.Entry<AgentKey, AgentInfo> entry : discoveredAgents.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }
        }
        System.out.println("===========================");
    }
    
    // IMPORTANTE LEER PARA APLICAR EL PROTOCOLO
    // CAMBIO IMPORTANTE, AHORA AQUI SE GESTIONARA LA LOGICA DE LOS INTERCAMBIOS
    // CADA VEZ QUE SE REALICE UN INTERCAMBIO, LLAMAR A actualizarFelicidad() y a check g para que tenga sentido el sistema
    // Puede decidir si realizar o no un intercambio llamando a this.album.evaluarIntercambio que recibe dos instancias del tipo cromo y devuelve si el agente hace o no el intercambio
    // Consideramos que el cromo A es el que tenemos y vamos a dar y el Cromo B el que vamos a recibir.
    public void funcionDelAgente() throws InterruptedException {

        Thread.sleep(2000);
        // Se ejecuta siempre
        while(true) {
            // Mientras que no esté pausado
            if(!pausado){
                Thread.sleep(200);
                for(AgentKey k : this.discoveredAgents.keySet()){
                    // Si no está ocupado manda ofertas iniciales a los agentes
                    if(!busy.get()){

                        // Creamos mensaje y le pasamos nuestra información de ofertaInicial
                        // (deseados, ofrecidos, G, rupias)
                        Message m = createMessage(null, "1", "ofertaInicial", 1, "TCP", k);
                        m.addTrade(this.album.lista_deseados, this.album.lista_ofrezco, false, 0);
                        m.addInfoMonitor((int) felicidad,album.getSetsCompletados(),album.tengo.size());
                        sendMessage(k.getIpString(), k.getPort(), m.toXML());
                        sendToMonitor(m.toXML());
                        // Esperamos respuesta a nuestra oferta
                        System.out.println("ESPERAMOS RESPUESTA");
                        Message response = this.responseQ.poll(this.timeout, TimeUnit.MILLISECONDS);

                        // Si hay respuesta, la atendemos
                        if (response != null) {
                            // Si la respuesta es que le interesa, empezamos a negociar con ese agente
                            if (response.getProtocol().equalsIgnoreCase("meInteresa")){
                                busy.set(true);     // Marcamos que está ocupado
                                this.tradeLock.lock();
                                System.out.println("LE INTERESA");
                                this.negotiationId = k;
                                this.tradeLock.unlock();
                                this.negociar(k, true);   // Negociamos
                                this.negotiationQ.clear(); // Limpiamos la lista de cualquier mensaje que pueda quedar
                                // Si no quedan ofertas iniciales que atender, deja de estar ocupado.
                                if(this.tradeQ.isEmpty()){ this.busy.set(false); }
                            }else{
                                System.out.println("NO LE INTERESA");
                            }
                        }

                    // Si está ocupado le toca mirar ofertas iniciales
                    }else{
                        while(!this.tradeQ.isEmpty()){
                            Message offer = this.tradeQ.poll(this.timeout, TimeUnit.MILLISECONDS);
                            // Si decidimos aceptarla
                            if (this.decidirOfertaInicial(offer)){
                                AgentKey leBron = new AgentKey(offer.getOriginIp(), offer.getOriginPortTCP());

                                // ENVIAMOS MENSAJE ME INTERESA
                                Message response = createMessage(null, "1", "meInteresa",
                                        1, "TCP", leBron);
                                response.addInfoMonitor((int)felicidad,album.getSetsCompletados(),album.tengo.size());
                                sendMessage(leBron.getIpString(), leBron.getPort(), response.toXML());
                                sendToMonitor(response.toXML());

                                // NOS PONEMOS A NEGOCIAR
                                this.tradeLock.lock();
                                this.negotiationId = leBron;
                                this.tradeLock.unlock();
                                this.negociar(leBron, false);
                                this.negotiationQ.clear(); // Limpiamos la lista de cualquier mensaje que pueda quedar
                                // YA NO ESTAMOS OCUPADOS
                                busy.set(false);
                            }else{
                                AgentKey leBron = new AgentKey(offer.getOriginIp(), offer.getOriginPortTCP());
                                // ENVIAMOS MENSAJE NO ME INTERESA
                                Message response = createMessage(null, "sis", "noMeInteresa",
                                        1, "TCP", k);
                                response.addInfoMonitor((int)felicidad,album.getSetsCompletados(),album.tengo.size());
                                sendMessage(leBron.getIpString(), leBron.getPort(), response.toXML());
                                sendToMonitor(response.toXML());
                                // YA NO ESTAMOS OCUPADOS
                                busy.set(false);
                            }
                        }
                    }
                }
            }
        }
    }

    // Función para mandar mensajes
    public void sendMessage() throws IOException {
        // Usamos un BufferedReader sin cerrarlo para evitar cerrar System.in
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // Pedir al usuario la IP de destino
        System.out.print("Enter target IP: ");
        String targetIp = reader.readLine();

        // Pedir el puerto de destino
        System.out.print("Enter target port: ");
        int targetPort = Integer.parseInt(reader.readLine());

        System.out.print("Enter message type: ");
        String messageType = reader.readLine();

        long originTime = System.currentTimeMillis();

        Message m = createMessage(null, "1", "messageType", 1, "TCP", new AgentKey(targetIp, targetPort));

        // System.out.println(m.toString());

        // Llamar a sendMessage para enviar el mensaje
        sendMessage(targetIp, targetPort, m.toXML());
    }
        /*if(!pausado) {


            this.trade_counter++;
            actualizarFelicdad();

            System.out.println(this.album);

            Cromo cromo1 = album.COLECCION.get(0);
            Cromo cromo2 = album.COLECCION.get(1);
            Cromo cromo3 = album.COLECCION.get(2);
            Cromo cromo4 = album.COLECCION.get(3);
            Cromo cromo5 = album.COLECCION.get(4);
            Cromo cromo15 = album.COLECCION.get(14);
            Cromo cromo10 = album.COLECCION.get(9);

            album.consigo(cromo1);
            album.consigo(cromo2);
            album.consigo(cromo3);
            album.consigo(cromo4);
            album.consigo(cromo15);

            System.out.println(this.album);
            System.out.println(this.felicidad);

            System.out.println(this.album.evaluarIntercambio(cromo15,cromo10));

        }else{
            System.out.println("\nEl agente esta parado y por lo tanto la funcion del agente tambien.\n");
        }
    }*/

    //Descubre agentes por fuerza bruta
    public void findAgents() {
        //String discoveryMessage = "DISCOVERY_REQUEST";
        while (true) {
            candado();
            try {

                for (int i = 0; i < ipList.size() ; i++) {

                    String address = ipList.get(i);

                    // Bucle para enviar mensajes a puertos impares en el rango
                    for (int port = 4001; port <= 4101; port += 2) {

                        if (port != this.udpPort || !address.equals(ip)) {

                            long originTime = System.currentTimeMillis();

                            // Si tenemos el agente en nuestra lista le quitamos 1 a su ttl
                            AgentKey k = new AgentKey(address, port);
                            if (discoveredAgents.containsKey(k)) {
                                discoveredAgents.get(k).searched();
                                // Si con su nuevo ttl lo consideramos muerto, lo eliminamos
                                if (!discoveredAgents.get(k).alive()) {
                                    discoveredAgents.remove(k);
                                }
                            }

                            // Creamos mensaje de descubrimiento
                            Message discoveryMessage = createMessage(null, "1", "hola", 1, "UDP", k);
                            String xmlString = discoveryMessage.toXML();

                            byte[] messageData = xmlString.getBytes(StandardCharsets.UTF_8);

                                // Crear un paquete UDP con el mensaje de descubrimiento
                                DatagramPacket packet = new DatagramPacket(
                                        messageData, messageData.length, InetAddress.getByName(address), port);
                            for (int j = 0; j < 50; j++) {
                                // Enviar el paquete de descubrimiento
                                datagramSocket.send(packet);
                            }

                        }
                    }
                }


                Thread.sleep(200);

            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    //Método de escucha para mensajes de descubrimiento de agentes
    //Si es una request le responde con sus datos.
    //Si es una response lo registra
    public void listenForUdpMessages() {
        byte[] buffer = new byte[1024];
    
        try {
            System.out.println("Listening for UDP messages...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    
                // Recibir un paquete UDP
                datagramSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                InetAddress senderAddress = packet.getAddress();
                int senderPort = packet.getPort();

                // Procesar el mensaje recibido
                if(validate(message)) {
                    Message m = new Message(message);
                    String i = m.getOriginId();
                    if(Objects.equals(m.getProtocol(), "hola")){
                        registerAgent(senderAddress,senderPort, i);
                        handleDiscoveryRequest(senderAddress, senderPort);
                    }if(Objects.equals(m.getProtocol(), "estoy")){
                        registerAgent(senderAddress,senderPort, i);
                    }
                }else{
                    System.out.println("El mensaje no ha sido validado");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public void handleDiscoveryRequest(InetAddress requesterAddress, int requesterPort) {
        try {
            // Creamos un mensaje estoy
            AgentKey k = new AgentKey(requesterAddress.getHostName(), requesterPort);
            Message m = createMessage(null,"1", "estoy", 1, "UDP", k);
            String responseMessage = m.toXML();

            byte[] responseData = responseMessage.getBytes(StandardCharsets.UTF_8);
    
            DatagramPacket responsePacket = new DatagramPacket(
                responseData, responseData.length, requesterAddress, requesterPort);
            for (int i = 0; i < 50 ; i++) {
                datagramSocket.send(responsePacket);
            }

            //System.out.println("Sent discovery response to " + requesterAddress + ":" + requesterPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerAgent(InetAddress agentAddress, int serverPort, String i) {
        serverPort = serverPort - 1;
        String agentIp = agentAddress.getHostAddress();
        AgentKey k = new AgentKey(agentIp, serverPort);
        AgentInfo v = new AgentInfo(i);
    
        if (!discoveredAgents.containsKey(k)) {
            discoveredAgents.put(k, v);
            //System.out.println("Registered new agent: " + agentInfo);
        } else {
            // La id no es la misma, es otro agente
            if(!discoveredAgents.get(k).getId().equals(i)){
                AgentInfo other = new AgentInfo(i);
            }else {
                discoveredAgents.get(k).answered();
            }
            //System.out.println("Agent already registered: " + agentInfo);
        }
    }

    public void interpretarTipoMensaje(String tipo, String id, Message m) throws InterruptedException {
        if (!this.pausado || tipo.equals("continua")) {
            switch (tipo) {
                case "parate"                       -> this.parar();
                case "continua"                     -> this.continuar();
                case "autodestruyete"               -> this.autodestruccion();
                case "reproducete"                  -> this.reproducirse();
                case "ofertaInicial"                -> this.tratarOfertaInicial(m);
                case "meInteresa", "noMeInteresa"   -> this.tratarRespuesta(m);
                case "intercambio"                  -> this.tratarIntercambio(m);
                case "decision"                     -> this.tratarDecision(m);
                default -> System.out.println("Tipo de mensaje no implementado: " + tipo);
            }
        }else{
            System.out.println("\nEl agente esta parado, actualmente la unica accion que recibe es continuar\n" );
        }
    }

    public void tratarOfertaInicial(Message m) throws InterruptedException {
        Thread.sleep(100);
        AgentKey k = new AgentKey(m.getOriginIp(), m.getOriginPortTCP());
        System.out.println("LLEGA OFERTA DE "+m.getOriginId()+" EN "+k);
        // Si no está ocupado, que se ocupe el thread de negociación
        if (!busy.get()){
            // Lo marcamos como ocupado y le pasamos la oferta
            busy.set(true);
            this.tradeQ.add(m);

            // Si está ocupado, rechazamos la oferta
        } else {
            System.out.println("OCUPADO, RECHAZAMOS OFERTA");
            Message reject = createMessage(m.getComId(), "RECHAZAO ", "noMeInteresa",
                    1, "TCP", k);
        }
    }

    public void tratarRespuesta(Message m){;
        this.responseQ.add(m);
    }

    public void tratarIntercambio(Message m) throws InterruptedException {
        // Sólo lo encolamos si estamos negociando con él
        Thread.sleep(100);
        this.negotiationQ.add(m);

    }

    public void tratarDecision(Message m) throws InterruptedException {
        // Sólo lo encolamos si estamos negociando con él
        Thread.sleep(100);
        this.negotiationQ.add(m);
    }

    public void reproducirse() {
        try {
            // Obtener el bean del sistema operativo para comprobar la carga del sistema
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

            // Obtener la carga de CPU
            double cpuLoad = osBean.getCpuLoad();

            System.out.println("Carga de la CPU:" + cpuLoad);

            // Obtener memoria física total y libre
            long totalMemory = osBean.getTotalMemorySize();
            long freeMemory = osBean.getFreeMemorySize();

            System.out.println("% memoria libre:" + (double) freeMemory / totalMemory);

            // Calcular el porcentaje de memoria libre
            double freeMemoryPercentage = (double) freeMemory / totalMemory;

            // Definir umbrales
            double cpuThreshold = 0.80; // 80%
            double memoryThreshold = 0.20; // 20%

            // Comprobar si se superan los umbrales
            if (cpuLoad >= cpuThreshold) {
                System.out.println("No se puede lanzar una nueva instancia. La carga de CPU es demasiado alta: " + (cpuLoad * 100) + "%");
                return;
            }

            if (freeMemoryPercentage <= memoryThreshold) {
                System.out.println("No se puede lanzar una nueva instancia. La memoria disponible es insuficiente: " + (freeMemoryPercentage * 100) + "% libre");
                return;
            }

            // Si los recursos son suficientes, lanzar un nuevo agente
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classPath = System.getProperty("java.class.path");
            String className = this.getClass().getName();

            ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classPath, className);

            // Aumenta su número de hijos y le pasa a su hijo la ID que usará
            this.num_sons ++;
            String childID = this.id + "_" + this.num_sons;
            builder.command().add(childID);

            //AVISAR AL MONITOR

            builder.start();

            System.out.println("Nueva instancia del agente lanzada exitosamente.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parar(){
        // Manda al monitor mensaje heParado
        Message m = createMessage(null, "1", "parado", 1, "TCP", monitor_key);
        sendToMonitor(m.toXML());

        // Con enfoque de variable global y continue en los métodos de escucha
        synchronized (monitor_stop) {
            pausado = true;
            System.out.println("\nAgente parado.\n");
        }
    }

    private void continuar(){

        //con enfoque de variable en el agente y continue en los metodos de escucha
        synchronized (monitor_stop) {
            pausado = false;
            monitor_stop.notifyAll();
            System.out.println("\nEl agente va a continuar.\n");
        }

        Message m = createMessage(null, "1", "continua", 1, "TCP", monitor_key);
        sendToMonitor(m.toXML());
    }

    private void candado(){
        try{
            synchronized (monitor_stop) {
                while (pausado) {
                    monitor_stop.wait();
                }
            }}catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public void autodestruccion() {
        System.out.println("El agente se esta autodestruyendo...");
    
        // Detener el servidor de escucha
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server socket closed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cerrar el DatagramSocket para UDP
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
            System.out.println("Datagram socket closed.");
        }

        // Avisar al Monitor de que el agente muere;
        Message message = createMessage(null, "1","meMuero", 1, "TCP", monitor_key);
        sendToMonitor(message.toXML());

        // Esperamos un poco para que el mensaje se mande correctamente que si no puede dar problemas con la gestion del socket
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        // Detener el proceso del agente
        System.exit(0); // Termina el programa
    }

    // Método de validación
    public static boolean validate(String xmlContent) {
        String xsdFilePath = "cambiaCromosProyecto/src/XMLParser/esquema.xsd";  // Ruta al archivo XSD

        // Crear una fábrica de esquemas que entienda XSD
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        try {
            // Cargar el esquema XSD
            Schema schema = schemaFactory.newSchema(new File(xsdFilePath));

            // Crear una instancia de Validator
            Validator validator = schema.newValidator();

            // Validar el contenido XML (desde la cadena) contra el esquema
            validator.validate(new StreamSource(new StringReader(xmlContent)));
            //System.out.println("XML es válido contra el XSD.");
            return true;

        } catch (Exception e) {
            System.out.println("XML no es válido: " + e.getMessage());
            return false;
        }
    }

    // Método para crear un mensaje de cero
    public Message createMessage(String comId, String msgId, String protocol, int protocolStep, String comProtocol,
                                        AgentKey k) {
        LocalTime time = LocalTime.now();
        String destId;
        // COMO EL MONITOR NO LO TENEMOS REGISTRADO EN AGENTES DESCUBIERTOS, DEBEMOS TRATARLO APARTE
        if(k.equals(this.monitor_key)){
            destId = "MONITOR";
        // SI TENEMOS LA LLAVE EN NUESTRA LISTA
        }else if (this.discoveredAgents.containsKey(k)){
            destId = this.discoveredAgents.get(k).getId();
        // SKIBIDI GYAT
        } else{ destId = "Skibidi Gyat"; }

        String cId;
        if(comId == null){
            cId = this.ip+":"+this.id+" to "+k.getIpString()+":"+destId+" started "+time.toString();
        }else { cId = comId; }
        // EL IDENTIFICADOR DE COMUNICACIÓN SERÁ BASADO EN DIRECCIÓN IP + ID DE LOS DOS AGENTES INVOLUCRADOS + TIEMPO

        try {
            Message m = new Message(cId, msgId, protocol, protocolStep, comProtocol,
                        this.id, this.ip, this.serverPort, this.udpPort, time.toString(),
                        destId, k.getIpString(), k.getPort(), k.getPort()+1, "N/A");
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getNum(String filepath){
        try{
            // Usamos esta clase que tiene acceso aleatorio en vez de las tradicionales con acceso secuencial
            // (BufferedReader y BufferedWriter p. ej.)
            // porque de esta manera podemos leer y actualizar el número de agente con una sola clase.
            RandomAccessFile file = new RandomAccessFile(filepath, "rw");
            FileChannel chn = file.getChannel();
            String s;
            int num;
            // Bloquea archivo
            FileLock lock = chn.lock();

            try{
                // Lee el número
                s = file.readLine();
                num = Integer.parseInt(s);
                // Actualiza el número del archivo
                file.setLength(0);
                file.writeBytes(String.valueOf(num+1));
                // Devuelve numero
                return num;
            }finally {
                // Desbloquea archivo
                lock.release();
            }

        }catch(IOException e){
            e.printStackTrace();
            return 0;
        }
    }

    ////////////////////////////////////

    ///   PARTE DE FUNCION AGENTE   //

    /////////////////////////////////

    // Función para decidir si aceptar una oferta
    public boolean decidirOfertaInicial(Message otherMsg){

        ArrayList<Cromo> otherWants = new ArrayList<>();
        ArrayList<Cromo> otherOffers = new ArrayList<>();

        ArrayList<Cromo> myTake = new ArrayList<>();
        ArrayList<Cromo> myGive = new ArrayList<>();

        // Memorizamos listas de deseados y ofrecidos del otro agente
        for(int i : otherMsg.getWanted()){ otherWants.add(this.album.COLECCION.get(i-1)); }
        for(int i : otherMsg.getOffered()){ otherOffers.add(this.album.COLECCION.get(i-1)); }

        // Miramos intersecciones entre nuestros cromos deseados y los suyos ofrecidos
        // Esto tiene un tímido O(n^2), pero no me da la vida pa cambiarlo a un hashmap con id pa reducirlo a O(n)

        // Se supone que listas están ordenadas según el valor calculado, por lo que el resultado lo estará también
        // Mayor a menor
        for(Cromo mW : this.album.lista_deseados){ if(otherOffers.contains(mW)) { myTake.add(mW); } }
        // Menor a mayor
        for(Cromo mO : this.album.lista_ofrezco){ if(otherWants.contains(mO)) { myGive.add(mO); } }

        // Si alguna de las intersecciones está vacía, se rechaza el intercambio
        if(myGive.size() == 0 || myTake.size() == 0){
            System.out.println("RECHAZAMOS");
            return false;
        }else{
            System.out.println("ACEPTAMOS");
            return true;
        }

    }

    // Función para negociar con un agente
    public void negociar(AgentKey k, boolean empiezo) throws InterruptedException {

        /*
        * 1. LOS AGENTES SE MANDAN EL UNO AL OTRO SUS ÁLBUMES ENTEROS
        * 2. CREAN INTERSECCIONES:
        *       INTERSECCIÓN CROMOS QUE QUIERES CON CROMOS QUE OFRECE EL OTRO
        *       INTERSECCIÓN CROMOS QUE OFRECES CON CROMOS QUE QUIERE EL OTRO
        * 3.
         */

        /*
         * SI ALGUNA DE ESAS DOS COSAS NO TIENE ELEMENTOS NO HAY INTERESES COMUNES Y SE CANCELA EL INTERCAMBIO
         * LUEGO LA IDEA ES QUE CADA UNO DE LOS AGENTES PASE UNA CARTA QUE OFRECE Y OTRA QUE QUIERE, Y ASÍ SE VAN EVALUANDO
         * CADA UNO VA CEDIENDO MÁS CADA ITERACIÓN (OFRECIENDO UNA MEJOR SI NUMERO PAR, PIDIENDO UNA PEOR SI ITERACIÓN ES IMPAR)
         */

        System.out.println("\n");
        System.out.println("-------------------------------------------------------");
        System.out.println("EMPEZANDO NEGOCIACIÓN CON AGENTE "+this.negotiationId);
        System.out.println("-------------------------------------------------------");
        System.out.println("\n");

        // ESTAS VARIABLES SE USARÁN PARA SABER QUÉ TE TOCA HACER ESTA NEGOCIACIÓN
        int n = 0;  // Contador, se usará para hacer resto = n%2
        int c;      // Me tocará cuando resto == c
        if(empiezo){ c = 0; }else{ c = 1; }

        Message myMsg;
        Message otherMsg;

        int msgId;
        int protocolStep;
        String comId;

        boolean skibidi = true;         // Booleano para saber cuando acabar la negociación;

        // SI EMPIEZO PRIMERO COMPARTO MI MENSAJE Y LUEGO ESPERO AL DEL OTRO MENSAJE
        if(empiezo){
            msgId = 1;
            protocolStep = 1;

            // Crear y mandar mensaje
            myMsg = createMessage(null, Integer.toString(msgId), "intercambio", protocolStep,
                    "TCP", k);
            myMsg.addTrade(this.album.lista_deseados, this.album.lista_ofrezco, this.G, 0);
            myMsg.addInfoMonitor((int) felicidad,album.getSetsCompletados(),album.tengo.size());
            sendMessage(k.getIpString(), k.getPort(), myMsg.toXML());
            sendToMonitor(myMsg.toXML());
            comId = myMsg.getComId();
            otherMsg = this.negotiationQ.poll(this.timeout, TimeUnit.MILLISECONDS);
            if (otherMsg == null){
                tradeLock.lock();
                this.negotiationId = null;
                tradeLock.unlock();
                System.out.println("ME SALGO 1");
                return;
            }
            msgId = Integer.parseInt(otherMsg.getMsgId())+1;
            protocolStep = otherMsg.getProtocolStep()+1;

        // SI NO EMPIEZO PRIMERO LEO EL MENSAJE DEL OTRO AGENTE Y LUEGO CONSTRUYO Y ENVÍO EL MÍO
        }else{
            otherMsg = this.negotiationQ.poll(this.timeout, TimeUnit.MILLISECONDS);
            if (otherMsg == null){
                tradeLock.lock();
                this.negotiationId = null;
                tradeLock.unlock();
                System.out.println("ME SALGO 2");
                return;
            }
            msgId = Integer.parseInt(otherMsg.getMsgId())+1;
            protocolStep = otherMsg.getProtocolStep()+1;
            comId = otherMsg.getComId();

            // Crear y mandar mensaje
            myMsg = createMessage(comId, Integer.toString(msgId), "intercambio", protocolStep,
                    "TCP", k);
            myMsg.addTrade(this.album.lista_deseados, this.album.lista_ofrezco, this.G, 0);
            myMsg.addInfoMonitor((int)felicidad,album.getSetsCompletados(),album.tengo.size());
            sendMessage(k.getIpString(), k.getPort(), myMsg.toXML());
            sendToMonitor(myMsg.toXML());
        }

        // Lista de deseados del otro agente
        ArrayList<Cromo> otherWants = new ArrayList<>();
        // Lista de ofrecidos del otro agente
        ArrayList<Cromo> otherOffers = new ArrayList<>();

        // Lista de orden ascendente que contendrá los cromos que podemos dar
        LinkedList<Cromo> myGive = new LinkedList<>();
        // Lista de orden descendente que contendrá los cromos que podemos tomar
        LinkedList<Cromo> myTake = new LinkedList<>();

        // Memorizamos listas de deseados y ofrecidos del otro agente
        for(int i : otherMsg.getWanted()){ otherWants.add(this.album.COLECCION.get(i-1)); }
        for(int i : otherMsg.getOffered()){ otherOffers.add(this.album.COLECCION.get(i-1)); }

        // Miramos intersecciones entre nuestros cromos deseados y los suyos ofrecidos
        // Esto tiene un tímido O(n^2), pero no me da la vida pa cambiarlo a un hashmap con id pa reducirlo a O(n)

        // Se supone que listas están ordenadas según el valor calculado, por lo que el resultado lo estará también
        // Mayor a menor
        for(Cromo mW : this.album.lista_deseados){ if(otherOffers.contains(mW)) { myTake.add(mW); } }
        // Menor a mayor
        for(Cromo mO : this.album.lista_ofrezco){ if(otherWants.contains(mO)) { myGive.add(mO); } }

        System.out.println("-------------------------------------------------------");
        System.out.println("INTERESES COMUNES:");
        System.out.println("YO TOMO: "+myTake);
        System.out.println("YO DOY: "+myGive);
        System.out.println("-------------------------------------------------------");
        System.out.println();

        // CROMOS CON LOS QUE NEGOCIAMOS AHORA MISMO
        Cromo give = myGive.poll();
        Cromo take = myTake.poll();
        Cromo ofertaDar = null;
        Cromo ofertaTomar = null;

        // COMIENZA EL BUCLE LESS GO
        while(skibidi){

            // TURNO DE ENVIAR MENSAJE
            if(n%2 == c){

                // PRIMERO MIRAREMOS LA OFERTA PREVIA Y DECIDIREMOS SI ACEPTARLA
                if (ofertaDar != null && ofertaTomar != null){ // Miramos que existan (en el primer envío no existen)
                    // Evaluamos la oferta recibida, si nos gusta el intercambio la aceptamos.
                    if(this.album.evaluarIntercambio(ofertaDar, ofertaTomar)){
                        // Actualizar cosas del album
                        this.album.consigo(ofertaTomar);
                        this.album.quito(ofertaDar);
                        this.trade_counter++;
                        this.terminarIntercambio(true, k, msgId, comId);
                        return;
                    }
                }

                // VA ALTERNANDO ENTRE PEDIR CROMOS LIGERAMENTE PEORES Y OFRECER CROMOS LIGERAMENTE MEJORES
                if(n%4 < 2){ give = myGive.poll(); }else{ take = myTake.poll(); }

                // CANCELAMOS INTERCAMBIO EN DOS CONDICIONES
                // 1. NO QUEDAN OFERTAS QUE HACER
                // 2. NO VALE LA PENA HACER MÁS OFERTAS (LAS EVALUAMOS COMO FALSE)
                // SI SE CUMPLE ALGUNA DE ELLAS CANCELAMOS
                if (give == null || take == null){
                    this.terminarIntercambio(false, k, 1, comId);
                    return;
                }

                if(!this.album.evaluarIntercambio(give, take)){
                    this.terminarIntercambio(false, k, 1, comId);
                    return;
                }

                // MODIFICAMOS Y ENVIAMOS OFERTA
                ArrayList<Cromo> w = new ArrayList<>(); w.add(take);
                ArrayList<Cromo> o = new ArrayList<>(); o.add(give);
                myMsg.addTrade(w, o, this.G, 0);
                myMsg.addInfoMonitor((int) felicidad,album.getSetsCompletados(),album.tengo.size());
                this.sendMessage(k.getIpString(), k.getPort(), myMsg.toXML());
                this.sendToMonitor(myMsg.toXML());
                System.out.println("-------------------------------------------------------");
                System.out.println("HE ENVIADO:");
                System.out.println(myMsg);
                System.out.println();

            // TURNO DE MIRAR MENSAJE
            }else{
                // Miramos mensaje
                otherMsg = this.negotiationQ.poll(this.timeout, TimeUnit.MILLISECONDS);

                // Si no obtenemos mensaje asumimos que ha muerto (D.E.P.)
                if (otherMsg == null){
                    tradeLock.lock();
                    this.negotiationId = null;
                    tradeLock.unlock();
                    System.out.println("ME SALGO 3");
                    return;
                }
                System.out.println("-------------------------------------------------------");
                System.out.println("HE RECIBIDO");
                System.out.println(otherMsg);
                System.out.println();

                // Actualizamos msgId y protocolStep
                msgId = Integer.parseInt(otherMsg.getMsgId())+1;
                protocolStep = otherMsg.getProtocolStep()+1;

                // Miramos si es decisión,  hacemos lo que debamos
                if(otherMsg.getProtocol().equals("decision")){
                    // Si acepta
                    if(otherMsg.getDecision()){
                        // Actualizar cosas del album
                        this.album.consigo(take);
                        this.album.quito(give);
                        this.trade_counter++;
                        System.out.println("******************************************************");
                        System.out.println("*****************INTERCAMBIO ACEPTADO*****************");
                        System.out.println("******************************************************");
                        actualizarFelicdad();
                        Random random = new Random();
                        int randomNumber = random.nextInt(100) + 1;
                        if (felicidad >= 75 && randomNumber >= 50){
                            reproducirse();
                        }
                    }else{
                        System.out.println("******************************************************");
                        System.out.println("*****************INTERCAMBIO DENEGADO*****************");
                        System.out.println("******************************************************");
                        this.trade_counter--;
                        actualizarFelicdad();
                        Random random = new Random();
                        int randomNumber = random.nextInt(100) + 1;
                        if (felicidad <= 25 && randomNumber >= 50){
                            autodestruccion();
                        }
                    }

                    tradeLock.lock();
                    this.busy.set(false);
                    this.negotiationId = null;
                    tradeLock.unlock();
                    System.out.println("");
                    return;
                }

                // SI LA EJECUCIÓN LLEGA AQUÍ, ES TIPO "intercambio" Y MIRAMOS LA OFERTA QUE NOS LLEGA
                ofertaDar = this.album.COLECCION.get(otherMsg.getWanted().get(0)-1);
                ofertaTomar = this.album.COLECCION.get(otherMsg.getOffered().get(0)-1);

            }
            n++;

        }

        /*
        * TODO: NO PODREMOS IMPLEMENTAR INTERCAMBIO DE VARIAS CARTAS SIMULTÁNEAMENTE CON NUESTRA IMPLEMENTACIÓN ACTUAL
        *   (en verdad podríamos hacer un evaluate por cada par de cromos (doy, tomo) y tomar la clase mayoritaria)
        *   (pero que pereza en verdad)
        *   SUGIERO LA SIGUIENTE IMPLEMENTACIÓN:
        *   1. CAMBIAR MÉTODO EVALUAR DE EVALUAR UN PAR DE CROMOS A EVALUAR EL CAMBIO DE VALOR RESULTANTE
        *       AL DAR O RECIBIR UN CROMO (LLAMAR PARA CADA CROMO OFRECIDO Y PEDIDO Y DECIDIDR EN BASE A CAMBIO VALOR FINAL)
        *   2. CON EL CAMBIO DE VALOR FINAL (+10 P.EJ.) YA DECIDIR
        */

    }

    public void terminarIntercambio(boolean d, AgentKey k, int mId, String cId) {
        if(d){
            System.out.println("******************************************************");
            System.out.println("*****************INTERCAMBIO ACEPTADO*****************");
            System.out.println("******************************************************");
            Random random = new Random();
            int randomNumber = random.nextInt(100) + 1;
            if (felicidad >= 75 && randomNumber >= 50){
                reproducirse();
            }
        }else{
            System.out.println("******************************************************");
            System.out.println("*****************INTERCAMBIO DENEGADO*****************");
            System.out.println("******************************************************");
            Random random = new Random();
            int randomNumber = random.nextInt(100) + 1;
            if (felicidad <= 25 && randomNumber >= 50){
                autodestruccion();
            }
        }
        Message m = createMessage(cId, Integer.toString(mId), "decision", 1, "TCP", k);
        m.addDecision(d);
        m.addInfoMonitor((int)felicidad,album.getSetsCompletados(),album.tengo.size());
        this.sendMessage(k.getIpString(), k.getPort(), m.toXML());
        this.sendToMonitor(m.toXML());
        tradeLock.lock();
        this.busy.set(false);
        this.negotiationId = null;
        actualizarFelicdad();
        tradeLock.unlock();
    }

    public void actualizarFelicdad() {

        // Función elegida para suavizar el número de intercambios: raíz cuadrada
        double valor = regularizacion_incremento_album * (this.album.valorTotal - initial_album_value) + regularizacion_numero_intercambios * Math.sqrt(this.trade_counter);
        // Función logística para mantener felicidad entre 0 y 100, con 50 como punto base.
        this.felicidad  = 100 / (1 + Math.exp(-valor));
        System.out.println("Actualizando felicidad " + this.felicidad);
    }

    public void check_g() {
        if (this.felicidad < 35) {
            this.G = true;
        }else{
            this.G = false;
        }
    }

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        String agentID;

        // Si tenemos un ID en args, el agente es hijo
        if(args.length > 0){
            // Es hijo, usaremos el argumento como ID
            agentID = args[0];
        }else{
            // No es hijo de ningún agente, construimos su ID a partir de un fichero.
            String filepath = "cambiaCromosProyecto/src/agente/num_ag.txt";
            int num = getNum(filepath);
            agentID = "AG_"+ num;
        }
        System.out.println(agentID);

        Agent agent = new Agent(agentID);
        System.out.println("Agent is running correctly:");

        try {
            Thread.sleep(1000); // Pausa de 1 segundo (1000 milisegundos)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        agent.funcionDelAgente();

        // Leer comandos desde la consola
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String command;
            while (true) {
                // Mostrar prompt de terminal
                System.out.print(">> ");

                // Leer el comando
                command = reader.readLine();
                if (command == null) break;
                // Procesar el comando
                if (command.equalsIgnoreCase("status")) {
                    agent.reporteEstado();
                } else if (command.equalsIgnoreCase("exit")) {
                    agent.autodestruccion();
                    break; // Salir del bucle tras autodestrucción
                } else if (command.equalsIgnoreCase("funcionagente")) {
                    agent.funcionDelAgente();
                } else if (command.equalsIgnoreCase("reproducete")){
                    agent.reproducirse();
                } else if (command.equalsIgnoreCase("continua")){
                    agent.continuar();
                } else if (command.equalsIgnoreCase("send")){
                    agent.sendMessage();
                }
                else {
                    System.out.println("Unknown command. Available commands: 'status', 'send', 'exit'");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}   

