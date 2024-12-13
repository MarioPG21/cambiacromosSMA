package agente;

import Cambiacromos.Cromo;
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
    private String comProtocol; // Protocolo de comunicación (TCP/UDP)

    // INFORMACIÓN DE ORIGEN
    private String originId;    // Id de origen del mensaje
    private String originIp;    // Ip de origen del mensaje
    private int originPortTCP;  // Puerto TCP de origen del mensaje
    private int originPortUDP;  // Puerto UDP de origen del mensaje
    private String originTime;  // Tiempo de origen

    // INFORMACIÓN DE DESTINO
    private final String destId;      // Id de destino del mensaje
    private final String destIp;      // Ip de destino del mensaje
    private final int destPortTCP;    // Puerto TCP de destino del mensaje
    private final int destPortUDP;    // Puerto UDP de destino del mensaje
    private String destTime;          // Tiempo de destino

    // INFORMACIÓN DE INTERCAMBIO
    private ArrayList<Integer> wanted = new ArrayList<>();    // Lista de cromos pedidos
    private ArrayList<Integer> offered = new ArrayList<>();   // Lista de cromos ofrecidos
    private float rupees;                                   // Rupias (pos. es ofrecido, neg. es pedido)
    private boolean steal = false;                                  // Boolean que indica si el emisor le va a robar al otro

    // INFORMACIÓN PARA MONITOR
    private int happiness = -1;     // Felicidad de agente emisor
    private int completedSets = -1; // Número de sets completados
    private int numCards = -1;      // Número de cartas en colección

    // DECISIÓN
    private boolean decision;   // Decisión a la que llega en caso de que el mensaje sea tipo "decision"

    // Boolean que nos dirá si hay error en el mensaje
    private boolean error = false;



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
        this.originTime = doc.getElementsByTagName("origin_time").item(0).getTextContent();

        // Información de destino
        this.destId = doc.getElementsByTagName("destination_id").item(0).getTextContent();
        this.destIp = doc.getElementsByTagName("destination_ip").item(0).getTextContent();
        this.destPortTCP = Integer.parseInt(doc.getElementsByTagName("destination_port_TCP").item(0).getTextContent());
        this.destPortUDP = Integer.parseInt(doc.getElementsByTagName("destination_port_UDP").item(0).getTextContent());
        this.destTime = doc.getElementsByTagName("destination_time").item(0).getTextContent();

        // EN ESTE BLOQUE RECOGEMOS INFORMACIÓN ESPECÍFICA DE INTERCAMBIO O DECISIÓN
        switch (this.protocol) {
            case "intercambio" -> {
                // Miramos si hay trading block
                NodeList t = doc.getElementsByTagName("trading_block");
                if (t.getLength() > 0) {

                    // Recogemos cartas deseadas
                    NodeList wanted_cards = doc.getElementsByTagName("wanted_card");
                    for (int i = 0; i < wanted_cards.getLength(); i++) {
                        this.wanted.add(Integer.parseInt(wanted_cards.item(i).getTextContent()));
                    }
                    // Recogemos cartas ofrecidas
                    NodeList offered_cards = doc.getElementsByTagName("offered_card");
                    for (int i = 0; i < wanted_cards.getLength(); i++) {
                        this.wanted.add(Integer.parseInt(offered_cards.item(i).getTextContent()));
                    }

                    // Recogemos rupias
                    this.rupees = Float.parseFloat(doc.getElementsByTagName("rupees").item(0).getTextContent());

                    // Miramos si roba
                    this.steal = Boolean.parseBoolean(doc.getElementsByTagName("steal").item(0).getTextContent());

                    // Si no hay trading block, error
                } else {
                    this.error = true;
                }
            }
            case "decision" -> {
                // Miramos si hay decision_block
                NodeList d = doc.getElementsByTagName("decision");
                if (d.getLength() > 0) {
                    // Obtenemos nuestra decisión
                    this.decision = Boolean.parseBoolean(d.item(0).getTextContent());
                } else {
                    this.error = true;
                }
            }
        }


        // Miramos si hay monitor_info
        NodeList m = doc.getElementsByTagName("monitor_info");
        if(m.getLength() > 0){

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
    // IMPORTANTE
    // ESTA ES LA PLANTILLA BÁSICA DE CREACIÓN DE MENSAJE, LUEGO EXISTEN MÉTODOS PARA HACERLO DE INTERCAMBIO O NO
    public Message(String comId, String msgId, String protocol, int protocolStep, String comProtocol,
                   String originId, String originIp, int originPortTCP, int originPortUDP, String originTime,
                   String destId, String destIp, int destPortTCP, int destPortUDP, String destTime){

        this.comId = comId; this.msgId = msgId;
        this.protocol = protocol; this.protocolStep = protocolStep; this.comProtocol = comProtocol;

        this.originId = originId; this.originIp = originIp;
        this.originPortTCP = originPortTCP; this.originPortUDP = originPortUDP; this.originTime = originTime;

        this.destId = destId; this.destIp = destIp;
        this.destPortTCP = destPortTCP; this.destPortUDP = destPortUDP; this.destTime = destTime;
    }

    // MÉTODOS QUE USAREMOS PARA AÑADIR AL MENSAJE LOS ELEMENTOS ESPECÍFICOS A UN PROTOCOLO

    // INTERCAMBIO
    public void addTrade(ArrayList<Cromo> w, ArrayList<Cromo>o, boolean g, float r){
        ArrayList<Integer> wanted = new ArrayList<>();
        ArrayList<Integer> offered = new ArrayList<>();
        for(Cromo c: w){ wanted.add(c.getId()); }
        for(Cromo c: o){ offered.add(c.getId()); }
        this.wanted = wanted; this.offered = offered; this.steal = g; this.rupees = r;
    }

    // DECISION
    public void addDecision(boolean d){
        this.decision = d;
    }

    // MÉTODO PARA AÑADIR INFO PARA EL MONITOR
    public void addInfoMonitor(int h, int c, int n){
        this.happiness = h; this.completedSets = c; this.numCards = n;
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
            originT.setTextContent(this.originTime);
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
            Element destinationPortUDP = doc.createElement("destination_port_UDP");
            destinationPortUDP.setTextContent(Integer.toString(this.destPortUDP));
            destination.appendChild(destinationPortUDP);
            Element destinationPortTCP = doc.createElement("destination_port_TCP");
            destinationPortTCP.setTextContent(Integer.toString(this.destPortTCP));
            destination.appendChild(destinationPortTCP);
            Element destinationT = doc.createElement("destination_time");
            destinationT.setTextContent(this.destTime);
            destination.appendChild(destinationT);

            switch (this.protocol) {
                case "intercambio" -> {
                    // Si el protocolo es intercambio añade bloque Intercambio
                    Element tradingBlock = doc.createElement("trading_block");
                    rootElement.appendChild(tradingBlock);

                    // Creamos un elemento wanted_cards
                    Element wantedCards = doc.createElement("wanted_cards");
                    tradingBlock.appendChild(wantedCards);

                    // Por cada cromo de nuestra lista de cromos deseados metemos un elemento wanted a wanted_cards
                    for (int c : this.wanted) {
                        Element wanted = doc.createElement("wanted");
                        wanted.setTextContent(Integer.toString(c));
                        wantedCards.appendChild(wanted);
                    }

                    // Creamos un elemento offered_cards
                    Element offeredCards = doc.createElement("offered_cards");
                    tradingBlock.appendChild(offeredCards);

                    // Por cada cromo de nuestra lista de cromos deseados metemos un elemento offered a offered_cards
                    for (int c : this.offered) {
                        Element offered = doc.createElement("offered");
                        offered.setTextContent(Integer.toString(c));
                        offeredCards.appendChild(offered);
                    }

                    // Creamos el elemento rupias
                    Element rupees = doc.createElement("rupees");
                    rupees.setTextContent(Float.toString(this.rupees));
                    tradingBlock.appendChild(rupees);

                    // Creamos el elemento steal
                    Element steal = doc.createElement("steal");
                    steal.setTextContent(Boolean.toString(this.steal));
                    tradingBlock.appendChild(steal);
                }
                case "decision" -> {
                    // Añade elemento decisión
                    Element decision = doc.createElement("decision");
                    decision.setTextContent(Boolean.toString(this.decision));
                    rootElement.appendChild(decision);
                }
            }

            // Si los parámetros para el monitor son distintos de -1, se crea también el bloque monitor_info
            if(this.happiness > -1 && this.completedSets > -1 && this.numCards > -1){
                Element monitorInfo = doc.createElement("monitor_info");
                rootElement.appendChild(monitorInfo);

                // Felicidad
                Element happiness = doc.createElement("happiness");
                happiness.setTextContent(Integer.toString(this.happiness));
                monitorInfo.appendChild(happiness);

                // Número de sets completados
                Element completedSets = doc.createElement("completed_sets");
                completedSets.setTextContent(Integer.toString(this.completedSets));
                monitorInfo.appendChild(completedSets);

                // Cromos en la colección
                Element numCards = doc.createElement("num_cards");
                numCards.setTextContent(Integer.toString(this.numCards));
                monitorInfo.appendChild(numCards);
            }

            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String toString(){
        StringBuilder str = new StringBuilder("IDENTIFICADORES\n" +
                "comunicación: " + comId + ", mensaje: " + msgId + "\n" +
                "PROTOCOLOS\n" +
                "tipo de protocolo: " + protocol + " paso " + protocolStep + "protocolo de comunicación: " + comProtocol +
                "ORIGEN\n" +
                "id: " + originId + ", ip: " + originIp + ", puertos (TCP, UDP): " + originPortTCP + originPortUDP +
                "DESTINO\n" +
                "id: " + destId + ", ip: " + destIp + ", puertos (TCP, UDP): " + destPortTCP + destPortUDP);

        // Miramos si tiene información de intercambio y lo unimos
        if (!wanted.isEmpty()){
            str.append("\nCROMOS OFRECIDOS\n");
            for(int c: wanted){
                str.append(c).append(", ");
            }

            str.append("\nCROMOS PEDIDOS\n");
            for(int c: offered){
                str.append(c).append(", ");
            }

            if(rupees > 0){
                str.append("\nRUPIAS OFRECIDAS: ").append(rupees);
            }else{
                str.append("\nRUPIAS PEDIDAS: ").append(rupees);
            }

            if(steal)
            str.append("\n").append(originId).append(" LE ROBA A ").append(destId).append("!!!");
        }

        return str.toString();
    }

    public String getComId() {
        return comId;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getProtocolStep() {
        return protocolStep;
    }

    public String getComProtocol() {
        return comProtocol;
    }

    public String getOriginId() {
        return originId;
    }

    public String getOriginIp() {
        return originIp;
    }

    public int getOriginPortTCP() {
        return originPortTCP;
    }

    public int getOriginPortUDP() {
        return originPortUDP;
    }

    public String getOriginTime() { return originTime; }

    public String getDestId() {
        return destId;
    }

    public String getDestIp() {
        return destIp;
    }

    public int getDestPortTCP() {
        return destPortTCP;
    }

    public int getDestPortUDP() {
        return destPortUDP;
    }

    public String getDestTime() {
        return destTime;
    }

    public ArrayList<Integer> getWanted() {
        return wanted;
    }

    public ArrayList<Integer> getOffered() {
        return offered;
    }

    public float getRupees() {
        return rupees;
    }

    public boolean isSteal() {
        return steal;
    }

    public int getHappiness() {
        return happiness;
    }

    public int getCompletedSets() {
        return completedSets;
    }

    public int getNumCards() {
        return numCards;
    }

    public boolean isDecision() {
        return decision;
    }

    public boolean isError() {
        return error;
    }

    public void setComId(String comId) {
        this.comId = comId;
    }

    public void setOriginTime(String originTime) { this.originTime = originTime; }

    public void setDestTime(String destTime) { this.destTime = destTime; }
}
