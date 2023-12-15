package io.domotik8s.model.generic.num;

import io.domotik8s.model.generic.PropertyState;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class NumberPropertyState extends PropertyState<Integer> {

    public NumberPropertyState() {

    }

}
