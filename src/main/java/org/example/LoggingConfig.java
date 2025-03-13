package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Enhanced logging configuration to capture detailed system information
 */
public class LoggingConfig {
    // Log levels
    public static final int LEVEL_ERROR = 1;
    public static final int LEVEL_WARN = 2;
    public static final int LEVEL_INFO = 3;
    public static final int LEVEL_DEBUG = 4;
    public static final int LEVEL_TRACE = 5;
    
    // Current log level
    private static int currentLogLevel = LEVEL_INFO;
    
    // Log file name
    private static String logFile = "factory_simulation.log";
    private static boolean logToFile = true;
    
    /**
     * Set the current log level
     * @param level The log level to set
     */
    public static void setLogLevel(int level) {
        currentLogLevel = level;
        log(LEVEL_INFO, "System", "Log level set to " + getLevelName(level));
    }
    
    /**
     * Get the name of a log level
     * @param level The log level
     * @return The name of the log level
     */
    private static String getLevelName(int level) {
        switch (level) {
            case LEVEL_ERROR: return "ERROR";
            case LEVEL_WARN: return "WARN";
            case LEVEL_INFO: return "INFO";
            case LEVEL_DEBUG: return "DEBUG";
            case LEVEL_TRACE: return "TRACE";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Log a message
     * @param level The log level
     * @param component The component name
     * @param message The message to log
     */
    public static void log(int level, String component, String message) {
        if (level <= currentLogLevel) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            String formattedMessage = String.format("[%s] [%s] [%s] %s", 
                timestamp, getLevelName(level), component, message);
                
            // Log to console
            System.out.println(formattedMessage);
            
            // Log to file if enabled
            if (logToFile) {
                try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
                    out.println(formattedMessage);
                } catch (IOException e) {
                    System.err.println("Error writing to log file: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Log an error message
     * @param component The component name
     * @param message The message to log
     */
    public static void error(String component, String message) {
        log(LEVEL_ERROR, component, message);
    }
    
    /**
     * Log an error message with exception
     * @param component The component name
     * @param message The message to log
     * @param e The exception to log
     */
    public static void error(String component, String message, Exception e) {
        log(LEVEL_ERROR, component, message + ": " + e.getMessage());
        if (currentLogLevel >= LEVEL_DEBUG) {
            e.printStackTrace();
        }
    }
    
    /**
     * Log a warning message
     * @param component The component name
     * @param message The message to log
     */
    public static void warn(String component, String message) {
        log(LEVEL_WARN, component, message);
    }
    
    /**
     * Log an info message
     * @param component The component name
     * @param message The message to log
     */
    public static void info(String component, String message) {
        log(LEVEL_INFO, component, message);
    }
    
    /**
     * Log a debug message
     * @param component The component name
     * @param message The message to log
     */
    public static void debug(String component, String message) {
        log(LEVEL_DEBUG, component, message);
    }
    
    /**
     * Log a trace message
     * @param component The component name
     * @param message The message to log
     */
    public static void trace(String component, String message) {
        log(LEVEL_TRACE, component, message);
    }
    
    /**
     * Enable or disable file logging
     * @param enable True to enable file logging, false to disable
     */
    public static void setFileLogging(boolean enable) {
        logToFile = enable;
        info("System", "File logging " + (enable ? "enabled" : "disabled"));
    }
    
    /**
     * Set the log file name
     * @param fileName The log file name
     */
    public static void setLogFile(String fileName) {
        logFile = fileName;
        info("System", "Log file set to " + fileName);
    }
}