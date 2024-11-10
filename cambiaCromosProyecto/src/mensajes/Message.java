package mensajes;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "Message")
@XmlAccessorType(XmlAccessType.FIELD)
public class Message {

    @XmlElement(name = "comunc_id", required = true)
    private String comuncId;

    @XmlElement(name = "msg_id", required = true)
    private String msgId;

    @XmlElement(name = "header", required = true)
    private Header header;

    @XmlElement(name = "body", required = true)
    private Body body;

    @XmlElement(name = "common_content", required = true)
    private CommonContent commonContent;

    // Getters y setters
    public String getComuncId() { return comuncId; }
    public void setComuncId(String comuncId) { this.comuncId = comuncId; }

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }

    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }

    public Body getBody() { return body; }
    public void setBody(Body body) { this.body = body; }

    public CommonContent getCommonContent() { return commonContent; }
    public void setCommonContent(CommonContent commonContent) { this.commonContent = commonContent; }
}
