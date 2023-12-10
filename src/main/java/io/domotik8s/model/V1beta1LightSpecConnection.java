package io.domotik8s.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class V1beta1LightSpecConnection<C> {

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

