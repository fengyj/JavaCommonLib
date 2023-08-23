package me.fengyj.exception;

import org.slf4j.spi.LoggingEventBuilder;

/**
 * To describe the case that the dependencies are not available temporarily.
 * And they could be available after a short time.
 * Throw out this exception when calculation, and it will be caught at atlas-lib-calcsrv layer.
 * Then event state will be set to Need2Retry, and finish calculating.
 *
 * Created by efeng on 2017-03-29.
 */
public class RetrievableException extends ApplicationBaseException {

    static final long serialVersionUID = -699003279;
    protected final ResourceInfo resourceInfo;
    private int triedTimes = 1;

    public RetrievableException(
        ErrorSeverity level,
        ResourceInfo resourceInfo,
        String message)
    {
        super(level, message, null);

        this.resourceInfo = resourceInfo;
    }

    public RetrievableException(
        ErrorSeverity level,
        ResourceInfo resourceInfo,
        String message,
        Throwable ex)
    {
        super(level, message, ex);

        this.resourceInfo = resourceInfo;
    }

    public ResourceInfo getResourceInfo() {

        return resourceInfo;
    }

    public int getTriedTimes() {
        return this.triedTimes;
    }

    public void setTriedTimes(int times) {
        this.triedTimes = times;
    }

    @Override
    protected LoggingEventBuilder appendLogData(LoggingEventBuilder builder) {

        return super.appendLogData(builder)
                    .addKeyValue("tried_times", this.triedTimes)
                    .addKeyValue("resource_type", this.getResourceInfo().getType())
                    .addKeyValue("resource_name", this.getResourceInfo().getName());
    }
}
