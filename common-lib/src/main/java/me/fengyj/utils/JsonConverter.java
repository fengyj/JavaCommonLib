package me.fengyj.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JsonConverter {
    
    private static final ObjectMapper objectMapper;
    private static final ObjectMapper anotherMapper;

    static {
        
        Jdk8Module jdk8Module = new Jdk8Module();
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        objectMapper = JsonMapper.builder()
                                 .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                                 .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                 .disable(SerializationFeature.CLOSE_CLOSEABLE)
                                 .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                 .addModules(jdk8Module, javaTimeModule)
                                 .build();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        anotherMapper = JsonMapper.builder()
                                  .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                                  .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                  .disable(SerializationFeature.CLOSE_CLOSEABLE)
                                  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                  .addModules(jdk8Module, javaTimeModule)
                                  .build();
        anotherMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }
    
    public static <T> T fromJson(String json, Class<T> valueType) throws JsonConvertException {

        try {
            return objectMapper.readValue(json, valueType);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) throws JsonConvertException {

        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T, K> T fromJson(String json, Class<T> genericType, Class<K> paramType) throws JsonConvertException {

        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(genericType, paramType);
            return objectMapper.readValue(json, javaType);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T, K> T fromJson(InputStream stream, Class<T> genericType, Class<K> paramType) throws
        JsonConvertException {

        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(genericType, paramType);
            return objectMapper.readValue(stream, javaType);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T> T fromJson(byte[] json, Class<T> valueType) throws JsonConvertException {

        try {
            return objectMapper.readValue(json, valueType);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T, K> T fromJson(byte[] json, Class<T> genericType, Class<K> paramType) throws JsonConvertException {

        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(genericType, paramType);
            return objectMapper.readValue(json, javaType);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T> T fromJson(InputStream stream, Class<T> valueType) throws JsonConvertException {

        try {
            return objectMapper.readValue(stream, valueType);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T> List<T> fromBytesToList(byte[] bytes, Class<T> valueType) throws JsonConvertException {

        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return objectMapper.readValue(bytes, javaType);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T> List<T> fromBytesToList(InputStream stream, Class<T> valueType) throws JsonConvertException {

        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return objectMapper.readValue(stream, javaType);
        } catch (IOException ioEx) {
            throw new JsonConvertException("Failed to parse JSON.", ioEx);
        }
    }

    public static <T> String toJson(T data) throws JsonConvertException {

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException jpEx) {
            throw new JsonConvertException("Failed to serialize to JSON.", jpEx);
        }
    }

    public static <T> String toJson(T data, boolean includeNullFields) throws JsonConvertException {

        try {
            return includeNullFields ? anotherMapper.writeValueAsString(data) : objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException jpEx) {
            throw new JsonConvertException("Failed to serialize to JSON.", jpEx);
        }
    }

    public static byte[] toBytes(Object data) throws JsonConvertException {

        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException jpEx) {
            throw new JsonConvertException("Failed to serialize to JSON.", jpEx);
        }
    }

    public static byte[] toBytes(Object data, boolean includeNullFields) throws JsonConvertException {

        try {
            return includeNullFields ? anotherMapper.writeValueAsBytes(data) : objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException jpEx) {
            throw new JsonConvertException("Failed to serialize to JSON.", jpEx);
        }
    }

    public static JsonNode toJsonNode(String json) throws JsonConvertException {

        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            throw new JsonConvertException("Failed to convert to JSON Node.", e);
        }
    }

    public static <T> T toValue(JsonNode json, Class<T> clazz) throws JsonConvertException {

        try {
            return objectMapper.treeToValue(json, clazz);
        } catch (JsonProcessingException jpEx) {
            throw new JsonConvertException("Failed to serialize to JSON.", jpEx);
        }
    }

    public static void toJson(Object data, OutputStream stream) throws JsonConvertException {

        try {
            objectMapper.writeValue(stream, data);
        } catch (IOException e) {
            throw new JsonConvertException("Failed to serialize to JSON.", e);
        }
    }

    public static ObjectMapper getDefaultMapper() {

        return objectMapper;
    }

    public static ObjectMapper getAlternativeMapper() {

        return anotherMapper;
    }
}
