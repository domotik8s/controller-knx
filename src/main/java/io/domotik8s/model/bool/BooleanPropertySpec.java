package io.domotik8s.model.bool;

import io.domotik8s.model.PropertyAccess;
import io.domotik8s.model.PropertyAddress;
import io.domotik8s.model.PropertySpec;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
public class BooleanPropertySpec<A extends PropertyAddress> implements PropertySpec<A, BooleanPropertyState> {

    private Set<PropertyAccess> access;

    private A address;

    private BooleanPropertyState state;

    private Boolean locked;

}
