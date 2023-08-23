package me.fengyj.utils;

public class JsonConvertException extends RuntimeException {
    public JsonConvertException(String msg, Throwable innerEx) {
        super(msg, innerEx);
    }
}
