package io.domotik8s.knxcontroller.knx.convert;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConfigurationPropertiesBinding
public class StringToDptConverter implements Converter<String, DPT> {

    private Map<String, DPT> map = new HashMap<>();

    public static <T> Set<Class<? extends T>> findAllClasses(String packageName, Class<T> type) {
        Reflections reflections = new Reflections(packageName, Scanners.SubTypes);
        return reflections.getSubTypesOf(type)
                .stream()
                .collect(Collectors.toSet());
    }

    public static Set<Field> getStaticFields(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        Set<Field> staticFields = new HashSet<Field>();
        for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                staticFields.add(field);
            }
        }
        return staticFields;
    }


    public StringToDptConverter() {
        String packageName = "tuwien.auto.calimero.dptxlator";
        Set<Class<? extends DPTXlator>> classes = findAllClasses(packageName, DPTXlator.class);

        Set<Field> dptFields = new HashSet<>();
        for (Class<?> clazz: classes) {
            Set<Field> fields = getStaticFields(clazz).stream()
                    .filter(f -> f.getType().isAssignableFrom(DPT.class))
                    .collect(Collectors.toSet());
            dptFields.addAll(fields);
        }

        for (Field f: dptFields) {
            try {
                DPT dpt = (DPT) f.get(null);
                map.put(dpt.getID(), dpt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public DPT convert(String source) {
        if (source == null || source.trim().length() == 0)
            throw new RuntimeException("Unable to convert null or empty string to DPT.");
        DPT dpt = map.get(source);

        if (dpt != null)
            return dpt;

        throw new RuntimeException("Unable to convert string " + source + " to an instance of DPT.");
    }

}
