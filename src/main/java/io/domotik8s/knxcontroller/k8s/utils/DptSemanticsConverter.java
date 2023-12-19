package io.domotik8s.knxcontroller.k8s.utils;

import io.domotik8s.model.bool.BooleanSemantic;
import tuwien.auto.calimero.dptxlator.DPT;

import java.util.HashMap;
import java.util.Map;

public class DptSemanticsConverter {

    private static Map<String, BooleanSemantic> dptSemanticMap = new HashMap<>();

    static {
        dptSemanticMap.put("1.001", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.OFF_ON).reversed(false).build());
        dptSemanticMap.put("1.002", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.FALSE_TRUE).reversed(false).build());
        dptSemanticMap.put("1.003", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.DISABLE_ENABLE).reversed(false).build());
        dptSemanticMap.put("1.004", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.NORAMP_RAMP).reversed(false).build());
        dptSemanticMap.put("1.005", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.NOALARM_ALARM).reversed(false).build());
        dptSemanticMap.put("1.006", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.LOW_HIGH).reversed(false).build());
        dptSemanticMap.put("1.007", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.DECREASE_INCREASE).reversed(false).build());
        dptSemanticMap.put("1.008", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.UP_DOWN).reversed(false).build());
        dptSemanticMap.put("1.009", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.OPEN_CLOSE).reversed(false).build());
        dptSemanticMap.put("1.010", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.STOP_START).reversed(false).build());
        dptSemanticMap.put("1.011", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.INACTIVE_ACTIVE).reversed(false).build());
        dptSemanticMap.put("1.012", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.NOTINVERTED_INVERTED).reversed(false).build());
        dptSemanticMap.put("1.013", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.STARSTOP_CYCLICALLY).reversed(false).build());
        dptSemanticMap.put("1.014", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.FIXED_CALCULATED).reversed(false).build());
        dptSemanticMap.put("1.015", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.DUMMY_TRIGGER).reversed(false).build());
        dptSemanticMap.put("1.016", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.TRIGGER_TRIGGER).reversed(false).build());
        dptSemanticMap.put("1.017", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.NOTOCCUPIED_OCCUPIED).reversed(false).build());
        dptSemanticMap.put("1.018", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.CLOSED_OPEN).reversed(false).build());
        dptSemanticMap.put("1.019", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.OR_AND).reversed(false).build());
        dptSemanticMap.put("1.021", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.SCENEA_SCENEB).reversed(false).build());
        dptSemanticMap.put("1.022", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.UPDOWN_UPDOWNSTEPSTOP).reversed(false).build());
        dptSemanticMap.put("1.023", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.COOLING_HEATING).reversed(false).build());

    }

    public static BooleanSemantic dptToSemantic(String dpt) {
        return dptSemanticMap.get(dpt);
    }

    public static BooleanSemantic dptToSemantic(DPT dpt) {
        return dptSemanticMap.get(dpt.getID());
    }

}
