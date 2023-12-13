package io.domotik8s.model;

import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ListMeta;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(description = "List of Objects")
public abstract class DefaultList<T extends KubernetesObject> implements KubernetesListObject {

  @SerializedName("apiVersion")
  private String apiVersion;

  @SerializedName("items")
  private List<T> items = new ArrayList<>();

  @SerializedName("kind")
  private String kind;

  @SerializedName("metadata")
  private V1ListMeta metadata = null;

}

