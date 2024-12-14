package monitor;
import agente.Message;
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
    private static Object[][] datos = {{"Total", 0, 0, 0},};
    private static DefaultTableModel modelo = new DefaultTableModel(datos, columnas);


    public static void main(String[] args) {
        //Creamos el archivo xsd
        try (PrintWriter writer = new PrintWriter(new FileWriter("mensajes.csv"))) {
            writer.println("OriginID,DestinationID,TypeProtocol,ComunicationProtocol,ProtocolStep,origin_time"); // Encabezados del CSV
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Crear la JTable con el modelo
        tabla = new JTable(modelo);
        //Creamos la tabla
        crearTabla();
        try {
            //Creamos el server socket
            int port = 4300;
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Monitor está escuchando en el puerto " + port);
            while (true) {
                //Crea un hilo para gestionar las peticiones del agente
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

    //Función para crear la tabla con la información del sistema
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

    //Función para actualizar la información de la tabla
    public static void ActualizarInfo(String xmlContent) {
        try {
            // Sacamos los atributos de la clase mensaje
            Message message = new Message(xmlContent);

            String id = message.getOriginId();
            int felicidad = message.getHappiness();
            if (felicidad == -1) {
                felicidad = 0;
            }
            int num_sets = message.getCompletedSets();

            if (num_sets == -1) {
                num_sets = 0;
            }
            int num_cromos = message.getNumCards();

            if (num_cromos == -1) {
                num_cromos = 0;
            }
            boolean ladron = message.isSteal();

            //Si el agente no está en el hashmap se añade, en cualquier caso se actualiza la tabla
            if (!agents.containsKey(id)) {
                agents.put(new AgentKey_Monitor(id), new AgentInfo_Monitor(id,felicidad,num_sets,num_cromos,ladron));
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
        //Eliminamos las filas de latabla
        modelo.setRowCount(0);
        //Ponemos a 0 los atributos generales
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
    public static void generarXsd(String xmlContent) {
        try {

            //Sacamos los atributos del mensaje
            Message message = new Message(xmlContent);
            String Dest_id = message.getDestId();
            String protocol = message.getProtocol();
            int protocol_step = message.getProtocolStep();
            String origin_id = message.getOriginId();
            String type = message.getProtocol();
            String time = message.getOriginTime();
            //Los añadimos al  xsd
            try (PrintWriter writer = new PrintWriter(new FileWriter("mensajes.csv", true))) {
                writer.println(origin_id + "," + Dest_id + "," + type + "," + protocol + "," + protocol_step + "," + time);
            }
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

                    Message message = new Message(xmlContent);
                    // Extraer el tipo de protocolo
                    String type = message.getProtocol();

                    // Extraer el origin_id
                    String originId = message.getOriginId();

                    // Si no se pudo obtener el originId, usar la dirección IP del cliente
                    if (originId == null || originId.isEmpty()) {
                        originId = clientSocket.getInetAddress().getHostAddress();
                    }

                    // Dependiendo del tipo, imprimir el mensaje correspondiente
                    switch (type) {
                        case "heNacido":
                            System.out.println("El agente " + originId + " acaba de nacer en " + clientSocket.getInetAddress().getHostAddress());
                            System.out.println("a:" + xmlContent);
                            ActualizarInfo(xmlContent);
                            generarXsd(xmlContent);
                            break;
                        case "parado":
                            System.out.println("El agente " + originId + " se va a parar en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent);
                            generarXsd(xmlContent);
                            break;
                        case "continua":
                            System.out.println("El agente " + originId + " va a continuar en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent);
                            generarXsd(xmlContent);
                            break;
                        case "meMuero":
                            System.out.println("El agente " + originId + " se va a morir en " + clientSocket.getInetAddress().getHostAddress());
                            agents.remove((new AgentKey_Monitor(originId)));
                            actualizarTabla();
                            generarXsd(xmlContent);
                            break;
                        case "intercambio":
                            System.out.println("El agente " + originId + " intenta intercambiar en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent);
                            generarXsd(xmlContent);
                            break;
                        case "decision":
                            System.out.println("El agente " + originId + " toma una decisión en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent);
                            generarXsd(xmlContent);
                        case "ofertaInicial":
                            System.out.println("El agente " + originId + " toma una decisión en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent);
                            generarXsd(xmlContent);
                        case "meInteresa":
                            System.out.println("El agente " + originId + " toma una decisión en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent);
                            generarXsd(xmlContent);
                        case "nomeInteresa":
                            System.out.println("El agente " + originId + " toma una decisión en " + clientSocket.getInetAddress().getHostAddress());
                            ActualizarInfo(xmlContent);
                            generarXsd(xmlContent);
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
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
