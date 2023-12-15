package io.domotik8s.model.generic.num;

import io.domotik8s.model.generic.PropertyState;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

@Data
@SuperBuilder
public class NumberPropertyState extends PropertyState<Number> {

    public NumberPropertyState() {

    }


    public void setValue(DPTXlator xlator, DPT dpt) throws KNXFormatException {
        int dptValue = getDptValue(dpt);
        Number value = toNumber(dptValue, xlator.getNumericValue());
        setValue(value);
    }

    private int getDptValue(DPT dpt) {
        String dptId = dpt.getID();
        String[] dptIdTokens = dptId.split("\\.");
        Integer value = Integer.parseInt(dptIdTokens[0]);
        return value;
    }

    private Number toNumber(int dptValue, double dptNumericValue) {
        Number retVal = Double.valueOf(dptNumericValue);
        if ((dptValue >= 2 && dptValue <= 8) ||
            (dptValue >= 10 && dptValue <= 13) ||
            (dptValue >= 20 && dptValue <= 211) ||
            dptValue == 214 || dptValue == 217 ||
            dptValue == 220 ||
            dptValue == 223 ||
            dptValue == 225 ||
            (dptValue >= 231 && dptValue <= 234) ||
            (dptValue >= 236 && dptValue <= 241)
        ) {
            retVal = retVal.longValue();
        }
        return retVal;
    }

}
