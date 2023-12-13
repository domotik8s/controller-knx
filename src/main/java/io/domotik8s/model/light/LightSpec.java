package io.domotik8s.model.light;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LightSpec<C> {

  @SerializedName("capabilities")
  @ApiModelProperty(value = "")
  private LightSpecCapabilities capabilities;

  @SerializedName("connection")
  @ApiModelProperty(value = "")
  private LightSpecConnection<C> connection;

  @SerializedName("state")
  @ApiModelProperty(value = "")
  private LightState state;

  @SerializedName("enforce")
  @ApiModelProperty(value = "Enforce the desired state or accept current state changes as the new desired state.")
  private Boolean enforce;

}

