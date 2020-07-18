package App;

import java.io.Serializable;
import java.util.Date;

public class LogItem implements Serializable {
    private String type;
    private String subtype;
    private Date date;

    public LogItem(String type, String subtype){
        this.type = type;
        this.subtype = subtype;
        date = new Date(System.currentTimeMillis());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String toString(){
        return "<" + type + ", " + subtype + ", " + date + ">";
    }
}
