package io.domotik8s.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;


@Data
public class V1beta1LightStatus {

  @SerializedName("lastUpdated")
  private OffsetDateTime lastUpdated;

  @SerializedName("state")
  private V1beta1LightState state;

}

