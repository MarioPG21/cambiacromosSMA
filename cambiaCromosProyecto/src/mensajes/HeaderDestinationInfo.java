package mensajes;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class HeaderDestinationInfo {

    @XmlElement(name = "destination_id", required = true)
    private String destinationId;

    @XmlElement(name = "destination_ip", required = true)
    private String destinationIp;

    @XmlElement(name = "destination_port_UDP", required = true)
    private Integer destinationPortUDP;

    @XmlElement(name = "destination_port_TCP", required = true)
    private Integer destinationPortTCP;

    @XmlElement(name = "destination_time", required = true)
    private Long destinationTime;

    // Getters y setters
    public String getDestinationId() { return destinationId; }
    public void setDestinationId(String destinationId) { this.destinationId = destinationId; }

    public String getDestinationIp() { return destinationIp; }
    public void setDestinationIp(String destinationIp) { this.destinationIp = destinationIp; }

    public Integer getDestinationPortUDP() { return destinationPortUDP; }
    public void setDestinationPortUDP(Integer destinationPortUDP) { this.destinationPortUDP = destinationPortUDP; }

    public Integer getDestinationPortTCP() { return destinationPortTCP; }
    public void setDestinationPortTCP(Integer destinationPortTCP) { this.destinationPortTCP = destinationPortTCP; }

    public Long getDestinationTime() { return destinationTime; }
    public void setDestinationTime(Long destinationTime) { this.destinationTime = destinationTime; }
}
