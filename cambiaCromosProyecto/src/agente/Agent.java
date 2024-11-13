package agente;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.*;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.util.Scanner;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

public class Agent {

    //TODO: CAMBIAR TIPO DE MENSAJE DE DESCUBRIMIENTO, PONERLO EN XML REQUISITO 7

    //TODO: FUNCIONES BASICAS REQUISITO 10
    
    // Atributos o propiedades de la clase
    private int id;
    private String ip;
    private int serverPort = 0;
    private int udpPort = 0;
    private long ts;
    private ServerSocket serverSocket;
    private DatagramSocket datagramSocket;
    private ConcurrentHashMap<AgentKey, AgentInfo> discoveredAgents = new ConcurrentHashMap<>();
    private ArrayList<InetAddress> ipList = new ArrayList<InetAddress>();
    //Monitor info
    private final String monitorIP = "127.0.0.1";
    private final int monitorPort = 4300;

    //Para parar el agente
    private final Object monitor_stop = new Object();
    private boolean pausado;

    // Constructor
    public Agent() throws UnknownHostException {
        //Pillamos nuestra IP local
        this.ip = getLocalIpAddress();

        //Encuentra puertos y los asigna automaticamente
        findPorts(); 

        // TODO: Considerar cambiar a como lo dice el profesor (tipo A_2_1)
        //Pillamos un timestamp y definimos el id del agente con un hash
        this.ts = System.currentTimeMillis();
        this.id = generateHash(ip,this.serverPort,this.ts);

        this.ipList.add(InetAddress.getByName("192.168.127.227"));
        this.ipList.add(InetAddress.getByName("192.168.127.83"));
        this.ipList.add(InetAddress.getByName("192.168.127.161"));
        this.ipList.add(InetAddress.getByName("192.168.127.212"));
        //Inicializamos el socket de servidor
        initializeServerSocket();
        
        //Inicalizamos el datagram socketç
        initializeDatagramSocket();

        // Avisar al Monitor de que el agente ha nacido;
        /* TODO: quitar comentario cuando tengamos monitor funcionando
        String message = createXmlMessage("1", "1","heNacido", 1, "TCP",
                Integer.toString(id), ip, udpPort, serverPort, Long.toString(ts) , "1", monitorIP ,
                monitorPort+1, monitorPort, "1", "nada"
        );
        sendToMonitor(message);
        */

        //Por ahora lanzamos los hilos independientes asi para poder hacerlo todo
        //todo new Thread(this::listenForMessages).start();
        new Thread(this::listenForMessages).start();
        new Thread(this::findAgents).start();
        new Thread(this::listenForUdpMessages).start();
    }

    // Método para obtener la IP local
    private String getLocalIpAddress() {
        // TODO: OBTENER MASCAR SUBRED Y OBTENER LISTA DE IPS REQUISITOS 2 Y 3
        // NOTA: VA COMO UN TIRO PERO PARA LAS PRUEBAS VAMOS A HACERLO CON LA IP LOCAL POR DEFECTO
        // try {
        //     InetAddress localHost = InetAddress.getLocalHost();
        //     return localHost.getHostAddress();
        // } catch (UnknownHostException e) {
        //     e.printStackTrace();
        //     return "127.0.0.1"; // IP por defecto en caso de error
        // }
        return "127.0.0.1";
    }

    // Método para encontrar puertos disponibles y asignarlos (LO DE UDP NO ESTA ESPECIFICADO PERO ME JUEGO EL CUELLO A QUE AL FINAL ES ASI)
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
            //PONER AQUI LO DE ERROR DEL AGENTE A LO MEJOR A VER COMO LO IMPLEMENTO
            throw new RuntimeException("No se encontraron puertos disponibles.");
        }
    }


    // Comprueba si un puerto está disponible
    // NOTA: NOOO DEJAA EL SOCKET ACTIVO SOLO LO COMPRUEBO
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
            //PONER ERROR DEL AGENTE
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
    // Nota lo de tener socket abierto para mandar mensajes una mierda, que sea dinámico y ya esta que es como siempre se hace
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

            candado();

            try {

                // Acepta una conexión entrante
                Socket incomingConnection = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(incomingConnection.getInputStream()));
                String receivedMessage = in.readLine();
                System.out.println(receivedMessage);
                // Leer el mensaje recibido
//                StringBuilder receivedMessageBuilder = new StringBuilder();
//                String line;
//                while ((line = in.readLine()) != null) {
//                    receivedMessageBuilder.append(line);
//                }
//
//                // Convertir el mensaje a un String
//                String receivedMessage = receivedMessageBuilder.toString();

                // Validar el mensaje XML
                if (validate(receivedMessage)) {
                    System.out.println("Received valid message:\n" + receivedMessage);

                    //Parseamos el mensaje


                    //Mostramos el tipo de mensaje para que se gestione correctamente
                    System.out.println("Tipo de mensaje:" + getTypeProtocol(receivedMessage));

                    this.interpretarTipoMensaje(getTypeProtocol(receivedMessage));
                } else {
                    System.out.println("Mensaje descartado: No cumple con la estructura XML definida.");
                }

                // Cierra la conexión una vez procesado el mensaje
                incomingConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    // Método para enviar un mensaje al Monitor
    private void sendToMonitor(String msg){
        try{
            Socket socket = new Socket(monitorIP, monitorPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            out.println(msg);
            System.out.println("Message sent to monitor -> " + msg);

        }catch(Exception e){
            System.out.println("ERROR: UNREACHABLE MONITOR");
            e.printStackTrace();
        }
    }

    // id creator 
    // NOTA: PARA UNOS MILES DE HASHES BIEN, PERO SI QUEREMOS CONSIDERAR UNOS 100.000 ENTONCES TENEMOS UN PROBLEMA, metemos SHA1 o algo asi y tirando
    private int generateHash(String ip, int port, long timestamp) {
        String combinedString = ip + ":" + port + ":" + timestamp;
        return combinedString.hashCode() & 0x7FFFFFFF;
    }

    // Método para obtener el ID
    public int getId() {
        return id;
    }

    // Printea informacion principal sobre el agente
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
    

    //Por ahora solo manda mensajes con formato XML o sin el 
    public void funcionDelAgente() {
        try {
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



            // Pedir el mensaje a enviar
            //  System.out.print("Enter message: ");
            //  String message = reader.readLine();
            //String message = this.createAgentMessageXml();
            long originTime = System.currentTimeMillis();

            //TODO cambiar comID, msgID, destID ya que no tengo la lista de agentes

            String message = createXmlMessage("1", "2", messageType, 1, "UDP",Integer.toString(id)
                    , ip, udpPort, serverPort,Long.toString(originTime) , "1", targetIp ,
                    targetPort-2, targetPort+2, "1", "nada"
            );

            // Llamar a sendMessage para enviar el mensaje
            sendMessage(targetIp, targetPort, message);
        } catch (Exception e) {
            System.out.println("An error occurred while sending the message.");
            e.printStackTrace();
        }
    }

    
    //Descubre agentes por fuerza bruta SOLO dentro de la red local, considerar red o rangos oportunos en su momento, pero cuidado con la seguridad...
    public void findAgents() {
        //String discoveryMessage = "DISCOVERY_REQUEST";
        while (true) {
            candado();
            try {

                for (int i = 0; i < ipList.size() ; i++) {

                    InetAddress localAddress = ipList.get(0);

                    // Bucle para enviar mensajes a puertos impares en el rango
                    for (int port = 4001; port <= 4100; port += 2) {

                        if (port != this.udpPort) {

                            long originTime = System.currentTimeMillis();

                            // Si tenemos el agente en nuestra lista le quitamos 1 a su ttl
                            AgentKey k = new AgentKey(localAddress.getHostName(), port);
                            if (discoveredAgents.containsKey(k)) {
                                discoveredAgents.get(k).searched();
                                // Si con su nuevo ttl lo consideramos muerto, lo eliminamos
                                if (!discoveredAgents.get(k).alive()) {
                                    discoveredAgents.remove(k);
                                }
                            }

                        /*
                        destID se obtendría así, pero no creo que tenga sentido añadirlo al mensaje de descubrimiento,
                        ya que no hay manera de saberlo antes de que el agente que has descubierto te lo diga.

                        String destId = discoveredAgents.get(a).getId();
                         */

                            String discoveryMessage = createXmlMessage("1", "2", "hola", 1, "UDP", Integer.toString(id)
                                    , ip, udpPort, serverPort, Long.toString(originTime), "1", localAddress.getHostName(),
                                    port, port + 2, "1", "nada"
                            );

                            byte[] messageData = discoveryMessage.getBytes(StandardCharsets.UTF_8);

                            // Crear un paquete UDP con el mensaje de descubrimiento
                            DatagramPacket packet = new DatagramPacket(
                                    messageData, messageData.length, localAddress, port);

                            // Enviar el paquete de descubrimiento
                            datagramSocket.send(packet);
                        }
                    }
                }


                Thread.sleep(2000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Método de escucha para mensajes de descubrimiento de agentes
    //Si es una request le responde con sus datos.
    //Si es una response lo registra
    // TODO esto se puede optimizar mucho
    public void listenForUdpMessages() {
        byte[] buffer = new byte[1024];
    
        try {
            System.out.println("Listening for UDP messages...");

            while (true) {
                candado();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    
                // Recibir un paquete UDP
                datagramSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                InetAddress senderAddress = packet.getAddress();
                int senderPort = packet.getPort();

                // Procesar el mensaje recibido
                if(validate(message)) {

                    if(Objects.equals(getTypeProtocol(message), "hola")){

                        handleDiscoveryRequest(senderAddress, senderPort);
                    }if(Objects.equals(getTypeProtocol(message), "estoy")){
                        registerAgent(senderAddress,senderPort);
                    }
                }else{
                    System.out.println("El mensaje no ha sido validado");
                }


//                if (message.equals("DISCOVERY_REQUEST")) {
//                    handleDiscoveryRequest(senderAddress, senderPort);
//                } else if (message.startsWith("DISCOVERY_RESPONSE")) {
//                    registerAgent(senderAddress,senderPort);
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleDiscoveryRequest(InetAddress requesterAddress, int requesterPort) {
        try {
            // Crear un mensaje de respuesta con el ID y puerto TCP del agente
            //String responseMessage = "DISCOVERY_RESPONSE " + id + "," + serverPort;

            long originTime = System.currentTimeMillis();

            //TODO cambiar comID, msgID, destID ya que no tengo la lista de agentes

            String responseMessage = createXmlMessage("1", "2", "estoy", 2, "UDP",Integer.toString(id)
                    , ip, udpPort, serverPort,Long.toString(originTime) , "1",requesterAddress.getHostName() ,
                    requesterPort, requesterPort+2, "1", "nada"
            );

            byte[] responseData = responseMessage.getBytes(StandardCharsets.UTF_8);
    
            DatagramPacket responsePacket = new DatagramPacket(
                responseData, responseData.length, requesterAddress, requesterPort);
    
            datagramSocket.send(responsePacket);
            //System.out.println("Sent discovery response to " + requesterAddress + ":" + requesterPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public void registerAgent(InetAddress agentAddress, int serverPort) {
        serverPort = serverPort - 1;
        String agentIp = agentAddress.getHostAddress();
        AgentKey k = new AgentKey(agentIp, serverPort);
        // TODO: Sacar la ID del mensaje recibido
        AgentInfo v = new AgentInfo("EJEMPLO");
    
        if (!discoveredAgents.containsKey(k)) {
            discoveredAgents.put(k, v);
            //System.out.println("Registered new agent: " + agentInfo);
        } else {
            // TODO: Cuando tenga la ID hacerlo así
            // La id no es la misma, es otro agente
            // if(!discoveredAgents.get(k).getId().equals(senderId)){
            //     AgentInfo other = new AgentInfo("otraId");
            // }else{ *la línea de abajo
            discoveredAgents.get(k).answered();
            //System.out.println("Agent already registered: " + agentInfo);
        }
    }

//    public String createAgentMessageXml() {
//        // Crear una instancia de Scanner para obtener entrada del usuario
//        Scanner scanner = new Scanner(System.in);
//
//        // Crear la instancia del mensaje
//        Message message = new Message();
//
//        // Pedir comunc_id y msg_id al usuario
//        System.out.print("Ingrese comunc_id: ");
//        message.setComuncId(scanner.nextLine());
//
//        System.out.print("Ingrese msg_id: ");
//        message.setMsgId(scanner.nextLine());
//
//        // Crear y llenar el encabezado
//        Header header = new Header();
//        System.out.print("Ingrese el tipo de protocolo (e.g., HOLA): ");
//        header.setTypeProtocol(TipoDeProtocolo.valueOf(scanner.nextLine().toUpperCase()));
//
//        System.out.print("Ingrese el paso del protocolo (protocol_step): ");
//        header.setProtocolStep(scanner.nextInt());
//        scanner.nextLine(); // Consumir la línea restante
//
//        System.out.print("Ingrese el protocolo de comunicación (TCP/UDP): ");
//        header.setComunicationProtocol(scanner.nextLine());
//
//        // Configurar la información de origen del mensaje con los atributos del agente
//        HeaderOriginInfo origin = new HeaderOriginInfo();
//        origin.setOriginId(String.valueOf(this.id));
//        origin.setOriginIp(this.ip);
//        origin.setOriginPortUDP(this.udpPort);
//        origin.setOriginPortTCP(this.serverPort);
//        origin.setOriginTime(this.ts);
//        header.setOrigin(origin);
//
//        // Pedir información de destino
//        HeaderDestinationInfo destination = new HeaderDestinationInfo();
//        System.out.print("Ingrese destination_id: ");
//        destination.setDestinationId(scanner.nextLine());
//
//        System.out.print("Ingrese destination_ip: ");
//        destination.setDestinationIp(scanner.nextLine());
//
//        System.out.print("Ingrese destination_port_UDP: ");
//        destination.setDestinationPortUDP(scanner.nextInt());
//
//        System.out.print("Ingrese destination_port_TCP: ");
//        destination.setDestinationPortTCP(scanner.nextInt());
//
//        System.out.print("Ingrese destination_time: ");
//        destination.setDestinationTime(scanner.nextLong());
//        scanner.nextLine(); // Consumir la línea restante
//
//        header.setDestination(destination);
//        message.setHeader(header);
//
//        // Crear el cuerpo del mensaje
//        Body body = new Body();
//        System.out.print("Ingrese el contenido del cuerpo (body_info): ");
//        body.setBodyInfo(scanner.nextLine());
//        message.setBody(body);
//
//        // Configurar el contenido común (dejar en blanco si no hay datos adicionales)
//        CommonContent commonContent = new CommonContent();
//        message.setCommonContent(commonContent);
//
//        // Crear el XML a partir del objeto Message
//        String xmlMessage = XMLParser.createXmlMessage(message);
//
//        // Imprimir el mensaje generado
//        System.out.println("Mensaje XML generado:\n" + xmlMessage);
//
//        // Retornar el XML como string
//        return xmlMessage;
//    }
//

    public void interpretarTipoMensaje(String tipo) {
        switch (tipo) {
            case "parate" -> this.parar();
            case "continua" -> this.continuar();
            case "autodestruyete" -> this.autodestruccion();
            case "reproducete" -> this.reproducirse();
            default -> System.out.println("Tipo de mensaje no implementado: " + tipo);
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


            //AVISAR AL MONITOR

            builder.start();

            System.out.println("Nueva instancia del agente lanzada exitosamente.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void parar(){
        // Mandar al monitor mensaje heParado
        /* TODO: quitar comentario cuando tengamos monitor funcionando
        // Avisar al Monitor de que el agente ha parado;
        String message = createXmlMessage("1", "1","heNacido", 1, "TCP",
                Integer.toString(id), ip, udpPort, serverPort, Long.toString(ts) , "1", monitorIP ,
                monitorPort+1, monitorPort, "1", "nada"
        );
        sendToMonitor(message);
         */
        //con endoque de variable global y continue en los metodos de escucha
        synchronized (monitor_stop) {
            pausado = true;
            System.out.println("Agente parado.");
        }
    }

    private void continuar(){

        //con enfoque de variable en el agente y continue en los metodos de escucha
        synchronized (monitor_stop) {
            pausado = false;
            monitor_stop.notifyAll();
            System.out.println("El agente va a continuar.");
        }

        //NOTIFICAR AL MONITOR
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


    private void error(){
        //Hacemos que el agente aborte y mostramos el error qeu se ha producido
    }

    public void autodestruccion() {
        System.out.println("Agent is self-destructing...");
    
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

        /* TODO: quitar comentario cuando tengamos monitor funcionando
        // Avisar al Monitor de que el agente muere;
        String message = createXmlMessage("1", "1","meMuero", 1, "TCP",
                Integer.toString(id), ip, udpPort, serverPort, Long.toString(ts) , "1", monitorIP ,
                monitorPort+1, monitorPort, "1", "nada"
        );
        sendToMonitor(message);
         */

        // Detener el proceso del agente
        System.exit(0); // Termina el programa
    }



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
            //System.out.println("XML no es válido: " + e.getMessage());
            return false;
        }
    }



    public static String createXmlMessage(String comucID, String msgID, String typeProtocol, int protocolStep,
                                          String communicationProtocol, String originId, String originIp, int originPortUDP,
                                          int originPortTCP, String originTime, String destinationId, String destinationIp,
                                          int destinationPortUDP, int destinationPortTCP, String destinationTime, String bodyInfo) {
        try {
            // Configura el analizador de documentos
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Crea el documento XML
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Message");
            rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            doc.appendChild(rootElement);
            Element nodeComunc = doc.createElement("comunc_id");
            nodeComunc.setTextContent(comucID);
            rootElement.appendChild(nodeComunc);
            Element nodeMesgID = doc.createElement("msg_id");
            nodeMesgID.setTextContent(msgID);
            rootElement.appendChild(nodeMesgID);

            // Agrega los elementos al XML

            // Elemento header
            Element header = doc.createElement("header");
            rootElement.appendChild(header);
            Element typeP = doc.createElement("type_protocol");
            typeP.setTextContent(typeProtocol);
            header.appendChild(typeP);
            Element protocols = doc.createElement("protocol_step");
            protocols.setTextContent(Integer.toString(protocolStep));
            header.appendChild(protocols);
            Element communicationProtocolS = doc.createElement("comunication_protocol");
            communicationProtocolS.setTextContent(communicationProtocol);
            header.appendChild(communicationProtocolS);



            // Elemento origin
            Element origin = doc.createElement("origin");
            header.appendChild(origin);

            Element originID = doc.createElement("origin_id");
            originID.setTextContent(originId);
            origin.appendChild(originID);
            Element originIP = doc.createElement("origin_ip");
            originIP.setTextContent(originIp);
            origin.appendChild(originIP);
            Element originPort = doc.createElement("origin_port_UDP");
            originPort.setTextContent(Integer.toString(originPortUDP));
            origin.appendChild(originPort);
            Element originPortp = doc.createElement("origin_port_TCP");
            originPortp.setTextContent(Integer.toString(originPortTCP));
            origin.appendChild(originPortp);
            Element originT = doc.createElement("origin_time");
            originT.setTextContent(originTime);
            origin.appendChild(originT);

            // Elemento destination
            Element destination = doc.createElement("destination");
            header.appendChild(destination);

            Element destinationID = doc.createElement("destination_id");
            destinationID.setTextContent(destinationId);
            destination.appendChild(destinationID);
            Element destinationIP = doc.createElement("destination_ip");
            destinationIP.setTextContent(destinationIp);
            destination.appendChild(destinationIP);
            Element destinationPort = doc.createElement("destination_port_UDP");
            destinationPort.setTextContent(Integer.toString(destinationPortUDP));
            destination.appendChild(destinationPort);
            Element destinationPortp = doc.createElement("destination_port_TCP");
            destinationPortp.setTextContent(Integer.toString(destinationPortTCP));
            destination.appendChild(destinationPortp);
            Element destinationT = doc.createElement("destination_time");
            destinationT.setTextContent(destinationTime);
            destination.appendChild(destinationT);


            // Elemento body
            Element body = doc.createElement("body");
            rootElement.appendChild(body);

            Element bodyI = doc.createElement("body_info");
            bodyI.setTextContent(bodyInfo);
            body.appendChild(bodyI);


            //createElement(doc, String.valueOf(body), "body_info", bodyInfo);

            // Elemento common_content vacío
            Element commonContent = doc.createElement("common_content");
            rootElement.appendChild(commonContent);

            // Convierte el documento en una cadena XML

            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String xmlString = writer.toString();
            return xmlString;


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getTypeProtocol(String xmlContent) {
        try {
            // Configura el analizador XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parsear el contenido XML desde la cadena en lugar de un archivo
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            // Crea un objeto XPath para realizar la búsqueda en el documento
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            // Expresión XPath para obtener el elemento type_protocol
            XPathExpression expression = xpath.compile("/Message/header/type_protocol");

            // Busca el nodo type_protocol en el XML
            Node node = (Node) expression.evaluate(doc, XPathConstants.NODE);

            // Retorna el contenido de type_protocol, o null si no se encuentra
            return (node != null) ? node.getTextContent() : null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        Agent agent = new Agent();
        System.out.println("Agent is running correctly:");

        try {
            Thread.sleep(1000); // Pausa de 1 segundo (1000 milisegundos)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
                } else if (command.equalsIgnoreCase("send")) {
                    agent.funcionDelAgente();
                } else if (command.equalsIgnoreCase("reproducete")){
                    agent.reproducirse();
                }else if (command.equalsIgnoreCase("continua")){
                    agent.continuar();}
                else {
                    System.out.println("Unknown command. Available commands: 'status', 'send', 'exit'");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}   

