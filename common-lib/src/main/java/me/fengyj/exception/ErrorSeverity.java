package me.fengyj.springdemo.exception;

public enum  ErrorSeverity {
    /**
     * Needs to inform TOC immediately.
     */
    Critical,
    /**
     * Needs to tackle the issue when doing daily operation work.
     */
    Error,
    /**
     * Could need to handle the issue. like a data issue but not very important.
     */
    Warning,
    /**
     * Just a information, no needs to follow up.
     */
    Info
}