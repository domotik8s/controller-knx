package io.domotik8s.model.numericsensor;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;


@Data
public class NumericSensorStatus {

  @SerializedName("lastUpdated")
  private OffsetDateTime lastUpdated;

  @SerializedName("state")
  private NumericSensorState state;

}

