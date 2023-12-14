package io.domotik8s.model.generic.bool;

import io.domotik8s.model.generic.Property;
import io.domotik8s.model.generic.PropertySystemAddress;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BooleanProperty<A extends PropertySystemAddress> extends Property<A, BooleanPropertyState> {

    public BooleanProperty() {
        super("BooleanProperty");
    }

}
