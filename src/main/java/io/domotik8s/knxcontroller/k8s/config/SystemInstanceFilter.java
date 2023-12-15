package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.model.generic.Property;
import io.domotik8s.model.generic.PropertySpec;
import io.domotik8s.model.generic.PropertySystemAddress;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

import static io.domotik8s.knxcontroller.k8s.Constants.SYSTEM_TYPE;

@Component
public class SystemInstanceFilter implements Predicate<Property> {

    @Override
    public boolean test(Property property) {
        Optional<PropertySystemAddress> address = Optional.ofNullable(property)
                .map(Property::getSpec)
                .map(PropertySpec::getAddress);

        if (address.isPresent()) {
            PropertySystemAddress addrVal = address.get();
            return SYSTEM_TYPE.equals(addrVal.getSystem());
        }
        return false;
    }

}
