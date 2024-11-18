package monitor;

import java.net.*;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import javax.xml.XMLConstants;
import org.xml.sax.InputSource;

public class Monitor {

    public static void main(String[] args) {
        int port = 4300;
        try {
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
                            System.out.println("El agente " + originId + " acaba de nacer.");
                            break;
                        case "parado":
                            System.out.println("El agente " + originId + " se va a parar.");
                            break;
                        case "continua":
                            System.out.println("El agente " + originId + " va a continuar.");
                            break;
                        case "meMuero":
                            System.out.println("El agente " + originId + " se va a morir.");
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
