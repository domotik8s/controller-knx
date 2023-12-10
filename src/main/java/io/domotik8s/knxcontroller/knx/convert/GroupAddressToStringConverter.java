package io.domotik8s.knxcontroller.knx.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.GroupAddress;

@Component
public class GroupAddressToStringConverter implements Converter<GroupAddress, String> {

    @Override
    public String convert(GroupAddress source) {
        if (source == null)
            return null;

        StringBuffer buf = new StringBuffer();
        buf.append(source.getMainGroup());
        buf.append("/");
        buf.append(source.getMiddleGroup());
        buf.append("/");
        buf.append(source.getSubGroup8());

        return buf.toString();
    }

}