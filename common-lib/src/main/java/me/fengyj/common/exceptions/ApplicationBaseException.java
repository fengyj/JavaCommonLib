package me.fengyj.common.exceptions;

import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

import java.io.Serial;

public class ApplicationBaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -340139605L;
    protected final ErrorSeverity level;

    public ApplicationBaseException(ErrorSeverity level, String message, Throwable causedBy) {

        super(message, getRealException(causedBy));
        this.level = level;
    }

    private static Throwable getRealException(Throwable causedBy) {
        if (causedBy instanceof RetrievableException && causedBy.getCause() != null)
            return causedBy.getCause();

        return causedBy;
    }

    public ErrorSeverity getLevel() {

        return level;
    }
    
    public ApplicationBaseException log(Logger logger) {

        return ExceptionUtils.log(logger, this);
    }

    public ApplicationBaseException log(Logger logger, String msg) {

        return ExceptionUtils.log(logger, this, msg);
    }

    protected LoggingEventBuilder appendLogData(LoggingEventBuilder builder) {

        return ExceptionUtils.appendLogData(builder, this);
    }
}
