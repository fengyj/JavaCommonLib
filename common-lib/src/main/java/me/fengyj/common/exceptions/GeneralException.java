package me.fengyj.common.exceptions;

public class GeneralException extends ApplicationBaseException {

    static final long serialVersionUID = -709034897;

    public GeneralException(
        ErrorSeverity level,
        String message,
        Throwable causedBy) {

        super(level, message, causedBy);
    }

    public static GeneralException create(
        ErrorSeverity level,
        String message,
        Throwable causedBy) {

        return new GeneralException(level, message, causedBy);
    }
}
