package pk;

import java.io.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.Semaphore;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

public class Monitor {
    private final Map<Integer, AgenteInfo> mapaAgentes;
    private final Semaphore semaphore = new Semaphore(1);


    public Monitor() {
        mapaAgentes = new ConcurrentHashMap<>();
    }


    // Agrega un id y una clase nueva agenteInfo ( representacion de un agente de nuestro sistema ) al hash map del monitor
    public void agregarAgente(int id,String controladorIP, int controladorPuerto ) {
        AgenteInfo agenteInfo = new AgenteInfo(id, controladorIP,controladorPuerto);
        mapaAgentes.put(id, agenteInfo);
        registrarActividad(agenteInfo,"Added");
    }

    // Devuelve el hash map de agentes completos
    public Map<Integer, AgenteInfo> obtenerTodosLosAgentes() {
        return new ConcurrentHashMap<>(mapaAgentes);
    }

    // Devuelve la informacion de un agente por su id
    public AgenteInfo obtenerAgente(int id) {
        return mapaAgentes.get(id);
    }

    // Elimina un agente del hash map del monitor
    public void eliminarAgente(int id) {
        AgenteInfo agente = mapaAgentes.get(id);
        if (agente != null) {
            registrarActividad(agente,"Removed");
            mapaAgentes.remove(id);
        }
    }

    // Incrementa en uno el contador de inactividad de un agente
    public void incrementarContadorInactividad(int id) {
        AgenteInfo agente = mapaAgentes.get(id);
        if (agente != null) {
            agente.incrementarContadorInactividad();
        }
    }

    // Pone a cero el contador de inactividad de un agente
    public void reiniciarContadorInactividad(int id) {
        AgenteInfo agente = mapaAgentes.get(id);
        if (agente != null) {
            agente.reiniciarContadorInactividad();
            //TO DO aqui podriamos registrar la actividad de que se ha reiniciado el contador
            //pero eso ya estara reflejado en el registro, pues las actividades se veran tmb
        }
    }


    // Método registrarActividad utilizando Semaphore
    private void registrarActividad(AgenteInfo agenteInfo, String tipo) {
        String nombreArchivo = "RegistroEntradaSalidaAgentes.xml";
        File archivo = new File(nombreArchivo);

        try {
            // Adquirimos el permiso del semáforo
            semaphore.acquire();

            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc;

                if (!archivo.exists() || archivo.length() == 0) {
                    // Si no existe, o está vacío, lo creamos y añadimos el elemento raíz
                    doc = dBuilder.newDocument();
                    Element elementoRaiz = doc.createElement("RegistroEntradaSalidaAgentes");
                    doc.appendChild(elementoRaiz);
                } else {
                    // Si ya existe, lo cargamos para añadir los nuevos elementos
                    FileInputStream fis = new FileInputStream(archivo);
                    doc = dBuilder.parse(fis);
                    fis.close();
                }

                // Creamos el nuevo elemento Actividad
                Element elementoActividad = doc.createElement("Actividad");

                // Tipo de actividad
                Element elementoTipo = doc.createElement("Tipo");
                elementoTipo.appendChild(doc.createTextNode(tipo));
                elementoActividad.appendChild(elementoTipo);

                // Fecha y hora
                Element elementoFechaHora = doc.createElement("FechaHora");
                LocalDateTime fechaHoraActual = LocalDateTime.now();
                DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String fechaHoraFormateada = fechaHoraActual.format(formato);
                elementoFechaHora.appendChild(doc.createTextNode(fechaHoraFormateada));
                elementoActividad.appendChild(elementoFechaHora);

                // Información del agente
                Element elementoAgente = doc.createElement("Agente");

                // ID del agente
                Element elementoID = doc.createElement("ID");
                elementoID.appendChild(doc.createTextNode(String.valueOf(agenteInfo.getId())));
                elementoAgente.appendChild(elementoID);

                // Controlador IP
                Element elementoIP = doc.createElement("ControladorIP");
                elementoIP.appendChild(doc.createTextNode(agenteInfo.getControladorIP()));
                elementoAgente.appendChild(elementoIP);

                // Controlador Puerto
                Element elementoPuerto = doc.createElement("ControladorPuerto");
                elementoPuerto.appendChild(doc.createTextNode(String.valueOf(agenteInfo.getControladorPuerto())));
                elementoAgente.appendChild(elementoPuerto);

                // Contador de inactividad
                Element elementoContador = doc.createElement("ContadorInactividad");
                elementoContador.appendChild(doc.createTextNode(String.valueOf(agenteInfo.getContadorInactividad())));
                elementoAgente.appendChild(elementoContador);

                // Agrega el elemento Agente al elemento Actividad
                elementoActividad.appendChild(elementoAgente);

                // Agregamos el elemento Actividad al nodo raíz
                Node raiz = doc.getDocumentElement();
                raiz.appendChild(elementoActividad);

                // Escribe el contenido en el archivo XML
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                // Opcional: establece propiedades de salida
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new FileOutputStream(archivo));

                transformer.transform(source, result);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Liberamos el permiso del semáforo
                semaphore.release();
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("El hilo fue interrumpido mientras esperaba el semáforo.");
        }
    }


}
