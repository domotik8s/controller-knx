package io.domotik8s.model.generic.num;

import io.domotik8s.model.generic.PropertyStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class NumberPropertyStatus extends PropertyStatus<NumberPropertyState> {
}
