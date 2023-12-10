package io.domotik8s.knxcontroller.k8s.model;

import io.domotik8s.model.PropertyAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class KnxPropertyAddress extends PropertyAddress {

    private String read;

    private String write;

    private String dpt;

}
