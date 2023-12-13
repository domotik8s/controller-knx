package io.domotik8s.knxcontroller.knx.utils;

import tec.units.ri.unit.Units;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;

import javax.measure.Unit;
import java.util.Map;

public class DptUnitConverter {

    private static final Map<DPT, Unit<?>> dpt2UnitMap = Map.of(
            DPTXlator8BitUnsigned.DPT_SCALING, Units.PERCENT,
            DPTXlator2ByteFloat.DPT_TEMPERATURE, Units.CELSIUS,
            DPTXlator2ByteFloat.DPT_WIND_SPEED, Units.METRE_PER_SECOND,
            DPTXlator2ByteFloat.DPT_INTENSITY_OF_LIGHT, Units.LUX
    );

    public Unit toUnit(DPT dpt) {
        return dpt2UnitMap.get(dpt);
    }

}
