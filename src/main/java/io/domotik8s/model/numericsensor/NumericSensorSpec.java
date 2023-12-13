package io.domotik8s.model.numericsensor;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NumericSensorSpec<C> {

  @SerializedName("connection")
  @ApiModelProperty(value = "")
  private NumericSensorSpecConnection<C> connection;

  @SerializedName("state")
  @ApiModelProperty(value = "")
  private NumericSensorState state;

}

