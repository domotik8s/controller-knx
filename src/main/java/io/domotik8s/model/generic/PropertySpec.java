package io.domotik8s.model.generic;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@NoArgsConstructor
@SuperBuilder
public class PropertySpec<A extends PropertySystemAddress, S extends PropertyState> {

    @SerializedName("address")
    private A address;

    @SerializedName("state")
    private S state;

    @SerializedName("access")
    private Set<PropertyAccess> access;

}
