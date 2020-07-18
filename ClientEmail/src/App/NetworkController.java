package App;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.util.Pair;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class NetworkController{
    public SimpleStringProperty IpAddr = new SimpleStringProperty();
    public SimpleIntegerProperty port = new SimpleIntegerProperty();
    public SimpleStringProperty state = new SimpleStringProperty();
    Thread socketUpdate, sendEmails, receiveEmails;
    Socket connection;
    boolean isEstablished = false, threadStop = false;
    ObjectOutputStream out;
    ObjectInputStream in;

    private Map<String, List<Pair<String, ObservableList<Email>>>> listTable;
    private String[] accounts;

    public NetworkController(SimpleStringProperty IpAddr, SimpleIntegerProperty port, SimpleStringProperty state , String[] accounts, Map<String, List<Pair<String,ObservableList<Email>>>> listTable){
        this.IpAddr.bind(IpAddr);
        this.port.bind(port);
        this.state.bindBidirectional(state);
        this.accounts = accounts;
        this.listTable = listTable;

        socketUpdate = new Thread(this::socketUpdate);
        sendEmails = new Thread(this::sendMails);
        receiveEmails = new Thread(this::receiveEmails);

        socketUpdate.setDaemon(true);
        sendEmails.setDaemon(true);
        receiveEmails.setDaemon(true);
    }

    public void start(){
        socketUpdate.start();
        sendEmails.start();
        receiveEmails.start();
    }


    public void stop(){
        Thread stop = new Thread(()->{
            threadStop = true;
            try{
                socketUpdate.join();
                sendEmails.join();
                connection.close();
            }catch (Exception e){};
        });
        stop.start();
    }


    //private-------------------------------
    private void socketUpdate(){
        boolean toggle = false;
        String prevIpAddr = "";
        int prevPort = 0;
        while (true){
            if(threadStop)break;
            try {
                if(!isEstablished || prevIpAddr.compareTo(IpAddr.get()) != 0 || prevPort != port.get()){
                    if(!(toggle && !isEstablished)) toggle = false;
                    isEstablished = false;
                    if(connection != null)connection.close();
                    prevIpAddr = IpAddr.get(); prevPort = port.get();
                    connection = new Socket(IpAddr.get(), port.get());
                    System.out.println(connection.getInputStream());
                    try{Thread.sleep(100);}catch (Exception e){};
                    out = new ObjectOutputStream(connection.getOutputStream());
                    in = new ObjectInputStream(connection.getInputStream());
                    toggle = true;
                    isEstablished = connectionInizializer();

                    if(isEstablished) {
                        Platform.runLater(() -> state.set("Connected"));
                    }else{
                        Platform.runLater(() -> state.set("Connecting"));
                    }
                }
            }catch (IOException e){
                if((e instanceof ConnectException)&& !toggle){
                    genErrorPopup();
                    toggle = true;
                    isEstablished = false;
                    Platform.runLater(() -> state.set("Connecting"));
                }//else e.printStackTrace();
            }
            try {Thread.sleep(Settings.autoConnectTime);}catch (Exception e){};
        }
    }

    private boolean connectionInizializer(){
        try {
            try{
                Pair<String, ArrayList<String>> toSend = new Pair<String, ArrayList<String>>("AUTH", new ArrayList<String>(Arrays.asList(accounts)));
                if(out == null)return false;
                out.writeObject(toSend);
                out.flush();
                return true;
            }catch (Exception e){
                e.printStackTrace();
                connection.close();
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return  false;
        }
    }

    private void sendMails() {
        List<ObservableList<Email>> toClear = new ArrayList<>();
        while (true) {
            if (threadStop) break;
            if (isEstablished && connection != null && connection.isConnected() && out != null) {
                synchronized (listTable) {
                    for (String entry : listTable.keySet()) {
                        for (Pair<String, ObservableList<Email>> pair : listTable.get(entry)) {
                            if (pair.getKey().compareTo("Pending") == 0) {
                                toClear.add(pair.getValue());
                                for (Email email : pair.getValue()) {
                                    try {
                                        out.writeObject(email);
                                        out.flush();
                                    } catch (IOException e) {
                                        if (e.getMessage().compareTo("Connection reset") == 0 || e.getMessage().compareTo("Socket closed") == 0){
                                            isEstablished = false;
                                            Platform.runLater(() -> state.set("Connecting"));
                                        }else{
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (ObservableList<Email> list : toClear) {
                synchronized (list) {
                    list.clear();
                }
            }

            try {
                Thread.sleep(500);
            } catch (Exception e) { }
        }
    }

    private void receiveEmails() {
        while (true) {
            if (threadStop) break;
            if (isEstablished && connection != null && connection.isConnected() && in != null) {
                try {
                    Pair<String, Email> message = (Pair<String, Email>) in.readObject();
                    Email thisEmail = message.getValue();
                    System.out.println("Arrived pair:" + message);
                    switch (message.getKey()) {
                        case "NACK": {
                            for (String accounts : thisEmail.getReceivers()) {
                                if (Arrays.asList(Settings.localAccounts).contains(accounts))
                                    addEmail(accounts, "Rejected", thisEmail);
                            }
                            break;
                        }

                        case "EMAIL": {
                            for (String accounts : thisEmail.getReceivers()) {
                                if (Arrays.asList(Settings.localAccounts).contains(accounts))
                                    addEmail(accounts, "Inbox", thisEmail);
                            }
                            break;
                        }
                    }

                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().compareTo("Connection reset") == 0 || e.getMessage().compareTo("Socket closed") == 0)){
                        isEstablished = false;
                        Platform.runLater(() -> state.set("Connecting"));
                    }else{
                        e.printStackTrace();
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
    }

    private void addEmail(String account, String field, Email email){
        ObservableList<Email> list = null;
        List<Pair<String,ObservableList<Email>>> tempList = listTable.get(account);
        for (Pair<String, ObservableList<Email>> pair:tempList) {
            if((pair.getKey()).compareTo(field) == 0){
                list =  pair.getValue();
                break;
            }
        }
        if(list != null)list.add(email);
    }

    private void genErrorPopup(){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("Connection error:");
            alert.setContentText("It was impossible to connect at ip address:" + IpAddr.get() + " port:" + port.get()
                    + ", the client will attempt to reconnect at regular intervals until the connection parameters will be changed");
            alert.showAndWait();
        });
    }
}
