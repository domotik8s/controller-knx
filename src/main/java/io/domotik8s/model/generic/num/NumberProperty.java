package io.domotik8s.model.generic.num;

import io.domotik8s.model.generic.Property;
import io.domotik8s.model.generic.PropertySystemAddress;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class NumberProperty<A extends PropertySystemAddress> extends Property<A, NumberPropertyState> {

    public NumberProperty() {
        super("NumberProperty");
    }

}
