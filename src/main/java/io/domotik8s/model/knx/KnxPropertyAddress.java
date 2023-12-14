package io.domotik8s.model.knx;

import com.google.gson.annotations.SerializedName;
import io.domotik8s.model.generic.PropertySystemAddress;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class KnxPropertyAddress extends PropertySystemAddress {

    @SerializedName("read")
    private String read;

    @SerializedName("write")
    private String write;

    @SerializedName("dpt")
    private String dpt;

}
