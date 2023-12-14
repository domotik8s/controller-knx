package io.domotik8s.model.generic.str;

import io.domotik8s.model.generic.PropertySpec;
import io.domotik8s.model.knx.KnxPropertyAddress;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class StringPropertySpec extends PropertySpec<KnxPropertyAddress, StringPropertyState> {
}
