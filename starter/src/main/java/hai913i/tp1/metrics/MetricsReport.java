package hai913i.tp1.metrics;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// B2 : résultats des métriques.
public record MetricsReport(
        int classCount,
        int applicationLines,
        int methodCount,
        int packageCount,
        double averageMethodsPerClass,
        double averageBodyLinesPerMethod,
        double averageAttributesPerClass,
        List<String> topClassesByMethods,
        List<String> topClassesByAttributes,
        List<String> classesInBothTop10,
        List<String> classesAboveThreshold,
        Map<String, List<String>> topMethodsByClass,
        int maxParameters,
        List<String> methodsWithMaxParameters) {

    public MetricsReport {
        topClassesByMethods = List.copyOf(topClassesByMethods);
        topClassesByAttributes = List.copyOf(topClassesByAttributes);
        classesInBothTop10 = List.copyOf(classesInBothTop10);
        classesAboveThreshold = List.copyOf(classesAboveThreshold);
        Map<String, List<String>> copiedMethods = new LinkedHashMap<>();
        topMethodsByClass.forEach((className, methods) ->
                copiedMethods.put(className, List.copyOf(methods)));
        topMethodsByClass = Collections.unmodifiableMap(copiedMethods);
        methodsWithMaxParameters = List.copyOf(methodsWithMaxParameters);
    }
}
