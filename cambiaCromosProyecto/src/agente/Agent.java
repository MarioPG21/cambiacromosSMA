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
    private Condition tradeCondition = tradeLock.newCondition();    // Condición por la que se esperará a nuevas ofertas
    private AtomicBoolean busy = new AtomicBoolean(false);                                     // Bool que define si el agente está ocupado
    // Cola que se usará para esperar otro mensaje durante la negociación
    LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private String negId = "";                                      // Id del agente con el que se está negociando

    //Atributos funcion del agente
    Random random = new Random();
    private int S = random.nextInt(41) + 60;
    private boolean G = false;
    private Album album = new Album(60,S);
    private double initial_album_value = album.valorTotal;

    private double felicidad = 50;

    //Subir cada vez que se realice un intercambio
    // TODO: concretar funcionamiento de esto, ahora mismo está sólo con los intercambios exitosos
    private int trade_counter = 0;

    private double regularizacion_incremento_album = 0.05;
    private double regularizacion_numero_intercambios = 0.05;

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

    // Método para obtener el ID
    public String getId() {
        return id;
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
                // Le envía una petición de mensaje de intercambio a cada uno de los agentes que hay en su lista
                for(AgentKey k : this.discoveredAgents.keySet()){
                    System.out.println("Amimir: Enviando mensaje a "+k);
                    Thread.sleep(2000);

                    // Creamos mensaje y le pasamos nuestra información de intercambio
                    // (deseados, ofrecidos, G, rupias)
                    Message m = createMessage(null, "1", "intercambio", 1, "TCP", k);
                    m.addTrade(this.album.lista_deseados, this.album.lista_ofrezco, false, 0);
                    m.addInfoMonitor((int) felicidad,album.getSetsCompletados(),album.tengo.size());
                    sendMessage(k.getIpString(), k.getPort(), m.toXML());
                    sendToMonitor(m.toXML());
                    // Si nos llega la notificación de que alguien ha devuelto un mensaje de negociación
                    // Negociamos y actualizamos felicidad y g

                    if(busy.get()){ this.negociar(); this.actualizarFelicdad(); this.check_g();
                        System.out.println("Negociando");}

                }
            }
        }
    }

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

    public void interpretarTipoMensaje(String tipo, String id, Message m) {
        if (!this.pausado || tipo.equals("continua")) {
            switch (tipo) {
                case "parate" -> this.parar();
                case "continua" -> this.continuar();
                case "autodestruyete" -> this.autodestruccion();
                case "reproducete" -> this.reproducirse();
                case "intercambio" -> {
                    System.out.println("Mensaje intercambio");
                    this.intercambiar(id, m); // Llama a intercambiar
                }
                case "decision" -> {
                    System.out.println("Mensaje Decision");
                    this.intercambiar(id, m);    // Llama a intercambiar
                }
                default -> System.out.println("Tipo de mensaje no implementado: " + tipo);
            }
        }else{
            System.out.println("\nEl agente esta parado, actualmente la unica accion que recibe es continuar\n" );
        }
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

    // Función para negociar con un agente
    public void negociar() throws InterruptedException {

        /*
         * EXPLICACIÓN DE LA LÓGICA
         * AL PRINCIPIO LOS AGENTES SE GUARDAN TODAS LAS CARTAS QUE OFRECE Y QUIERE EL OTRO (PASAN LA LISTA ENTERA)
         * LA PRIMERA ITERACIÓN SE MIRA:
         *   1. INTERSECCIÓN CROMOS QUE QUIERES CON CROMOS QUE OFRECE EL OTRO
         *   2. INTERSECCIÓN CROMOS QUE OFRECES CON CROMOS QUE QUIERE EL OTRO
         * SI ALGUNA DE ESAS DOS COSAS NO TIENE ELEMENTOS NO HAY INTERESES COMUNES Y SE CANCELA EL INTERCAMBIO
         * LUEGO LA IDEA ES QUE CADA UNO DE LOS AGENTES PASE UNA CARTA QUE OFRECE Y OTRA QUE QUIERE, Y ASÍ SE VAN EVALUANDO
         * CADA UNO VA CEDIENDO MÁS CADA ITERACIÓN (OFRECIENDO UNA MEJOR SI NUMERO PAR, PIDIENDO UNA PEOR SI ITERACIÓN ES IMPAR)
         */


        int negotiationCounter = 0;     // Contador de negociaciones
        boolean skibidi = true;         // Booleano para saber cuando acabar la negociación;

        // Lista de deseados del otro agente
        ArrayList<Cromo> negWanted = new ArrayList<>();
        // Lista de ofrecidos del otro agente
        ArrayList<Cromo> negOffered = new ArrayList<>();

        // Lista de orden ascendente, ya que nos interesa dar más las cartas con menos valor
        LinkedList<Cromo> give = new LinkedList<>();
        // Lista de orden descendente, nos interesa más llevarnos las cartas más valiosas
        LinkedList<Cromo> take = new LinkedList<>();

        Message m = this.queue.poll(20, TimeUnit.SECONDS);

        // Si no obtenemos mensaje asumimos que ha muerto (D.E.P.)
        if(m == null){
            tradeLock.lock();
            this.busy.set(false);
            this.negId = "";
            tradeLock.unlock();
            return;
        }

        // Info de comunicación
        System.out.println("EL OTRO PRINGAO ES ESTE PAVO");
        System.out.println(m.getOriginIp());
        System.out.println(m.getOriginPortTCP());
        AgentKey k = new AgentKey(m.getOriginIp(), m.getOriginPortTCP());
        System.out.println(this.discoveredAgents.containsKey(k));
        int msgId = Integer.parseInt(m.getMsgId())+1;
        int prStep = m.getProtocolStep()+1;

        // Plantilla de los mensajes que iremos mandando
        Message myMessage = createMessage(m.getComId(), Integer.toString(msgId), "intercambio",
                prStep, "TCP", k);

        // Memorizamos listas de deseados y ofrecidos del otro agente
        for(int i : m.getWanted()){ negWanted.add(this.album.COLECCION.get(i-1)); }
        for(int i : m.getOffered()){ negOffered.add(this.album.COLECCION.get(i-1)); }

        // Miramos intersecciones entre nuestros cromos deseados y los suyos ofrecidos
        // Esto tiene un tímido O(n^2), pero no me da la vida pa cambiarlo a un hashmap con id pa reducirlo a O(n)

        // Se supone que listas están ordenadas según el valor calculado, por lo que el resultado lo estará también
        // Mayor a menor
        for(Cromo mW : this.album.lista_deseados){ if(negOffered.contains(mW)) { take.add(mW); } }
        // Menor a mayor
        for(Cromo mO : this.album.lista_ofrezco){ if(negWanted.contains(mO)) { give.add(mO); } }

        System.out.println("MI MENSAJE");
        System.out.println(myMessage.toString());
        System.out.println("OTRO MENSAJE");
        System.out.println(m.toString());

        // Si alguna de las intersecciones está vacía, se rechaza el intercambio
        if(give.size() == 0 || take.size() == 0){
            this.terminarIntercambio(false, k, msgId, m.getComId());
            return;
        }

        // CROMOS CON LOS QUE NEGOCIAMOS AHORA MISMO
        Cromo doy = give.poll();
        Cromo tomo = take.poll();

        Cromo current_doy = doy;
        Cromo current_tomo = tomo;

        // if(this.album.evaluarIntercambio(give.get(0), take.get(0))) {}

        // COMIENZA EL BUCLE LESS GO
        while(skibidi){

            // Primero, miramos la mejor oferta que estamos haciendo para ver si nos vale la pena, si no cancelamos
            if(!this.album.evaluarIntercambio(doy, tomo)){
                this.terminarIntercambio(false, k, msgId, m.getComId());
                return;
            }

            // Obtenemos mensaje SOLO SI NO ES PRIMERA ITERACIÓN (ya viene pre-cargado)
            if(negotiationCounter != 0){

                System.out.println("OTRO MENSAJE");
                System.out.println(m.toString());

                m = this.queue.poll(20, TimeUnit.SECONDS);

                // Si no obtenemos mensaje asumimos que ha muerto (D.E.P.)
                if(m == null){
                    tradeLock.lock();
                    this.busy.set(false);
                    this.negId = "";
                    tradeLock.unlock();
                    return;
                }

                // Actualizamos msgId y prStep
                msgId = Integer.parseInt(m.getMsgId())+1;
                prStep = m.getProtocolStep()+1;
            }

            // Miramos si es decisión
            if(m.getProtocol().equals("decision")){
                // Si acepta
                if(m.getDecision()){
                    // Actualizar cosas del album
                    this.album.consigo(tomo);
                    this.album.quito(doy);
                    this.trade_counter++;
                }
                tradeLock.lock();
                this.busy.set(false);
                this.negId = "";
                tradeLock.unlock();
                return;
            }

            // Ahora miramos el paso del protocolo, si es el 2 el otro agente aún no conoce nuestras listas
            // ponemos G=false para meterle un tímido nerf, que si no puedes ir robando todas las cartas de una
            if(prStep == 2){
                myMessage.addTrade(this.album.lista_deseados, this.album.lista_ofrezco, false, 0);
                myMessage.addInfoMonitor((int)felicidad,album.getSetsCompletados(),album.tengo.size());
                this.sendMessage(k.getIpString(), k.getPort(), myMessage.toXML());
                this.sendToMonitor(myMessage.toXML());
                negotiationCounter++;
                continue; // Pasamos a siguiente iteración del bucle
            }

            // EN EL RESTO DE ITERACIONES, MIRAMOS LA OFERTA QUE NOS LLEGA
            Cromo ofertaDar = this.album.COLECCION.get(m.getWanted().get(0)-1);
            Cromo ofertaTomar = this.album.COLECCION.get(m.getOffered().get(0)-1);

            // Evaluamos la oferta recibida, si nos gusta el intercambio la aceptamos.
            if(this.album.evaluarIntercambio(ofertaDar, ofertaTomar)){
                // Actualizar cosas del album
                this.album.consigo(current_tomo);
                this.album.quito(current_doy);
                this.trade_counter++;
                this.terminarIntercambio(true, k, msgId, m.getComId());
                return;
            }

            // Actualizamos nuestra oferta vigente
            if(doy != null){ current_doy = doy; }
            if(tomo != null){ current_tomo = tomo; }

            ArrayList<Cromo> dame = new ArrayList<>();
            dame.add(tomo);

            ArrayList<Cromo> toma = new ArrayList<>();
            toma.add(doy);

            // Enviamos nuestra nueva oferta
            myMessage.addTrade(dame, toma, this.G, 0);
            sendMessage(k.getIpString(),k.getPort(),myMessage.toXML());

            if(negotiationCounter % 2 == 0){
                doy = give.poll();
                if(doy == null){
                    // Si no podemos mejorar oferta con el ofrecido, lo hacemos con el pedido
                    tomo = take.poll();
                }

            }else{
                tomo = take.poll();
                if(tomo == null){
                    // Si no podemos mejorar oferta con el pedido, lo hacemos con el ofrecido
                    doy = give.poll();
                }
            }

            // Si nos quedamos sin poder mejorar ofertas, cancelamos intercambio
            if(doy == null && tomo == null){
                this.terminarIntercambio(false, k, msgId, m.getComId());
                return;
            }

            negotiationCounter++;
            Thread.sleep(2000);

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

    // Función para tratar los mensajes de intercambio que nos lleguen.
    public void intercambiar(String id, Message m) {
        tradeLock.lock();
        try{

            // Miramos si está ocupado, si no lo está empezamos un intercambio nuevo y lo marcamos como ocupado
            if (!busy.get()){
                busy.set(true);
                negId = id;
            }else{
                // Si estamos ocupados y nos llega un mensaje del agente con el que estamos negociando, lo atendemos
                if (id.equals(negId)){
                    queue.put(m);   // Encolamos el mensaje de negociación
                }else{
                    /*AgentKey k = new AgentKey(m.getOriginIp(),m.getOriginPortTCP());
                    Message m_1 = createMessage(null,"1","decision",1,"TCP",k);
                    m_1.addDecision(false);
                    sendMessage(k.getIpString(),k.getPort(),m_1.toXML());*/
                    // Si no es con el que estamos negociando, lo ignoramos
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        finally{ tradeLock.unlock(); }
    }

    public void terminarIntercambio(boolean d, AgentKey k, int mId, String cId) {
        if(d){
            System.out.println("******************************************************");
            System.out.println("*****************INTERCAMBIO ACEPTADO*****************");
            System.out.println("******************************************************");
        }else{
            System.out.println("******************************************************");
            System.out.println("*****************INTERCAMBIO DENEGADO*****************");
            System.out.println("******************************************************");
        }
        Message m = createMessage(cId, Integer.toString(mId), "decision", 1, "TCP", k);
        m.addDecision(d);
        m.addInfoMonitor((int)felicidad,album.getSetsCompletados(),album.tengo.size());
        this.sendMessage(k.getIpString(), k.getPort(), m.toXML());
        this.sendToMonitor(m.toXML());
        tradeLock.lock();
        this.busy.set(false);
        this.negId = "";
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

