package io.domotik8s.model.bool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BooleanSemantic {

    public enum Meaning {
        OFF_ON,
        FALSE_TRUE,
        DISABLE_ENABLE,
        NORAMP_RAMP,
        NOALARM_ALARM,
        LOW_HIGH,
        DECREASE_INCREASE,
        UP_DOWN,
        OPEN_CLOSE,
        STOP_START,
        INACTIVE_ACTIVE,
        NOTINVERTED_INVERTED,
        STARSTOP_CYCLICALLY,
        FIXED_CALCULATED,
        DUMMY_TRIGGER,
        TRIGGER_TRIGGER,
        NOTOCCUPIED_OCCUPIED,
        CLOSED_OPEN,
        OR_AND,
        SCENEA_SCENEB,
        UPDOWN_UPDOWNSTEPSTOP,
        COOLING_HEATING
    }

    private Meaning meaning;

    private boolean reversed = false;

}
