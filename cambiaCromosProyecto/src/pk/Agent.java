package pk;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        sendHelloToMonitor();
        listen();           // Se pone a escuchar
    }

    private void sendHelloToMonitor() {

        try (Socket monitorSocket = new Socket(controlIp, controlPort)) {
            DataOutputStream out = new DataOutputStream(monitorSocket.getOutputStream());
            DataInputStream in = new DataInputStream(monitorSocket.getInputStream());
            LocalDateTime tiempoLocal = LocalDateTime.now();
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String tiempoFormateado = tiempoLocal.format(formato);
            String mensaje;
            mensaje = createXmlMessage("1", "2", "heNacido", 1, "TCP", this.id, this.listenSocket.getInetAddress().getHostName(), port + 1, port, tiempoFormateado, "1", controlIp, controlPort + 1, controlPort, tiempoFormateado, "nada"
            );
            out.writeUTF(mensaje);
            String data = in.readUTF();
            System.out.println("Mensaje Recibido: " + data);
        } catch (IOException e) {
            System.err.println("No se pudo conectar al monitor en el puerto " + controlPort);
            e.printStackTrace();
        }
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
            //TODO: Cambiar a mensaje de hola de descubrimiento
            byte[] sendData = "Hola agente!".getBytes(); // Mensaje a enviar al agente

            //TODO: no se hasta que punto esto es capaz de recibir el mensaje de vuelta por tamaño
            byte[] receiveData = new byte[1024];         // Buffer para recibir la respuesta

            while (true) {  // Bucle infinito para buscar siempre
                for (String ipString : ipList) {  // Iterar sobre las IPs en la lista de IPs
                    try {
                        InetAddress ipAddress = InetAddress.getByName(ipString);

                        for (int p = portMin+1; p <= portMax; p += 2) {  // Iterar puertos pares dentro del rango

                            System.out.println("Agente buscando en IP: " + ipString + ", Puerto: " + p);

                            try (DatagramSocket udpSocket = new DatagramSocket()) {
                                // Configurar timeout para la espera de respuesta
                                udpSocket.setSoTimeout(100);
                                LocalDateTime tiempoLocal = LocalDateTime.now();
                                DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                String tiempoFormateado = tiempoLocal.format(formato);
                                // Crear paquete de envío
                                String mensaje;
                                mensaje = createXmlMessage("1", "2", "hola", 1, "TCP", id, listenSocket.getInetAddress().getHostName(), port + 1, port, tiempoFormateado, "1", ipString, p, p-1, tiempoFormateado, "nada"
                                );
                                sendData = mensaje.getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, p);
                                udpSocket.send(sendPacket);  // Enviar paquete al agente

                                // Intentar recibir respuesta del agente
                                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                                udpSocket.receive(receivePacket); // Espera a recibir respuesta

                                // Si recibimos respuesta, el agente está en este puerto
                                System.out.println("Agente encontrado en IP: " + ipString + ", Puerto: " + p);
                                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

                                //TODO: Ahora mismo hace simplemente un print, debe añadirlo a la lista de agentes
                                System.out.println("Respuesta del agente: " + response);

                                // Mandar mensaje de hola al monitor
                                sendHelloToMonitor();

                            } catch (SocketTimeoutException e) {
                                // Si no hay respuesta, asumimos que no hay agente en este puerto
                                System.out.println("No se recibió respuesta de " + ipString + " en el puerto " + p);
                            } catch (IOException e) {
                                // Otras excepciones de entrada/salida
                                System.err.println("Error al enviar/recibir en IP: " + ipString + ", Puerto: " + p);
                                e.printStackTrace();
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
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                LocalDateTime tiempoLocal = LocalDateTime.now();
                DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String tiempoFormateado = tiempoLocal.format(formato);
                String mensaje;
                mensaje = createXmlMessage("1", "2", "hola", 1, "TCP", id, listenSocket.getInetAddress().getHostName(), port + 1, port, tiempoFormateado, "1", ipDest, portDest+1, portDest, tiempoFormateado, "nada"
                );
                out.writeUTF(mensaje);
                String data = in.readUTF();
                System.out.println("Mensaje Recibido: " + data);
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

    public static void saveXmlStringToFile(String xmlContent, String filePath) {
        try {


            // Crea un objeto File para el archivo XML
            File file = new File(filePath);

            // Crea un BufferedWriter para escribir en el archivo
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            // Escribe el contenido XML en el archivo
            writer.write(xmlContent);

            // Cierra el BufferedWriter
            writer.close();

            System.out.println("Archivo XML guardado correctamente en: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ocurrió un error al guardar el archivo XML.");
        }
    }

    /* Método exec deprecado, habrá que matar el proceso desde el controlador o con opción en main
    public void off() throws IOException {
        Runtime.getRuntime().exec("taskkill /F /IM <processname>.exe");
    }*/
}
