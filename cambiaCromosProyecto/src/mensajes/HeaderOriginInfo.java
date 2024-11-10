package mensajes;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class HeaderOriginInfo {

    @XmlElement(name = "origin_id", required = true)
    private String originId;

    @XmlElement(name = "origin_ip", required = true)
    private String originIp;

    @XmlElement(name = "origin_port_UDP", required = true)
    private Integer originPortUDP;

    @XmlElement(name = "origin_port_TCP", required = true)
    private Integer originPortTCP;

    @XmlElement(name = "origin_time", required = true)
    private Long originTime;

    // Getters y setters
    public String getOriginId() { return originId; }
    public void setOriginId(String originId) { this.originId = originId; }

    public String getOriginIp() { return originIp; }
    public void setOriginIp(String originIp) { this.originIp = originIp; }

    public Integer getOriginPortUDP() { return originPortUDP; }
    public void setOriginPortUDP(Integer originPortUDP) { this.originPortUDP = originPortUDP; }

    public Integer getOriginPortTCP() { return originPortTCP; }
    public void setOriginPortTCP(Integer originPortTCP) { this.originPortTCP = originPortTCP; }

    public Long getOriginTime() { return originTime; }
    public void setOriginTime(Long originTime) { this.originTime = originTime; }
}
