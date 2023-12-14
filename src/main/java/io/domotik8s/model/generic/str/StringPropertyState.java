package io.domotik8s.model.generic.str;

import io.domotik8s.model.generic.PropertyState;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class StringPropertyState extends PropertyState<String> {
}
