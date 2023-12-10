package io.domotik8s.knxcontroller.k8s.model;

import io.domotik8s.model.bool.BooleanPropertySpec;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class KnxBooleanPropertySpec extends BooleanPropertySpec<KnxPropertyAddress> {
}
