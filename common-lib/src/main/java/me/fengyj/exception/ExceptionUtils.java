package me.fengyj.exception;

import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

public class ExceptionUtils {

    public static LoggingEventBuilder appendLogData(LoggingEventBuilder builder, Throwable throwable) {

        return builder.addKeyValue("error_type", throwable.getClass().getSimpleName())
                .addKeyValue("error_severity", getErrorSeverity(throwable).name());
    }
    
    public static <T extends Throwable> T log(Logger logger, T throwable) {

        return log(logger, null);
    }

    public static <T extends Throwable> T  log(Logger logger, T throwable, String msg) {

        switch (getErrorSeverity(throwable)) {
            case Critical:
            case Error:
                appendLogData(logger.atError(), throwable).log(msg == null ? throwable.getMessage() : msg, throwable);
                return throwable;
            case Warning:
                appendLogData(logger.atWarn(), throwable).log(msg == null ? throwable.getMessage() : msg, throwable);
                return throwable;
            default:
                appendLogData(logger.atInfo(), throwable).log(msg == null ? throwable.getMessage() : msg, throwable);
                return throwable;
        }
    }

    private static ErrorSeverity getErrorSeverity(Throwable throwable) {

        return throwable instanceof ApplicationBaseException
                        ? ((ApplicationBaseException) throwable).getLevel()
                        : ErrorSeverity.Error;
    }
}
