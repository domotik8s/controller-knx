package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.model.Property;
import io.domotik8s.model.PropertyAddress;
import io.domotik8s.model.PropertySpec;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@Component
public class SystemInstanceFilter implements Predicate<Property> {

    @Override
    public boolean test(Property property) {
        Optional<PropertyAddress> address = Optional.ofNullable(property)
                .map(Property::getSpec)
                .map(PropertySpec::getAddress);

        if (address.isPresent()) {
            PropertyAddress addrVal = address.get();
            return "knx".equals(addrVal.getSystem());
        }
        return false;
    }

}
