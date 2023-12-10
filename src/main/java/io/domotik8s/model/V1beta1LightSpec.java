package io.domotik8s.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class V1beta1LightSpec<C> {

  @SerializedName("capabilities")
  @ApiModelProperty(value = "")
  private V1beta1LightSpecCapabilities capabilities;

  @SerializedName("connection")
  @ApiModelProperty(value = "")
  private V1beta1LightSpecConnection<C> connection;

  @SerializedName("state")
  @ApiModelProperty(value = "")
  private V1beta1LightState state;

  @SerializedName("enforce")
  @ApiModelProperty(value = "Enforce the desired state or accept current state changes as the new desired state.")
  private Boolean enforce;

}

