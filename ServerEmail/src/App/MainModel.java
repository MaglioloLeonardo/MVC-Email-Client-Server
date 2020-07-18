package App;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.util.Pair;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class MainModel {
    public Map<String, Pair<Socket, Pair<ObjectInputStream, ObjectOutputStream>>> emailMap;
    public ArrayList<Email> emailList = new ArrayList<Email>();
    private ObservableList<LogItem> logList;
    private ObservableList<String> accountList;
    private TableView<LogItem> logTableView;


    public MainModel(){
        emailMap = new HashMap<String, Pair<Socket, Pair<ObjectInputStream, ObjectOutputStream>>>();
        emailList = new ArrayList<Email>();
        logList = FXCollections.observableArrayList();
        accountList = FXCollections.observableArrayList();
        startAutoSave();
    }

    public void addAccount(String emailAddr, Socket connection, ObjectInputStream in, ObjectOutputStream out){
        if(!isAlreadyPresent(emailAddr)){
            synchronized (emailMap) {
                emailMap.put(emailAddr, new Pair<Socket, Pair<ObjectInputStream, ObjectOutputStream>>(connection, new Pair<>(in, out)));
            }
            synchronized (accountList){
                Platform.runLater(() -> accountList.add(emailAddr));
            }

        }
    }

    public void removeAccount(String emailAddr){
        List<String> toRemove = new ArrayList<String>();
        if(isAlreadyPresent(emailAddr)){
            synchronized (emailMap){
                for(String entry: emailMap.keySet()){
                    if(entry.compareTo(emailAddr) == 0)toRemove.add(entry);//emailMap.remove(entry);
                }

                for(String account:toRemove){
                    emailMap.remove(account);
                }

                toRemove.clear();
            }

            synchronized (accountList) {
                Platform.runLater(() -> {
                    for(String account: accountList){
                        if(account.compareTo(emailAddr) == 0){
                            toRemove.add(account);
                        }
                    }

                    for(String account:toRemove){
                        accountList.remove(account);
                    }
                });
            }
        }
    }

    public Socket getAccountSocket(String emailAddr){
        Socket s = null;
        synchronized(emailMap){
            Pair<Socket, Pair<ObjectInputStream, ObjectOutputStream>> temp = emailMap.get(emailAddr);
            if(temp == null)return null;
            s = temp.getKey();
        }
        return s;
    }

    public List<String> getAllSocketAccount(Socket connection){
        List<String> toReturn = new ArrayList<String>();
        synchronized (emailMap){
            for(String entry: emailMap.keySet()){
                if(emailMap.get(entry).getKey().equals(connection)){
                    toReturn.add(entry);
                }
            }
        }
        return toReturn;
    }

    public void addEmailOnList(Email email){
        synchronized (emailList){
            emailList.add(email);
        }

    }

    public void removeEmailFromList(Email email){
        synchronized (emailList){
            emailList.remove(email);
        }
    }

    public boolean isAlreadyPresent(String emailAddr){
        boolean toReturn;
        synchronized (emailMap) {toReturn = emailMap.containsKey(emailAddr);}
        return  toReturn;
    }

    public boolean isValidUsername(String account){
        for(String validAccount: Settings.validAccounts){
            if(validAccount.compareTo(account) == 0 || account.compareTo(Settings.localAccount) == 0){
                return true;
            }
        }
        return false;
    }

    public void addLogItem(LogItem logItem){
        synchronized (logList){logList.add(logItem);}
        if(logTableView != null){
                try{
                    Platform.runLater(() -> {try {logTableView.sort();}catch (Exception e){}});
                }catch(Exception e){};
        }
    }

    public void deleteLogItem(LogItem logItem){
        Platform.runLater(() ->  {
            synchronized (logList) {
                logList.remove(logItem);
            }
        });
    }

    public void deleteAllLogItem(){
        Platform.runLater(() ->  {
            synchronized (logList) {
                logList.clear();
            }
        });
    }

    public void setLogOnTable(TableView<LogItem> logTableView){
        //The user interface cannot be directly updated from a non-application thread
        if(logTableView != null){
            this.logTableView = logTableView;
            Platform.runLater(() ->  {
                synchronized (logList) {
                    logTableView.setItems(logList);
                }
            });
        }
    }

    public void setAccountOnList(ListView<String> accountListView){
        //The user interface cannot be directly updated from a non-application thread
        if(logTableView != null){
            this.logTableView = logTableView;
            Platform.runLater(() ->  {
                synchronized (logList) {
                    accountListView.setItems(accountList);
                }
            });
        }
    }

    public void saveMessages(){
        Thread thread = new Thread(() -> {
            ObjectOutputStream out;
            try{

                synchronized (emailList) {
                    File file = new File(Settings.emailSavePath);
                    out = new ObjectOutputStream(new FileOutputStream(file));
                    out.writeObject(emailList);
                    out.close();
                }
                addLogItem(new LogItem("ActionInfo", "Saved emails"));

                addLogItem(new LogItem("ActionInfo", "Saved logs"));
                ArrayList<LogItem> tempLogList = new ArrayList<LogItem>(logList);
                synchronized (logList){
                    File file = new File(Settings.logSavePath);
                    out = new ObjectOutputStream(new FileOutputStream(file));
                    out.writeObject(tempLogList);
                    out.close();
                }

            }catch (Exception e){
                addLogItem(new LogItem("Error", "Impossible to save:" + e.getMessage()));
            }
        });
        thread.setName(Settings.saveThreadName);
        thread.start();
    }

    public void resumeMessages(){
        Thread thread = new Thread(() -> {
            try{

                synchronized (logList){
                    File file = new File(Settings.logSavePath);
                    ObjectInputStream out = new ObjectInputStream(new FileInputStream(file));
                    logList.addAll((ArrayList<LogItem>) out.readObject());
                    out.close();
                }
                addLogItem(new LogItem("ActionInfo", "Resumed logs"));

                synchronized (emailList) {
                    File file = new File(Settings.emailSavePath);
                    ObjectInputStream out = new ObjectInputStream(new FileInputStream(file));
                    emailList = (ArrayList<Email>) out.readObject();
                    out.close();
                }
                addLogItem(new LogItem("ActionInfo", "Resumed emails"));

            }catch (Exception e){
                addLogItem(new LogItem("Error", "Impossible to resume:" + e.getMessage()));
            }
        });
        thread.setName(Settings.resumeThreadName);
        thread.start();
    }

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

}
