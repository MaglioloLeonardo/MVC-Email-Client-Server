package App;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Email implements Serializable {

    private String subject;
    private String sender;
    private String text;
    private String[] receivers;
    private Float size;
    private Date date;


    public Email(){}

    public Email(String subject, String sender, String[] receivers,  String text) {
        this.subject = subject;
        this.sender = sender;
        this.receivers = receivers;
        this.text = text;
        date = new Date(System.currentTimeMillis());

        final byte[] utf16Bytes= text.getBytes(StandardCharsets.UTF_16BE);
        size = new Float(utf16Bytes.length);
    }


    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Float getSize() {
        return size;
    }

    public void setSize(Float size) {
        this.size = size;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String[] getReceivers() { return receivers; }

    public void setReceivers(String[] receivers) { this.receivers = receivers; }

    public String getStringreceivers() {
        String stringreceivers = "[";
        for(int i = 0; i < receivers.length; i++){
            stringreceivers += receivers[i];
            if(i < receivers.length-1)stringreceivers += ", ";
        }
        stringreceivers += "]";
        return stringreceivers;
    }
}