package me.fengyj.springdemo.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class XmlConverter {
    
    private static final XmlMapper objectMapper;

    static {
        objectMapper = new XmlMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new Jdk8Module());
    }

    public static XmlMapper getXmlMapper() {

        return objectMapper;
    }

    public static String toXml(Object obj) throws XmlConvertException {

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            throw new XmlConvertException("Failed to serialize to XML.", ex);
        }
    }

    public static byte[] toBytes(Object data) throws XmlConvertException {

        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException jpEx) {
            throw new XmlConvertException("Failed to serialize to JSON.", jpEx);
        }
    }

    public static void toXml(Object data, OutputStream stream) throws XmlConvertException {

        try {
            stream.write(objectMapper.writeValueAsBytes(data));
        } catch (IOException e) {
            throw new XmlConvertException("Failed to serialize to XML.", e);
        }
    }

    public static <T> List<T> fromBytesToList(byte[] bytes, Class<T> valueType) throws XmlConvertException {

        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return objectMapper.readValue(bytes, javaType);
        } catch (IOException ioEx) {
            throw new XmlConvertException("Failed to parse JSON.", ioEx);
        }
    }
}
