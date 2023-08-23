package me.fengyj.springdemo.utils;

public class XmlConvertException extends RuntimeException {
    public XmlConvertException(String msg, Throwable innerEx) {
        super(msg, innerEx);
    }
}
