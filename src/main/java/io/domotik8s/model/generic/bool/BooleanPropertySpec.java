package io.domotik8s.model.generic.bool;

import io.domotik8s.model.generic.PropertySpec;
import io.domotik8s.model.knx.KnxPropertyAddress;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BooleanPropertySpec extends PropertySpec<KnxPropertyAddress, BooleanPropertyState> {

    public BooleanPropertySpec() {

    }

}
