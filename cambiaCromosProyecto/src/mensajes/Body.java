package mensajes;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Body {

    @XmlElement(name = "body_info", required = true)
    private String bodyInfo;

    // Getters y setters
    public String getBodyInfo() { return bodyInfo; }
    public void setBodyInfo(String bodyInfo) { this.bodyInfo = bodyInfo; }
}

