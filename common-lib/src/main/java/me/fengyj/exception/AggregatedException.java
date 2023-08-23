package me.fengyj.springdemo.exception;

import java.util.LinkedList;
import java.util.List;

public class AggregatedException extends ApplicationBaseException {

    static final long serialVersionUID = -681103267;
    public static final int MaxExceptionsToLog = 50;
    private List<Exception> exceptions = new LinkedList<>();
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
        if (this.exceptions.size() > MaxExceptionsToLog)
            this.exceptions = this.exceptions.subList(0, 20);

        return true;
    }
}
