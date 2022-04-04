package com.olexyn.ensync;


import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;



public class LogUtil {

    private static final String format = "[%1$tF %1$tT] [%4$-7s] %5$-100s [%2$s]\n";

    public static Logger get(Class<?> c) {
        return get(c, Level.INFO);
    }

    public static Logger get(Class<?> c, Level level) {
        System.setProperty("java.util.logging.SimpleFormatter.format", format);
        Logger logger = Logger.getLogger(c.getName());
        try {
            String dir = System.getProperty("user.dir") + "/logs/main.log";
            FileHandler fh = new FileHandler(dir, true);
            fh.setFormatter(new SimpleFormatter() {
                @Override
                public synchronized String format(LogRecord logRecord) {
                    String msg = logRecord.getMessage();
                    return String.format(format,
                        new Date(logRecord.getMillis()),
                        logRecord.getSourceClassName() + " " + logRecord.getSourceMethodName(),
                        "",
                        logRecord.getLevel().getLocalizedName(),
                        msg
                    );
                }
            });

            logger.addHandler(fh);
            logger.setLevel(level);
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
        return logger;
    }

}
