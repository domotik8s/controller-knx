package io.domotik8s.model.generic;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@SuperBuilder
public class PropertyStatus<S extends PropertyState> {

    @SerializedName("lastUpdated")
    private OffsetDateTime lastUpdated;

    @SerializedName("state")
    private S state;

}
