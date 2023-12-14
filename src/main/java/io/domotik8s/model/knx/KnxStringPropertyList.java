package io.domotik8s.model.knx;

import io.domotik8s.model.generic.PropertyList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
public class KnxStringPropertyList extends PropertyList<KnxStringProperty> {
}
