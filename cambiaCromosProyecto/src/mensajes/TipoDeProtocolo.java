package mensajes;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum TipoDeProtocolo {
    @XmlEnumValue("reproducete") REPRODUCETE,
    @XmlEnumValue("heNacido") HENACIDO,
    @XmlEnumValue("parate") PARATE,
    @XmlEnumValue("parado") PARADO,
    @XmlEnumValue("continua") CONTINUA,
    @XmlEnumValue("continuo") CONTINUO,
    @XmlEnumValue("autodestruyete") AUTODESTRUYETE,
    @XmlEnumValue("meMuero") MEMUERTO,
    @XmlEnumValue("hola") HOLA,
    @XmlEnumValue("estoy") ESTOY
}
