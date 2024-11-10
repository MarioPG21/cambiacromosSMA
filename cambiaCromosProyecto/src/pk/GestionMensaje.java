package pk;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.Socket;

public class GestionMensaje implements Runnable {
    private Socket socket;
    public GestionMensaje(Socket socket) {this.socket = socket;}


    @Override
    public void run() {
        try {
            // Lector para recibir los datos del socket
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            StringBuilder mensaje = new StringBuilder();
            String respuesta = null;
            if (validate(mensaje.toString())){
                String tipo_Protocolo = getTypeProtocol(mensaje.toString());
            }else{
                System.out.println("El mensaje no ha sido validado");
            }
            // Cierre del lector y del socket
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean validate(String xmlContent) {
        String xsdFilePath = "C:\\Users\\alvar\\OneDrive - Universidad de Castilla-La Mancha\\Escritorio\\SMA\\cambiaCromosProyecto\\src\\pk\\esquema_AG_basico_01.xsd";  // Ruta al archivo XSD

        // Crear una fábrica de esquemas que entienda XSD
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        try {
            // Cargar el esquema XSD
            Schema schema = schemaFactory.newSchema(new File(xsdFilePath));

            // Crear una instancia de Validator
            Validator validator = schema.newValidator();

            // Validar el contenido XML (desde la cadena) contra el esquema
            validator.validate(new StreamSource(new StringReader(xmlContent)));
            System.out.println("XML es válido contra el XSD.");
            return true;

        } catch (Exception e) {
            System.out.println("XML no es válido: " + e.getMessage());
            return false;
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



}
