package io.domotik8s.knxcontroller.knx.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.dptxlator.DPT;

@Component
public class DptToStringConverter implements Converter<DPT, String> {

    @Override
    public String convert(DPT source) {
        if (source == null)
            return null;
        return source.getID();
    }

}
