package io.domotik8s.model;

public interface PropertySpec<A extends PropertyAddress, ST extends PropertyState> {

    A getAddress();

    ST getState();

    Boolean getLocked();

}
