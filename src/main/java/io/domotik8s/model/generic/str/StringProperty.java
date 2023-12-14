package io.domotik8s.model.generic.str;

import io.domotik8s.model.generic.Property;
import io.domotik8s.model.generic.PropertySystemAddress;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class StringProperty<A extends PropertySystemAddress> extends Property<A, StringPropertyState> {

    public StringProperty() {
        super("StringProperty");
    }

}
