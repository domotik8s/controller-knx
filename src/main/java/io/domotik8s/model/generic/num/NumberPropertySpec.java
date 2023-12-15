package io.domotik8s.model.generic.num;

import io.domotik8s.model.generic.PropertySpec;
import io.domotik8s.model.knx.KnxPropertyAddress;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class NumberPropertySpec extends PropertySpec<KnxPropertyAddress, NumberPropertyState> {

    public NumberPropertySpec() {

    }

}
