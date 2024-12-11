package agente;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

public class Message {

    // IDENTIFICADORES DE COMUNICACIÓN Y MENSAJE
    private String comId;   // Id de comunicación (único global)
    private String msgId;   // Id de mensaje (único dentro de comunicación)

    // INFORMACIÓN DEL HEADER
    private String protocol;    // Protocolo de mensaje
    private int protocolStep;   // Paso dentro del protocolo
    private String comProtocol;    // Protocolo de comunicación (TCP/UDP)

    // INFORMACIÓN DE ORIGEN
    private String originId;    // Id de origen del mensaje
    private String originIp;    // Ip de origen del mensaje
    private int originPortTCP;  // Puerto TCP de origen del mensaje
    private int originPortUDP;  // Puerto UDP de origen del mensaje
    private long originTime;    // Tiempo de origen

    // INFORMACIÓN DE DESTINO
    private String destId;      // Id de destino del mensaje
    private String destIp;      // Ip de destino del mensaje
    private int destPortTCP;    // Puerto TCP de destino del mensaje
    private int destPortUDP;    // Puerto UDP de destino del mensaje
    private long destTime;      // Tiempo de destino

    // INFORMACIÓN DE INTERCAMBIO
    private ArrayList<Integer> wanted = new ArrayList();    // Lista de cromos pedidos
    private ArrayList<Integer> offered = new ArrayList();   // Lista de cromos ofrecidos
    private double rupees;  // Rupias (pos. es ofrecido, neg. es pedido)

    // INFORMACIÓN PARA MONITOR
    private int happiness;
    private int completedSets;
    private int numCards;

    private boolean isTrade = false;  // Bool que indica si añadimos bloque de intercambio al mensaje o no
    private boolean isMonitor = false; // Bool que indica si contiene info para el monitor

    // INFORMACIÓN DE CUERPO DE MENSAJE
    // TODO: hacer todo el tema del cuerpo del mensaje
    private String bodyInfo;

    // CONSTRUCTOR PARA LOS MENSAJES QUE NOS LLEGAN
    // Construye un objeto mensaje a partir del xml
    public Message(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Parsear el contenido XML desde la cadena en lugar de un archivo
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize(); // función que normaliza el documento, por si acaso

        // FUNCIONAMIENTO:
        // getElementsByTagName devuelve una lista de elementos que tengan el tag "comunc_id", como en nuestro caso
        // solo hay uno pues accedemos el primero, y de ese nodo pillamos el contenido de texto

        // Identificadores de comunicación y mensaje
        this.comId = doc.getElementsByTagName("comunc_id").item(0).getTextContent();
        this.msgId = doc.getElementsByTagName("msg_id").item(0).getTextContent();

        // Información del header
        this.protocol = doc.getElementsByTagName("type_protocol").item(0).getTextContent();
        this.protocolStep = Integer.parseInt(doc.getElementsByTagName("protocol_step").item(0).getTextContent());
        this.comProtocol = doc.getElementsByTagName("comunication_protocol").item(0).getTextContent();

        // Información de origen
        this.originId = doc.getElementsByTagName("origin_id").item(0).getTextContent();
        this.originIp = doc.getElementsByTagName("origin_ip").item(0).getTextContent();
        this.originPortTCP = Integer.parseInt(doc.getElementsByTagName("origin_port_TCP").item(0).getTextContent());
        this.originPortUDP = Integer.parseInt(doc.getElementsByTagName("origin_port_UDP").item(0).getTextContent());
        this.originTime = Long.parseLong(doc.getElementsByTagName("origin_time").item(0).getTextContent());

        // Información de destino
        this.destId = doc.getElementsByTagName("destination_id").item(0).getTextContent();
        this.destIp = doc.getElementsByTagName("destination_ip").item(0).getTextContent();
        this.destPortTCP = Integer.parseInt(doc.getElementsByTagName("destination_port_TCP").item(0).getTextContent());
        this.destPortUDP = Integer.parseInt(doc.getElementsByTagName("destination_port_UDP").item(0).getTextContent());
        this.destTime = Long.parseLong(doc.getElementsByTagName("destination_time").item(0).getTextContent());

        // Miramos si hay trading_block
        NodeList t = doc.getElementsByTagName("trading_block");
        if(t.getLength() > 0){

            this.isTrade = true;

            // Recogemos cartas deseadas
            NodeList wanted_cards = doc.getElementsByTagName("wanted_card");
            for(int i = 0; i < wanted_cards.getLength(); i++){
                this.wanted.add(Integer.parseInt(wanted_cards.item(i).getTextContent()));
            }

            // Recogemos cartas ofrecidas
            NodeList offered_cards = doc.getElementsByTagName("offered_card");
            for(int i = 0; i < wanted_cards.getLength(); i++){
                this.wanted.add(Integer.parseInt(offered_cards.item(i).getTextContent()));
            }
        }

        // Miramos si hay monitor_info
        NodeList m = doc.getElementsByTagName("monitor_info");
        if(m.getLength() > 0){

            this.isMonitor = true;

            // Pillamos grado de felicidad
            this.happiness = Integer.parseInt(doc.getElementsByTagName("happiness").item(0).getTextContent());

            // Pillamos numero de sets completados
            this.completedSets = Integer.parseInt(doc.getElementsByTagName("completed_sets").item(0).getTextContent());

            // Pillamos numero de cromos
            this.numCards = Integer.parseInt(doc.getElementsByTagName("completed_sets").item(0).getTextContent());

        }

    }

    // CONSTRUCTOR PARA LOS MENSAJES QUE ENVIAMOS
    // Construye un objeto mensaje a partir de los parámetros que le pasamos
    public Message(String comId, String msgId, String protocol, int protocolStep, String comProtocol,
                   String originId, String originIp, int originPortTCP, int originPortUDP, long originTime,
                   String destId, String destIp, int destPortTCP, int destPortUDP, long destTime){

        this.comId = comId; this.msgId = msgId;
        this.protocol = protocol; this.protocolStep = protocolStep; this.comProtocol = comProtocol;

        this.originId = originId; this.originIp = originIp;
        this.originPortTCP = originPortTCP; this.originPortUDP = originPortUDP; this.originTime = originTime;

        this.destId = destId; this.destIp = destIp;
        this.destPortTCP = destPortTCP; this.destPortUDP = destPortUDP; this.destTime = destTime;
    }

    public String toXML(){
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
            nodeComunc.setTextContent(this.comId);
            rootElement.appendChild(nodeComunc);
            Element nodeMesgID = doc.createElement("msg_id");
            nodeMesgID.setTextContent(this.msgId);
            rootElement.appendChild(nodeMesgID);

            // Agrega los elementos al XML

            // Elemento header
            Element header = doc.createElement("header");
            rootElement.appendChild(header);
            Element typeP = doc.createElement("type_protocol");
            typeP.setTextContent(this.protocol);
            header.appendChild(typeP);
            Element protocols = doc.createElement("protocol_step");
            protocols.setTextContent(Integer.toString(this.protocolStep));
            header.appendChild(protocols);
            Element communicationProtocolS = doc.createElement("comunication_protocol");
            communicationProtocolS.setTextContent(this.comProtocol);
            header.appendChild(communicationProtocolS);



            // Elemento origin
            Element origin = doc.createElement("origin");
            header.appendChild(origin);

            Element originID = doc.createElement("origin_id");
            originID.setTextContent(this.originId);
            origin.appendChild(originID);
            Element originIP = doc.createElement("origin_ip");
            originIP.setTextContent(this.originIp);
            origin.appendChild(originIP);
            Element originPort = doc.createElement("origin_port_UDP");
            originPort.setTextContent(Integer.toString(this.originPortUDP));
            origin.appendChild(originPort);
            Element originPortp = doc.createElement("origin_port_TCP");
            originPortp.setTextContent(Integer.toString(this.originPortTCP));
            origin.appendChild(originPortp);
            Element originT = doc.createElement("origin_time");
            originT.setTextContent(Long.toString(this.originTime));
            origin.appendChild(originT);

            // Elemento destination
            Element destination = doc.createElement("destination");
            header.appendChild(destination);

            Element destinationID = doc.createElement("destination_id");
            destinationID.setTextContent(this.destId);
            destination.appendChild(destinationID);
            Element destinationIP = doc.createElement("destination_ip");
            destinationIP.setTextContent(this.destIp);
            destination.appendChild(destinationIP);
            Element destinationPort = doc.createElement("destination_port_UDP");
            destinationPort.setTextContent(Integer.toString(this.destPortUDP));
            destination.appendChild(destinationPort);
            Element destinationPortp = doc.createElement("destination_port_TCP");
            destinationPortp.setTextContent(Integer.toString(this.destPortTCP));
            destination.appendChild(destinationPortp);
            Element destinationT = doc.createElement("destination_time");
            destinationT.setTextContent(Long.toString(this.destTime));
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

    public String toString(){
        String str = "IDENTIFICADORES\n" +
                "comunicación: " + comId + ", mensaje: " + msgId + "\n" +
                "PROTOCOLOS\n" +
                "tipo de protocolo: " + protocol + " paso " +  protocolStep + "protocolo de comunicación: " + comProtocol +
                "ORIGEN\n" +
                "id: " + originId + ", ip: " + originIp + ", puertos (TCP, UDP): " + originPortTCP + originPortUDP +
                "DESTINO\n" +
                "id: " + destId + ", ip: " + destIp + ", puertos (TCP, UDP): " + destPortTCP + destPortUDP;

        if (isTrade){
            str += "\nCROMOS OFRECIDOS\n";
            for(int c: wanted){
                str += c + ", ";
            }

            str += "\nCROMOS PEDIDOS\n";
            for(int c: offered){
                str += c + ", ";
            }

            if(rupees > 0){
                str += "\nRUPIAS OFRECIDAS: " + rupees;
            }else{
                str += "\nRUBIAS PEDIDAS: " + rupees;
            }
        }
        return str;
    }

}
