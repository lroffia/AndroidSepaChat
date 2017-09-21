package it.unibo.arces.wot.sepa.android.util;

/**
 * Created by luca on 21/06/17.
 */
public class LogManager {

    public static Logger getLogger(String name) {
        return new Logger(name);
    }
}
