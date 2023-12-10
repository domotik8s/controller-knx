package io.domotik8s.model.num;

import io.domotik8s.model.PropertySpec;
import io.domotik8s.model.PropertyAccess;
import io.domotik8s.model.PropertyAddress;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
public class NumberPropertySpec<A extends PropertyAddress> implements PropertySpec<PropertyAddress, NumberPropertyState> {

    private Set<PropertyAccess> access;

    private A address;

    private NumberPropertyState state;

    private Boolean locked;

}
