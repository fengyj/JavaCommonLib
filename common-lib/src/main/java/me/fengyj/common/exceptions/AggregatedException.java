package me.fengyj.common.exceptions;

import java.util.LinkedList;
import java.util.List;

/**
 * Used for combining multiple exceptions to a single one.
 */
public class AggregatedException extends ApplicationBaseException {

    static final long serialVersionUID = -681103267;
    /**
     * In case too many exceptions are added to the {@link AggregatedException#exceptions}}.
     */
    public static final int MaxExceptionsToLog = 50;
    private final List<Exception> exceptions = new LinkedList<>();
    private int totalExceptions = 0;

    public AggregatedException(ErrorSeverity level, String msg) {

        super(level, msg, null);
        exceptions.forEach(e -> {
            if (e instanceof AggregatedException) {
                this.exceptions.addAll(((AggregatedException) e).getExceptions());
            } else {
                this.exceptions.add(e);
            }
        });
    }

    public List<Exception> getExceptions() {

        return this.exceptions;
    }

    public int getTotalExceptions() {

        return this.totalExceptions;
    }

    public boolean hasMoreExceptions() {

        return this.totalExceptions > MaxExceptionsToLog;
    }

    public boolean addException(Exception ex) {

        if (ex instanceof AggregatedException) {
            this.totalExceptions += ((AggregatedException) ex).getExceptions().size();
        } else {
            this.totalExceptions += 1;
        }

        if (totalExceptions >= MaxExceptionsToLog) return false;

        if (ex instanceof AggregatedException) {
            this.exceptions.addAll(((AggregatedException) ex).getExceptions());
        } else {
            this.exceptions.add(ex);
        }

        return true;
    }
}
