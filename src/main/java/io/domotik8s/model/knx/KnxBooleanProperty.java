package io.domotik8s.model.knx;

import io.domotik8s.model.generic.bool.BooleanProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class KnxBooleanProperty extends BooleanProperty<KnxPropertyAddress> {
}
