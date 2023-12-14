package io.domotik8s.model.generic.bool;

import io.domotik8s.model.generic.PropertyStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class BooleanPropertyStatus extends PropertyStatus<BooleanPropertyState> {
}
