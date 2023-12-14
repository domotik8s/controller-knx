package io.domotik8s.model.generic.bool;

import io.domotik8s.model.generic.PropertyState;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BooleanPropertyState extends PropertyState<Boolean> {

    public BooleanPropertyState() {

    }

}
