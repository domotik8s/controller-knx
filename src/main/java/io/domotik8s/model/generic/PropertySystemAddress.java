package io.domotik8s.model.generic;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public abstract class PropertySystemAddress {

    @SerializedName("system")
    private String system;

    @SerializedName("instance")
    private String instance;

}
