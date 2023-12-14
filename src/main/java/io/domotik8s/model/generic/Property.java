package io.domotik8s.model.generic;

import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@RequiredArgsConstructor
@SuperBuilder
public abstract class Property<A extends PropertySystemAddress, S extends PropertyState> implements KubernetesObject {

    @SerializedName("apiVersion")
    private final String apiVersion = "domotik8s.io";

    @SerializedName("kind")
    private final String kind;

    @SerializedName("metadata")
    private V1ObjectMeta metadata;

    @SerializedName("spec")
    private PropertySpec<A, S> spec;

    @SerializedName("status")
    private PropertyStatus<S> status;

}