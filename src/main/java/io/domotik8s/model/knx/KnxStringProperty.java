package io.domotik8s.model.knx;

import io.domotik8s.model.generic.str.StringProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class KnxStringProperty extends StringProperty<KnxPropertyAddress> {
}
