package me.fengyj.exception;

import org.slf4j.spi.LoggingEventBuilder;

public class InfrastructureException extends ApplicationBaseException {

    static final long serialVersionUID = -269234646;
    protected final ResourceInfo resourceInfo;

    public InfrastructureException(
        ErrorSeverity level,
        ResourceInfo resourceInfo,
        String message,
        Throwable causedBy) {

        super(level, message, causedBy);
        this.resourceInfo = resourceInfo;
    }

    public static InfrastructureException create(
        ErrorSeverity level,
        ResourceInfo resourceInfo,
        String message,
        Throwable causedBy) {

        return new InfrastructureException(level, resourceInfo, message, causedBy);
    }

    public ResourceInfo getResourceInfo() {

        return resourceInfo;
    }

    @Override
    protected LoggingEventBuilder appendLogData(LoggingEventBuilder builder) {

        return super.appendLogData(builder)
                    .addKeyValue("resource_type", this.getResourceInfo().getType())
                    .addKeyValue("resource_name", this.getResourceInfo().getName());
    }
}
