package App;

public class Settings {
    //IO-Saves
    static final String emailSavePath = "Saves/savedEmails.bin";
    static final String logSavePath = "Saves/savedLogs.bin";
    static final String saveThreadName = "saveTread";
    static final String resumeThreadName = "resumeTread";
    static final int autoSavetime = 3 * 60 * 1000; //in milliseconds
    static final int sleepTime = 500;

    //Thread-poll config
    static final int mainPollStartSize = 10;

    //Default accounts
    static final String localAccount = "thisServerAccount";
    static final String[] validAccounts = {"magliololeonardo@hotmail.it", "magliololeonardo@libero.it", "prova@prova.it",
    "magliolo@hotmail.it", "magliolo@libero.it", "prova2@prova.it"};

    //Socket settings
    static final int defaultPort = 1337;
}
