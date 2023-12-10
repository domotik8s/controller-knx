package io.domotik8s.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class V1beta1LightSpecCapabilities {

  @SerializedName("brightness")
  @ApiModelProperty(value = "")
  private Boolean brightness;

  @SerializedName("color")
  @ApiModelProperty(value = "")
  private Boolean color;

  @SerializedName("power")
  @ApiModelProperty(value = "")
  private Boolean power;

}

