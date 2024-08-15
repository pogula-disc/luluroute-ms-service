package com.luluroute.ms.service.logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class MaskingSerializer extends StdSerializer<String> {

    private static final long serialVersionUID = 3593714068922812123L;

    public MaskingSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeString("*******");
    }
}