package io.domotik8s.model.numericsensor;

import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.annotation.Nullable;

@Data
public abstract class NumericSensor<C> implements KubernetesObject {

    @Nullable
    @SerializedName("apiVersion")
    @ApiModelProperty(value = "")
    private String apiVersion;

    @SerializedName("kind")
    @ApiModelProperty(value = "")
    private String kind;

    @SerializedName("metadata")
    @ApiModelProperty(value = "")
    private V1ObjectMeta metadata = null;

    @SerializedName("spec")
    @ApiModelProperty(value = "")
    private NumericSensorSpec<C> spec;

    @SerializedName("status")
    @ApiModelProperty(value = "")
    private NumericSensorStatus status;

}