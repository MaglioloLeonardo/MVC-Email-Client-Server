package App;

public class Settings {
    //IO-Saves
    static final String savePath = "savedMessages.bin";
    static final String saveThreadName = "saveTread";
    static final String resumeThreadName = "resumeTread";
    static final int autoSavetime = 3 * 60 * 1000; //in milliseconds
    static final int autoConnectTime = 5 * 1000;

    //Thread-poll config
    static final int mainPollStartSize = 10;

    //Default accounts
    static final String[] localAccounts = {"magliololeonardo@hotmail.it", "magliololeonardo@libero.it", "prova@prova.it"};

    //MainComponents Settings
    static final String[] treeFields = {"Inbox", "Sent", "Pending", "Rejected"};

    //Socket settings
    static final String defaultIP = "127.0.0.1";
    static final int defaultPort = 1337;
}
