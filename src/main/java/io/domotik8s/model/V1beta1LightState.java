package io.domotik8s.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class V1beta1LightState {

  @SerializedName("brightness")
  @ApiModelProperty(value = "")
  private Integer brightness;

  @SerializedName("color")
  @ApiModelProperty(value = "")
  private String color;

  @SerializedName("power")
  @ApiModelProperty(value = "")
  private Boolean power;

}

