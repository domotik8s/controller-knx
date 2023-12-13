package io.domotik8s.model.light;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;


@Data
public class LightStatus {

  @SerializedName("lastUpdated")
  private OffsetDateTime lastUpdated;

  @SerializedName("state")
  private LightState state;

}

