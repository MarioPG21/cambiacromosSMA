package monitor;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import javax.xml.XMLConstants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Monitor {

    private static int num_agents = 0;
    private static int felicidad_prueba = 10;
    private static ConcurrentHashMap<AgentKey_Monitor, AgentInfo_Monitor> agents = new ConcurrentHashMap<>();
    private static JTable tabla;
    private static String[] columnas = {"Agente", "Número de cartas", "Sets Completados", "Felicidad", "Ladrón"};
    private static Object[][] datos = {{"Total", 0, 0, 0},
    };
    private static DefaultTableModel modelo = new DefaultTableModel(datos, columnas);
    public static void main(String[] args) {
        int port = 4300;

        try {
            // Crear la JTable con el modelo
            tabla = new JTable(modelo);
            //Creamos la tabla
            crearTabla();
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Monitor está escuchando en el puerto " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Función para validar el contenido XML
    public static boolean validate(String xmlContent) {
        String xsdFilePath = "cambiaCromosProyecto/src/XMLParser/esquema.xsd";  // Cambia esta ruta al archivo XSD real

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

    // Función para extraer el tipo de protocolo del contenido XML
    public static String getTypeProtocol(String xmlContent) {
        try {
            // Configura el analizador XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parsear el contenido XML desde la cadena
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

    public static void crearTabla() {

        //Valores de la tabla
        JFrame frame = new JFrame("Estado del Sistema");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400); // Tamaño ajustado para mejor visualización
        frame.setLayout(new BorderLayout());
        // Mejoras visuales
        tabla.setRowHeight(30); // Altura de las filas
        tabla.setGridColor(Color.LIGHT_GRAY); // Color de las líneas de la tabla
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 14)); // Fuente
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 16)); // Fuente del encabezado
        tabla.getTableHeader().setBackground(new Color(100, 149, 237)); // Fondo del encabezado (azul)
        tabla.getTableHeader().setForeground(Color.WHITE); // Color del texto del encabezado

        // Centrar texto en las celdas
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(centrado);
        }

        // Ajustar el ancho de las columnas
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Agregar la tabla a un JScrollPane para el desplazamiento
        JScrollPane scrollPane = new JScrollPane(tabla);

        JButton actualizarBtn = new JButton("Prueba actualizar Tabla");
        actualizarBtn.addActionListener(e -> {
            // Nuevos datos para actualizar la tabla

            agents.put(new AgentKey_Monitor(Integer.toString(num_agents)),new AgentInfo_Monitor(Integer.toString(num_agents),felicidad_prueba,2,8,true));
            num_agents += 1;
            felicidad_prueba += 10;
            actualizarTabla(); // Llamar al método de actualización
        });




        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(actualizarBtn, BorderLayout.SOUTH);
        // Mostrar el marco
        frame.setVisible(true);
    }

    // Función para extraer el origin_id del contenido XML
    public static String getOriginId(String xmlContent) {
        try {
            // Configura el analizador XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parsear el contenido XML desde la cadena
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            // Crea un objeto XPath para realizar la búsqueda en el documento
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            // Expresión XPath para obtener el elemento origin_id
            XPathExpression expression = xpath.compile("/Message/header/origin/origin_id");

            // Busca el nodo origin_id en el XML
            Node node = (Node) expression.evaluate(doc, XPathConstants.NODE);

            // Retorna el contenido de origin_id, o null si no se encuentra
            return (node != null) ? node.getTextContent() : null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //Función para actualizar la información de la tabla
    public static void ActualizarInfo(String xmlContent, String id) {

        try {
            // Configura el analizador XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parsear el contenido XML desde la cadena
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            // Crea un objeto XPath para realizar la búsqueda en el documento
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            // Expresión XPath para obtener el elemento happiness
            XPathExpression expression_felicidad = xpath.compile("/Message/monitor_info/happiness");
            // Busca el nodo happiness en el XML
            Node node_felicidad = (Node) expression_felicidad.evaluate(doc, XPathConstants.NODE);

            // Retorna el contenido de happiness, o null si no se encuentra
            String felicidad = (node_felicidad != null) ? node_felicidad.getTextContent() : null;

            // Expresión XPath para obtener el elemento num_cards
            XPathExpression expression_num_cromos = xpath.compile("/Message/monitor_info/num_cards");
            // Busca el nodo num_cards en el XML
            Node node_num_cromos = (Node) expression_num_cromos.evaluate(doc, XPathConstants.NODE);

            // Retorna el contenido de num_cards, o null si no se encuentra
            String num_cromos = (node_num_cromos != null) ? node_num_cromos.getTextContent() : null;


            // Expresión XPath para obtener el elemento completed_sets
            XPathExpression expression_num_sets = xpath.compile("/Message/monitor_info/completed_sets");
            // Busca el nodo completed_sets en el XML
            Node node_num_sets = (Node) expression_num_sets.evaluate(doc, XPathConstants.NODE);
            // Retorna el contenido de completed_sets, o null si no se encuentra
            String num_sets = (node_num_sets != null) ? node_num_sets.getTextContent() : null;

            // Expresión XPath para obtener el elemento ladrón
            XPathExpression expression_ladron = xpath.compile("/Message/trading_block/steal");
            // Busca el nodo completed_sets en el XML
            Node node_ladron= (Node) expression_ladron.evaluate(doc, XPathConstants.NODE);
            // Retorna el contenido de completed_sets, o null si no se encuentra
            String ladron = (node_ladron != null) ? node_ladron.getTextContent() : null;

            //Si el agente no está en el hashmap se añade, en cualquier caso se actualiza la tabla
            if (!agents.containsKey(id)) {
                agents.put(new AgentKey_Monitor(id), new AgentInfo_Monitor(id,Integer.parseInt(felicidad),Integer.parseInt(num_sets),Integer.parseInt(num_cromos),Boolean.getBoolean(ladron)));
                actualizarTabla();
            } else {
                actualizarTabla();
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    //Función para actualizar la propia tabla
    public static void actualizarTabla() {

        modelo.setRowCount(0);
        modelo.setRowCount(0);
        int total_agentes = agents.size();
        int total_felicidades = 0;
        int total_sets = 0;
        int total_cromos = 0;
        ArrayList<Object[]> listaDatos = new ArrayList<>();


        //Recorremos el hashmap para obtener los atributos de los agentes
        for (Map.Entry<AgentKey_Monitor, AgentInfo_Monitor> entry : agents.entrySet()) {

            Object[] dato = {entry.getValue().getId(), entry.getValue().getNumCromos(),entry.getValue().getNumSets_Completados(), entry.getValue().getFelicidad(),entry.getValue().isLadron()};
            listaDatos.add(dato);

            total_felicidades+= entry.getValue().getFelicidad();
            total_cromos+= entry.getValue().getNumCromos();
            total_sets+= entry.getValue().getNumSets_Completados();

        }

        total_felicidades = total_felicidades / total_agentes;

        //Dependiendo de la felicidad total el color de la tabla va a ser diferente
        if (total_felicidades >= 75){
            tabla.getTableHeader().setBackground(new Color(100, 149, 237)); // Fondo del encabezado (verde)
        }
        else if (total_felicidades >= 50){
            tabla.getTableHeader().setBackground(new Color(0, 128, 0)); // Fondo del encabezado (amarilla)
        }

        else if (total_felicidades >= 25){
            tabla.getTableHeader().setBackground(new Color(255, 165, 0)); // Fondo del encabezado (naranja)
        }
        else{
            tabla.getTableHeader().setBackground(new Color(255, 0, 0)); // Fondo del encabezado (rojo)
        }


        //Se añade la fila principal
        Object[] dato = {"Total", total_cromos,total_sets,total_felicidades,""};
        listaDatos.add(0,dato);
        //Se añaden las filas de todos loas agentes
        Object[][] arrayDatos = listaDatos.toArray(new Object[0][0]);
        Object ladron = true;
        for (Object[] fila : arrayDatos) {
            modelo.addRow(fila);
        }


    }
    //Generamos el XSD con todos los mensajes
    public static void generarXsd(String xmlContent,String origin_id, String type) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parsear el contenido XML desde la cadena
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            //Creamos el csv si no ha sido creado
            if (!Files.exists(Paths.get("mensajes.csv"))){
                PrintWriter writer = new PrintWriter(new FileWriter("mensajes.csv"));
                writer.println("OriginID,DestinationID,TypeProtocol,ComunicationProtocol,ProtocolStep,origin_time"); // Encabezados del CSV
            }

            PrintWriter writer = new PrintWriter(new FileWriter("mensajes.csv"));

            //Obtenemos los atributos que no estaban
            XPathExpression expression_Dest_id = xpath.compile("/Message/header/destination/destination_id");
            Node node_Dest_id = (Node) expression_Dest_id.evaluate(doc, XPathConstants.NODE);
            String Dest_id = (node_Dest_id != null) ? node_Dest_id.getTextContent() : null;

            XPathExpression expression_protocol = xpath.compile("/Message/header/comunication_protocol");
            Node node_protocol = (Node) expression_protocol.evaluate(doc, XPathConstants.NODE);
            String protocol = (node_protocol != null) ? node_protocol.getTextContent() : null;

            XPathExpression expression_protocol_step = xpath.compile("/Message/header/protocol_step");
            Node node_protocol_step = (Node) expression_protocol_step.evaluate(doc, XPathConstants.NODE);
            String protocol_step = (node_protocol_step != null) ? node_protocol_step.getTextContent() : null;

            XPathExpression expression_time = xpath.compile("/Message/header/origin/origin_time");
            Node node_time= (Node) expression_time.evaluate(doc, XPathConstants.NODE);
            String time = (node_time != null) ? node_time.getTextContent() : null;

            //Añadimos la fila
            writer.println(origin_id + "," +Dest_id + "," + type +"," + protocol +"," +protocol_step+","+ time);

            // Cerramos el archivo CSV
            writer.close();




        } catch (Exception e) {
        e.printStackTrace();
        }
    }


    // Clase para manejar cada cliente conectado
    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                // Leer el mensaje del cliente
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                StringBuilder xmlContentBuilder = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    xmlContentBuilder.append(line).append("\n");
                }

                String xmlContent = xmlContentBuilder.toString();

                // Validar el mensaje
                if (Monitor.validate(xmlContent)) {
                    // Extraer el tipo de protocolo
                    String type = Monitor.getTypeProtocol(xmlContent);

                    // Extraer el origin_id
                    String originId = Monitor.getOriginId(xmlContent);

                    // Si no se pudo obtener el originId, usar la dirección IP del cliente
                    if (originId == null || originId.isEmpty()) {
                        originId = clientSocket.getInetAddress().getHostAddress();
                    }

                    // Dependiendo del tipo, imprimir el mensaje correspondiente
                    switch (type) {
                        case "heNacido":
                            System.out.println("El agente " + originId + " acaba de nacer en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent,  originId);
                            generarXsd(xmlContent,originId,type);
                            break;
                        case "parado":
                            System.out.println("El agente " + originId + " se va a parar en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent, originId);
                            generarXsd(xmlContent,originId,type);
                            break;
                        case "continua":
                            System.out.println("El agente " + originId + " va a continuar en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent,  originId);
                            generarXsd(xmlContent,originId,type);
                            break;
                        case "meMuero":
                            System.out.println("El agente " + originId + " se va a morir en " + clientSocket.getInetAddress().getHostAddress());
                            agents.remove((new AgentKey_Monitor(originId)));
                            actualizarTabla();
                            generarXsd(xmlContent,originId,type);
                            break;
                        default:
                            System.out.println("Tipo de mensaje desconocido de " + originId + ": " + type);
                            break;
                    }

                } else {
                    System.out.println("Mensaje inválido recibido de " + clientSocket.getInetAddress().getHostAddress());
                }

                        // Cerrar recursos
                in.close();
                clientSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
