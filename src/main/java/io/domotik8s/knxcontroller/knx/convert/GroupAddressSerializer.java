package io.domotik8s.knxcontroller.knx.convert;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import tuwien.auto.calimero.GroupAddress;

@JsonComponent
public class GroupAddressSerializer extends JsonSerializer<GroupAddress> {

    private Converter<GroupAddress, String> converter = new GroupAddressToStringConverter();

    @Override
    public void serialize(GroupAddress value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(converter.convert(value));
    }

}
