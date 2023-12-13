package io.domotik8s.model.light;

import io.domotik8s.model.DefaultList;
import io.swagger.annotations.ApiModel;

@ApiModel(description = "List of Lights")
public class LightList<T extends Light> extends DefaultList<T> {

}

