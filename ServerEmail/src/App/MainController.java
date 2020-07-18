package App;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MainController implements Initializable {
    @FXML TableView<LogItem> logTableView;
    @FXML TableColumn<LogItem, String> typeCol;
    @FXML TableColumn<LogItem, String> subtypeCol;
    @FXML TableColumn<LogItem, Date> dateCol;
    @FXML ListView<String> accountListView;

    private ExecutorService exec_pool = Executors.newFixedThreadPool(Settings.mainPollStartSize,
            r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            });

    MainModel model;
    ServerSocket ss;

    public MainController(MainModel model){
        this.model = model;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeCol.setCellValueFactory(new PropertyValueFactory<LogItem, String>("type"));
        subtypeCol.setCellValueFactory(new PropertyValueFactory<LogItem, String>("subtype"));
        dateCol.setCellValueFactory(new PropertyValueFactory<LogItem, Date>("date"));
        dateCol.setSortType(TableColumn.SortType.ASCENDING);

        model.resumeMessages();
        model.setLogOnTable(logTableView);
        model.setAccountOnList(accountListView);

        try{
            for(Thread t: Thread.getAllStackTraces().keySet()){
                if(t.getName().compareTo(Settings.resumeThreadName) == 0){
                    t.join();
                    break;
                }
            }
        }catch (Exception e){}

        Thread connectionsHandler, emailReceiver, emailSender;
        connectionsHandler = new Thread(this::connectionsHandler);
        emailReceiver = new Thread(this::emailReceiver);
        emailSender = new Thread(this::emailSender);

        connectionsHandler.setDaemon(true);
        emailReceiver.setDaemon(true);
        emailSender.setDaemon(true);

        connectionsHandler.start();
        emailReceiver.start();
        emailSender.start();
    }

    public void deleteAllLogItem(){
        model.deleteAllLogItem();
    }

    public void copyLogItem(){
        LogItem selected = logTableView.getSelectionModel().getSelectedItem();
        if(selected != null){
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(selected.toString());
            clipboard.setContent(content);
        }
    }

    public void deleteLogItem(){
        LogItem selected = logTableView.getSelectionModel().getSelectedItem();
        if(selected != null)model.deleteLogItem(selected);
    }

    public void poolShutdown(){
        exec_pool.shutdown();
    }

    //_____________________________________________________________________

    private void connectionsHandler(){
        while (ss == null){
            try {ss = new ServerSocket(Settings.defaultPort);}catch (Exception e){};
        }

        while (true){
            try {
                Socket s = ss.accept();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                Pair<String, ArrayList<String>> message = null;

                try {
                     message = (Pair<String, ArrayList<String>>) in.readObject();
                }catch (Exception e){
                    e.printStackTrace();
                    out.writeObject(new Pair<String, Object>("AUTH_ERROR", "ImpossibleToCastAuthMessage"));
                    out.flush();
                    s.close();
                    model.addLogItem(new LogItem("Error", "Auth error: from " + s.getInetAddress().getHostAddress()));
                }

                if(message != null){
                    if(message.getKey().compareTo("AUTH") == 0){
                        ArrayList<String> accounts = (ArrayList<String>) message.getValue();
                        for(String account: accounts){
                            model.addAccount(account, s, in, out);
                        }
                        model.addLogItem(new LogItem("ActionInfo", "SuccessfulAuth: from "
                                + s.getInetAddress().getHostAddress() + ":" +  s.getPort()));
                    }else {
                        out.writeObject(new Pair<String, Object>("AUTH_ERROR", message.getValue()));
                        out.flush();
                        s.close();
                        model.addLogItem(new LogItem("Error", "Auth error: from " + s.getInetAddress().getHostAddress()));
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void emailSender() {
        while (true) {
            ArrayList<Email> toRemove = new ArrayList<Email>();
            synchronized (model.emailList) {
                for (Email email : model.emailList) {
                    if (model.isValidUsername(email.getSender())) {
                        String error = "";
                        for (String account : email.getReceivers()) {
                            if (!model.isValidUsername(account)) {
                                if (error.compareTo("") == 0) {
                                    error += "Invalid accounts: ";
                                }
                                error += account;
                            }
                        }

                        if (error.compareTo("") == 0) {
                            sendEmail(email, toRemove);
                        }else {
                            model.addLogItem(new LogItem("Error", "Invalid email addresses from sender: " + email.getSender()));
                            String prevInfos = "subject: " + email.getSubject() +" receivers: " + email.getStringreceivers()
                                    + " date: " + email.getDate();
                            email.setReceivers(new String[]{email.getSender()});
                            email.setSender(Settings.localAccount);
                            email.setText("ERROR: " + error + "\n______________\nPrev. infos: \n" + prevInfos + "\n\nPrev. Text: \n" + email.getText());
                        }
                    }else {
                        toRemove.add(email);
                    }
                }
            }

            for(Email email: toRemove){
                model.removeEmailFromList(email);
            }

            try {
                Thread.sleep(Settings.sleepTime);
            } catch (Exception e) {
            }
        }
    }

    private void sendEmail(Email email, ArrayList<Email> toRemove){
        ArrayList<String> temp = new ArrayList<String>(Arrays.asList(email.getReceivers()));
        ArrayList<Socket> socketsUsed = new ArrayList<Socket>();
        Pair<String, Email> toSend;
        try{
            for(String account: email.getReceivers()){
                Socket tempConnection = model.getAccountSocket(account);
                if(tempConnection != null && !tempConnection.isClosed()){
                    boolean toggleNACK = false;
                    if(email.getSender().compareTo(Settings.localAccount) == 0) {
                        toSend = new Pair<String, Email>("NACK", email);
                        toggleNACK = true;
                    }else toSend = new Pair<String, Email>("EMAIL", email);
                    ObjectOutputStream out;
                    Socket thisSocket;
                    synchronized (model.emailMap){
                        thisSocket = model.emailMap.get(account).getKey();
                        out = model.emailMap.get(account).getValue().getValue();
                    }
                    if(!socketsUsed.contains(thisSocket)) {
                        out.writeObject(toSend);
                        out.flush();
                        if (toggleNACK) {
                            model.addLogItem(new LogItem("ActionInfo", "Sended NACK to:" + account));
                        } else model.addLogItem(new LogItem("ActionInfo", "Sended email to:" + account));
                        socketsUsed.add(thisSocket);
                    }
                    temp.remove(account);
                }
            }
            if(temp.size() == 0){
                toRemove.add(email);
            }else{
                System.arraycopy(temp.toArray(), 0, email.getReceivers(), 0, temp.size());
            }
        }catch(Exception e){e.printStackTrace();}
    }

    private void emailReceiver() {
        Map<Socket, Boolean> socketMap = new HashMap<Socket,  Boolean>();
        List<String> toRemove = new ArrayList<String>();
        while (true) {
            synchronized (model.emailMap) {
                for (String entry : model.emailMap.keySet()) {
                    Socket thisConnection = model.emailMap.get(entry).getKey();
                    boolean isAlreadyListened = false;

                    synchronized (socketMap) {
                        if (!socketMap.containsKey(thisConnection)) {
                            socketMap.put(thisConnection, Boolean.FALSE);
                        } else {
                            isAlreadyListened = socketMap.get(thisConnection);
                        }
                    }

                    if (!isAlreadyListened) {
                        try {
                            exec_pool.submit(() -> socketListener(entry, thisConnection, socketMap, toRemove));
                            synchronized (socketMap) { ;
                                socketMap.remove(thisConnection);
                                socketMap.put(thisConnection, Boolean.TRUE);
                            }
                        }catch (Exception e){}
                    }

                }

                synchronized (toRemove) {
                    ArrayList<String> accountsToDelete = new ArrayList<String>();

                    for (String account : toRemove) {
                        accountsToDelete.addAll(model.getAllSocketAccount(model.getAccountSocket(account)));

                    }

                    for(String account : accountsToDelete){
                        model.removeAccount(account);
                        model.addLogItem(new LogItem("ActionInfo", "Ended connection to:" + account));
                    }

                    toRemove.clear();
                }
            }

            try {
                Thread.sleep(500);
            } catch (Exception e) {}
        }
    }

    private void socketListener(String account, Socket thisConnection,  Map<Socket,Boolean> socketMap, List<String> toRemove){

        try{
            if (!thisConnection.isClosed()) {
                try {
                    ObjectInputStream in = model.emailMap.get(account).getValue().getKey();
                    Email email = (Email) in.readObject();
                    synchronized (model.emailList){model.emailList.add(email);}
                    if(email.getSender().compareTo(Settings.localAccount) != 0){
                        model.addLogItem(new LogItem("ActionInfo", "Received email from:" + account));
                    }
                } catch (Exception e) {
                    if (e instanceof EOFException  || e.getMessage().compareTo("Connection reset") == 0) {
                            thisConnection.close();
                            synchronized (toRemove){toRemove.add(account);}
                    }else e.printStackTrace();
                }
            }else if(thisConnection != null && thisConnection.isClosed()){
                    synchronized (toRemove){toRemove.add(account);}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            synchronized (socketMap){
                socketMap.remove(thisConnection);
                socketMap.put(thisConnection, Boolean.FALSE);
            }
        }
    }

}
