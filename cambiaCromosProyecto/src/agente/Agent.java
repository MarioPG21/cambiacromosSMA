import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.io.File;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;


import mensajes.Body;
import mensajes.CommonContent;
import mensajes.Header;
import mensajes.HeaderDestinationInfo;
import mensajes.HeaderOriginInfo;
import mensajes.Message;
import mensajes.TipoDeProtocolo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.util.Scanner;

import mensajes.Message;

public class Agent {
    //TODO: CAMBIAR LO DE VALIDACION DE XML COMO LO TIENE CURTINEZ  REQUISITO 4
    //TODO: CAMBIAR TIPO DE MENSAJE DE DESCUBRIMIENTO, PONERLO EN XML REQUISITO 7
    //TODO: EN CADA METODO DE ESCUCHA PONER TRANSFORMACION A DOM Y VALIDACION DEL XSD REQUISITO 7
    //TODO: FUNCIONES BASICAS REQUISITO 10
    
    // Atributos o propiedades de la clase
    private int id;
    private String ip;
    private int serverPort = 0;
    private int udpPort = 0;
    private long ts;
    private ServerSocket serverSocket;
    private DatagramSocket datagramSocket;
    private Set<String> discoveredAgents = new HashSet<>(); //TODO: PONERLO COMO EN EL OTRO LAO REQUISITO 6

    //Para parar el agente
    private final Object monitor_stop = new Object();
    private boolean pausado;

    //Monitor info
    private final String monitorIP = "127.0.0.1";
    private final int monitorPort = 4300;


    // Constructor
    public Agent() {
        //Pillamos nuestra IP local
        this.ip = getLocalIpAddress();

        //Encuentra puertos y los asigna automaticamente
        findPorts(); 

        //Pillamos un timestamp y definimos el id del agente con un hash
        this.ts = System.currentTimeMillis();
        this.id = generateHash(ip,this.serverPort,this.ts);
        
        //Inicializamos el socket de servidor
        initializeServerSocket();
        
        //Inicalizamos el datagram socketç
        initializeDatagramSocket();

        //Por ahora lanzamos los hilos independientes asi para poder hacerlo todo
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

            //TODO: METODO HE NACIDO REQUISITO 5
            //TODO: CREAR FUNCION PARA COMUNICARSE CON EL MONITOR

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
            e.printStackTrace();
            //Poner ERROR DEL AGENTE
        }
    }

    // Metodo ejecutado en un hilo para escuchar todos los mensajes TCP entrantes con el socket inicializado
    // Valida que cumplan con la especificacion XML
    public void listenForMessages() {
        while (true) {

            candado(); //funcion para parar y continuar el agente

            try {
                // Acepta una conexión entrante
                Socket incomingConnection = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(incomingConnection.getInputStream()));
                
                // Leer el mensaje recibido
                StringBuilder receivedMessageBuilder = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    receivedMessageBuilder.append(line);
                }
                
                // Convertir el mensaje a un String
                String receivedMessage = receivedMessageBuilder.toString();
                
                // Validar el mensaje XML
                if (XMLParser.validateXml(receivedMessage)) {
                    System.out.println("Received valid message:\n" + receivedMessage);

                    //Parseamos el mensaje
                    Message mes = XMLParser.parseXmlMessage(receivedMessage);

                    //Mostramos el tipo de mensaje para que se gestione correctamente
                    System.out.println("Tipo de mensaje:" + mes.getHeader().getTypeProtocol());

                    //Interpreta el tipo de mensaje que ha recibido y ejecuta la funcion correspondiente en consecuencia
                    this.interpretarTipoMensaje(mes.getHeader().getTypeProtocol());

                    
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
            for (String agentInfo : discoveredAgents) {
                System.out.println("  - " + agentInfo);
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
    
            // Pedir el mensaje a enviar
            //  System.out.print("Enter message: ");
            //  String message = reader.readLine();
            String message = this.createAgentMessageXml();
    
            // Llamar a sendMessage para enviar el mensaje
            sendMessage(targetIp, targetPort, message);
        } catch (Exception e) {
            System.out.println("An error occurred while sending the message.");
            e.printStackTrace();
        }
    }

    
    //Descubre agentes por fuerza bruta SOLO dentro de la red local, considerar red o rangos oportunos en su momento, pero cuidado con la seguridad...
    public void findAgents() {
        String discoveryMessage = "DISCOVERY_REQUEST";
        while(true) {

            candado(); //funcion para parar y continuar el agente

            try {
                InetAddress localAddress = InetAddress.getByName("127.0.0.1");

                // Bucle para enviar mensajes a puertos impares en el rango
                for (int port = 4001; port <= 4100; port += 2) {
                    byte[] messageData = discoveryMessage.getBytes(StandardCharsets.UTF_8);

                    // Crear un paquete UDP con el mensaje de descubrimiento
                    DatagramPacket packet = new DatagramPacket(
                        messageData, messageData.length, localAddress, port);

                    // Enviar el paquete de descubrimiento
                    datagramSocket.send(packet);
                }

        
                Thread.sleep(30000);

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

                candado(); //funcion para parar y continuar el agente

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    
                // Recibir un paquete UDP
                datagramSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                InetAddress senderAddress = packet.getAddress();
                int senderPort = packet.getPort();
    
                // Procesar el mensaje recibido
                if (message.equals("DISCOVERY_REQUEST")) {
                    handleDiscoveryRequest(senderAddress, senderPort);
                } else if (message.startsWith("DISCOVERY_RESPONSE")) {
                    registerAgent(senderAddress,senderPort);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleDiscoveryRequest(InetAddress requesterAddress, int requesterPort) {
        try {
            // Crear un mensaje de respuesta con el ID y puerto TCP del agente
            String responseMessage = "DISCOVERY_RESPONSE " + id + "," + serverPort;
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
        String agentInfo = agentIp + ":" + serverPort;
    
        if (!discoveredAgents.contains(agentInfo)) {
            discoveredAgents.add(agentInfo);
            System.out.println("Registered new agent: " + agentInfo);
        } else {
            //System.out.println("Agent already registered: " + agentInfo);
        }
    }

    public String createAgentMessageXml() {
        // Crear una instancia de Scanner para obtener entrada del usuario
        Scanner scanner = new Scanner(System.in);

        // Crear la instancia del mensaje
        Message message = new Message();

        // Pedir comunc_id y msg_id al usuario
        System.out.print("Ingrese comunc_id: ");
        message.setComuncId(scanner.nextLine());

        System.out.print("Ingrese msg_id: ");
        message.setMsgId(scanner.nextLine());

        // Crear y llenar el encabezado
        Header header = new Header();
        System.out.print("Ingrese el tipo de protocolo (e.g., HOLA): ");
        header.setTypeProtocol(TipoDeProtocolo.valueOf(scanner.nextLine().toUpperCase()));

        System.out.print("Ingrese el paso del protocolo (protocol_step): ");
        header.setProtocolStep(scanner.nextInt());
        scanner.nextLine(); // Consumir la línea restante

        System.out.print("Ingrese el protocolo de comunicación (TCP/UDP): ");
        header.setComunicationProtocol(scanner.nextLine());

        // Configurar la información de origen del mensaje con los atributos del agente
        HeaderOriginInfo origin = new HeaderOriginInfo();
        origin.setOriginId(String.valueOf(this.id));
        origin.setOriginIp(this.ip);
        origin.setOriginPortUDP(this.udpPort);
        origin.setOriginPortTCP(this.serverPort);
        origin.setOriginTime(this.ts);
        header.setOrigin(origin);

        // Pedir información de destino
        HeaderDestinationInfo destination = new HeaderDestinationInfo();
        System.out.print("Ingrese destination_id: ");
        destination.setDestinationId(scanner.nextLine());

        System.out.print("Ingrese destination_ip: ");
        destination.setDestinationIp(scanner.nextLine());

        System.out.print("Ingrese destination_port_UDP: ");
        destination.setDestinationPortUDP(scanner.nextInt());

        System.out.print("Ingrese destination_port_TCP: ");
        destination.setDestinationPortTCP(scanner.nextInt());

        System.out.print("Ingrese destination_time: ");
        destination.setDestinationTime(scanner.nextLong());
        scanner.nextLine(); // Consumir la línea restante

        header.setDestination(destination);
        message.setHeader(header);

        // Crear el cuerpo del mensaje
        Body body = new Body();
        System.out.print("Ingrese el contenido del cuerpo (body_info): ");
        body.setBodyInfo(scanner.nextLine());
        message.setBody(body);

        // Configurar el contenido común (dejar en blanco si no hay datos adicionales)
        CommonContent commonContent = new CommonContent();
        message.setCommonContent(commonContent);

        // Crear el XML a partir del objeto Message
        String xmlMessage = XMLParser.createXmlMessage(message);

        // Imprimir el mensaje generado
        System.out.println("Mensaje XML generado:\n" + xmlMessage);

        // Retornar el XML como string
        return xmlMessage;
    }
    
    //////////////////////////////////////////////
    ///                                        /// 
    ///                                        ///
    ///     FUNCIONES BASICAS DEL AGENTE       /// 
    ///                                        /// 
    ///                                        ///     
    /// //////////////////////////////////////////



    public void interpretarTipoMensaje(TipoDeProtocolo tipo) {
        switch(tipo) {
            case PARATE:
                this.parar();
                break;
            case CONTINUA:
                this.continuar();
                break;
            case AUTODESTRUYETE:
                this.autodestruccion();
                break;
            case REPRODUCETE:
                this.reproducirse();
                break;
            default:
                System.out.println("Tipo de mensaje no implementado: " + tipo);
        }
    }

    private void parar(){
        synchronized (monitor_stop) {
            pausado = true;
            System.out.println("Agente parado.");
        }

        //NOTIFICAR AL MONITOR
    }

    private void continuar(){
        synchronized (monitor_stop) {
            pausado = false;
            monitor_stop.notifyAll();
            System.out.println("El agente va a continuarr.");
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
    

    public void reproducirse() {
        try {
            // Obtener el bean del sistema operativo para comprobar la carga del sistema
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

            // Obtener la carga de CPU
            double cpuLoad = osBean.getSystemCpuLoad();

            System.out.println("Carga de la CPU:" + cpuLoad);

            // Obtener memoria física total y libre
            long totalMemory = osBean.getTotalPhysicalMemorySize();
            long freeMemory = osBean.getFreePhysicalMemorySize();

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
    
        // Detener el proceso del agente
        System.exit(0); // Termina el programa
    }

    public static void main(String[] args) {
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
                    agent.continuar();
                }else {
                    System.out.println("Unknown command. Available commands: 'status', 'send', 'exit'");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

}   

