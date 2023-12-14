package io.domotik8s.model.knx;

import io.domotik8s.model.generic.PropertyList;
import io.domotik8s.model.generic.bool.BooleanProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
public class KnxBooleanPropertyList extends PropertyList<KnxBooleanProperty> {
}
