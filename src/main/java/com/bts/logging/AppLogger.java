package com.bts.logging;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.*;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * final class providing a surrogate for a standardized local logger using java.util.logging File and Console handler
 */
public final class AppLogger {

    /**
    * get standardized logger using java.util.logging File and Console handler
    * use .env LOG_FILEPATH, LOG_FILE_HANDLER_LEVEL and LOG_CONSOLE_HANDLER_LEVEL for configuring
    * @return      the standardized local logger
    */
    public static Logger getLogger() throws RuntimeException {
        String loggerName = AppLogger.class.getCanonicalName();

        if (LogManager.getLogManager().getLogger(loggerName) == null) {
            Dotenv dotenv = Dotenv.load();

            Logger logger = Logger.getLogger(loggerName);

            try {
                LogManager.getLogManager().readConfiguration(new FileInputStream("src/main/resources/bts.logging.properties"));
                FileHandler fileHandler = new FileHandler(dotenv.get("LOG_FILEPATH") + "/" + loggerName + ".log", 100000, 7, true);
                fileHandler.setLevel(Level.parse(dotenv.get("LOG_FILE_HANDLER_LEVEL")));
                logger.addHandler(fileHandler);
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(Level.parse(dotenv.get("LOG_CONSOLE_HANDLER_LEVEL")));
                logger.addHandler(consoleHandler);

                LogManager.getLogManager().addLogger(logger);
            }
            catch (IOException e){
                System.out.println( "Impossible to access log directory, please set the LOG_FILEPATH key in the project root .env file." );
                throw new RuntimeException(e);
            }
        }

        return LogManager.getLogManager().getLogger(loggerName);
    }
}