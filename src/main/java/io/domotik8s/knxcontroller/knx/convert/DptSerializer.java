package io.domotik8s.knxcontroller.knx.convert;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import tuwien.auto.calimero.dptxlator.DPT;

@JsonComponent
public class DptSerializer extends JsonSerializer<DPT> {

    private Converter<DPT, String> converter = new DptToStringConverter();

    @Override
    public void serialize(DPT value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String str = converter.convert(value);
        if (str == null || str.trim().length() == 0)
            gen.writeNull();
        else
            gen.writeString(converter.convert(value));
    }

    @Override
    public Class<DPT> handledType() {
        return DPT.class;
    }

}