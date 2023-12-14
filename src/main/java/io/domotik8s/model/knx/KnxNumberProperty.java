package io.domotik8s.model.knx;

import io.domotik8s.model.generic.num.NumberProperty;
import io.domotik8s.model.generic.str.StringProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class KnxNumberProperty extends NumberProperty<KnxPropertyAddress> {
}
