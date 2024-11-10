package mensajes;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Header {

    @XmlElement(name = "type_protocol", required = true)
    private TipoDeProtocolo typeProtocol;

    @XmlElement(name = "protocol_step", required = true)
    private Integer protocolStep;

    @XmlElement(name = "comunication_protocol", required = true)
    private String comunicationProtocol;

    @XmlElement(name = "origin", required = true)
    private HeaderOriginInfo origin;

    @XmlElement(name = "destination", required = true)
    private HeaderDestinationInfo destination;

    // Getters y setters
    public TipoDeProtocolo getTypeProtocol() { return typeProtocol; }
    public void setTypeProtocol(TipoDeProtocolo typeProtocol) { this.typeProtocol = typeProtocol; }

    public Integer getProtocolStep() { return protocolStep; }
    public void setProtocolStep(Integer protocolStep) { this.protocolStep = protocolStep; }

    public String getComunicationProtocol() { return comunicationProtocol; }
    public void setComunicationProtocol(String comunicationProtocol) { this.comunicationProtocol = comunicationProtocol; }

    public HeaderOriginInfo getOrigin() { return origin; }
    public void setOrigin(HeaderOriginInfo origin) { this.origin = origin; }

    public HeaderDestinationInfo getDestination() { return destination; }
    public void setDestination(HeaderDestinationInfo destination) { this.destination = destination; }
}
