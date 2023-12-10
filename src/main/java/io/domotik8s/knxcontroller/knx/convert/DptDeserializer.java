package io.domotik8s.knxcontroller.knx.convert;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import tuwien.auto.calimero.dptxlator.DPT;

@JsonComponent
public class DptDeserializer extends JsonDeserializer<DPT> {

    private Converter<String, DPT> converter = new StringToDptConverter();

    @Override
    public DPT deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String str = p.getValueAsString();
        if (str == null | str.trim().length() == 0)
            return null;
        return converter.convert(str);
    }

}