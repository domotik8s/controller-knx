package io.domotik8s.model.bool;

import io.domotik8s.model.Property;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BooleanProperty<SP extends BooleanPropertySpec> implements Property<SP, BooleanPropertyStatus> {

    private final String apiVersion = "domotik8s.io/v1beta1";

    private final String kind = "BooleanProperty";

    private V1ObjectMeta metadata;

    private SP spec;

    private BooleanPropertyStatus status;

}
