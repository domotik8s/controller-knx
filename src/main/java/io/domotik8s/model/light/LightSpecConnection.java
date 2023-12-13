package io.domotik8s.model.light;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LightSpecConnection<C> {

  @SerializedName("config")
  @ApiModelProperty(required = true, value = "")
  private C config;

  @SerializedName("instance")
  @ApiModelProperty(value = "")
  private String instance;

  @SerializedName("system")
  @ApiModelProperty(required = true, value = "")
  private String system;

}

