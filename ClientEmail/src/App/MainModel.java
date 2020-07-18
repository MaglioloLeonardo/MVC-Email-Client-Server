package App;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainModel{
    private ExecutorService exec_pool = Executors.newFixedThreadPool(Settings.mainPollStartSize);
    public Map<String, List<Pair<String,ObservableList<Email>>>> listTable;

    public SimpleStringProperty IpAddr = new SimpleStringProperty();
    public SimpleIntegerProperty port = new SimpleIntegerProperty();
    public SimpleStringProperty state = new SimpleStringProperty();

    public MainModel(){
        state.set("Disconnected");
        listTable = new HashMap<String, List<Pair<String,ObservableList<Email>>>>();
        for (String account: Settings.localAccounts){
            List<Pair<String,ObservableList<Email>>> temp = new ArrayList<Pair<String,ObservableList<Email>>>();
            for(String field: Settings.treeFields){
                temp.add(new Pair(field ,FXCollections.observableArrayList()));
            }
            listTable.put(account, temp);
        }
        startAutoSave();
    }

    public void addMessage(String account, String field, Email message){
        ObservableList<Email> messageList = getList(account, field);
        if(messageList != null)addMessage(message, messageList);
    }

    private void addMessage(Email message, ObservableList<Email> messageList){
       exec_pool.submit(() -> {
            synchronized (messageList) {
               if(!messageList.contains(message))messageList.add(message);
            }
        });
    }

    public void removeThisMessage(String account, String field, Email message){
        ObservableList<Email> messageList = getList(account, field);
        if(messageList != null)removeThisMessage(message, messageList);
    }

    private void removeThisMessage(Email message, ObservableList<Email> messageList){
        exec_pool.submit(() -> {
            synchronized (messageList) { messageList.remove(message);}
        });
    }


    public void removeAllMessages(String account, String field){
        ObservableList<Email> messageList = getList(account, field);
        if(messageList != null)removeAllMessages(messageList);
    }

    private void removeAllMessages(ObservableList<Email> messageList){
        exec_pool.submit(() -> {
            synchronized (messageList) { messageList.clear(); }
        });
    }

    public void setEmailsOnTable(String account, String field, TableView<Email> messageView){
        ObservableList<Email> messageList = getList(account, field);
        if(messageList != null)setEmailsOnTable(messageView, messageList);

    }

    private void setEmailsOnTable(TableView<Email> messageView, ObservableList<Email> messageList ){
        if(messageView != null && messageList != null){
            //The user interface cannot be directly updated from a non-application thread
            Platform.runLater(() ->  {
                synchronized (messageList) {
                    messageView.setItems(messageList);
                }
            });
        }
    }

    public void saveMessages(){
        Thread thread = new Thread(() -> {
            ObjectOutputStream out;
            try{
                Map<String, List<Pair<String,List<Email>>>> serializableTable = new HashMap<String, List<Pair<String,List<Email>>>>();
                synchronized (listTable) {
                    for (String entry: listTable.keySet()) {
                        List<Pair<String,List<Email>>> pairList = new ArrayList<Pair<String,List<Email>>>();
                        for (Pair<String, ObservableList<Email>> pair:listTable.get(entry)) {
                            synchronized (listTable.get(entry)){
                                List<Email> temp  = new ArrayList<Email>();
                                temp.addAll(pair.getValue()) ;
                                pairList.add(new Pair<String,List<Email>>(pair.getKey(), temp));
                            }
                        }
                        serializableTable.put(entry, pairList);
                    }
                    File file = new File(Settings.savePath);
                    out = new ObjectOutputStream(new FileOutputStream(file));
                    out.writeObject(serializableTable);
                    out.close();
                }
            }catch (Exception e){e.printStackTrace();}
        });
        thread.setName(Settings.saveThreadName);
        thread.start();
    }

    public void resumeMessages(){
        Thread thread = new Thread(() -> {
            try{
                synchronized (listTable) {
                    File file = new File(Settings.savePath);
                    ObjectInputStream out = new ObjectInputStream(new FileInputStream(file));
                    Map<String, List<Pair<String,List<Email>>>> serializableTable = (Map<String, List<Pair<String,List<Email>>>>) out.readObject();
                    out.close();
                    for (String entry: serializableTable.keySet()) {
                        List<Pair<String,ObservableList<Email>>> pairList = new ArrayList<Pair<String,ObservableList<Email>>>();
                        for (Pair<String, List<Email>> pair: serializableTable.get(entry)) {
                            synchronized (listTable.get(entry)){
                                for(Pair<String,ObservableList<Email>> pairObservable:listTable.get(entry)){
                                    if(pairObservable.getKey().compareTo(pair.getKey()) == 0)pairObservable.getValue().addAll(pair.getValue());
                                }
                            }
                        }
                    }
                }
            }catch (FileNotFoundException e){

            }catch (Exception e){
                e.printStackTrace();
            }
        });
        thread.setName(Settings.resumeThreadName);
        thread.start();
    }

    public void poolShutdown(){
        exec_pool.shutdown();
    }

    public void loadSampleMessages(){
        for(int i = 0; i < 100; i++) {
            String[] receivers = {"magliololeonardo@libero.it"};
            addMessage(Settings.localAccounts[0], Settings.treeFields[0], new Email(String.valueOf(i), "magliololeonardo@hotmail.it", receivers, "io non\nci sono riuscito"));
        }
    }

    //------------------
    private void startAutoSave(){
        TimerTask task = new TimerTask(){
            boolean start = false;
            public void run() {
                if(start){
                    saveMessages();
                }else{
                    try{
                        Thread.sleep(20000);
                    }catch(Exception e){};
                }
                start = true;
            }
        };

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(task, 0, Settings.autoSavetime);
    }

    private ObservableList<Email> getList(String account, String field){
        assert contains(Settings.localAccounts, account) && contains(Settings.treeFields, field);
        List<Pair<String,ObservableList<Email>>> tempList = listTable.get(account);
        for (Pair<String, ObservableList<Email>> pair:tempList) {
            if((pair.getKey()).compareTo(field) == 0){
                return pair.getValue();
            }
        }
        return null;
    }

    private boolean contains(String[] array, String str){
        for(String temp:array){
            if(str.compareTo(temp) == 0)return true;
        }
        return false;
    }
}
