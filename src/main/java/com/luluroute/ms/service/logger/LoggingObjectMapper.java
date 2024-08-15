package com.luluroute.ms.service.logger;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LoggingObjectMapper extends ObjectMapper {

    private static final long serialVersionUID = -3226026435751010999L;

    public LoggingObjectMapper() {
        super();
        LogAnnotationIntrospector maskAnnotationIntrospector = new LogAnnotationIntrospector();
        AnnotationIntrospector pair = AnnotationIntrospector.pair(this.getSerializationConfig().getAnnotationIntrospector(), maskAnnotationIntrospector);
        this.setAnnotationIntrospector(pair);
    }

    public String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return writeValueAsString(value);
        } catch (Exception e) {
            return "Object couldn't be converted to JSON: " + e.getMessage();
        }
    }
}