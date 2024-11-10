package XMLParser;

import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.transform.stream.StreamSource;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.xml.sax.SAXException;

import mensajes.Message;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

public class XMLParser {

    // Ruta al archivo XSD
    private static final String XSD_PATH = "XMLParser/esquema.xsd";

    // Método para validar un XML contra el XSD
    public static boolean validateXml(String xmlContent) {
        try {
            // Crear un SchemaFactory para el esquema XML
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // Cargar el esquema desde el archivo
            File schemaFile = new File(XSD_PATH);
            Schema schema = factory.newSchema(schemaFile);

            // Crear un validador a partir del esquema
            Validator validator = schema.newValidator();

            // Validar el contenido XML
            validator.validate(new StreamSource(new StringReader(xmlContent)));
            return true;

        } catch (SAXException e) {
            System.out.println("Error de validación XML: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error de I/O: " + e.getMessage());
        }
        return false; // Retorna false si el XML no es válido
    }

    public static String createXmlMessage(Message message) {
        try {
            // Crear el contexto JAXB y el Marshaller
            JAXBContext context = JAXBContext.newInstance(Message.class);
            Marshaller marshaller = context.createMarshaller();

            // Configurar el marshaller para que formatee el XML de salida
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            // Convertir el objeto Message a XML
            StringWriter writer = new StringWriter();
            marshaller.marshal(message, writer);
            return writer.toString();

        } catch (JAXBException e) {
            e.printStackTrace();
            return null; // Devuelve null si ocurre un error durante la serialización
        }
    }

    public static Message parseXmlMessage(String xmlContent) {
        try {
            // Crear el contexto JAXB y el Unmarshaller
            JAXBContext context = JAXBContext.newInstance(Message.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            // Convertir el XML en un objeto Message
            StringReader reader = new StringReader(xmlContent);
            Message message = (Message) unmarshaller.unmarshal(reader);

            return message; // Retornar el objeto Message

        } catch (JAXBException e) {
            e.printStackTrace();
            System.out.println("Error al parsear el XML en un objeto Message.");
            return null; // Devuelve null si ocurre un error durante la deserialización
        }
    }
}
