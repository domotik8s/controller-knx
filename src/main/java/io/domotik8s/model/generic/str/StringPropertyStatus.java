package io.domotik8s.model.generic.str;

import io.domotik8s.model.generic.PropertyStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class StringPropertyStatus extends PropertyStatus<StringPropertyState> {
}
