package io.domotik8s.model.numericsensor;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NumericSensorState {

  @SerializedName("value")
  @ApiModelProperty(value = "")
  private Double value;

  @SerializedName("unit")
  @ApiModelProperty(value = "")
  private String unit;

}

