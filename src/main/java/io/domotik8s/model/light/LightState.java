package io.domotik8s.model.light;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LightState {

  @SerializedName("brightness")
  @ApiModelProperty(value = "")
  private Double brightness;

  @SerializedName("color")
  @ApiModelProperty(value = "")
  private String color;

  @SerializedName("power")
  @ApiModelProperty(value = "")
  private Boolean power;

}

