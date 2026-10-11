package hai913i.tp1.metrics;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeInfo;

// B2 : calcul des métriques à partir de ProjectFacts.
public final class MetricsCalculator {

    public MetricsReport calculate(ProjectFacts facts, int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Le seuil X doit être positif ou nul");
        }

        List<TypeInfo> types = facts.types();
        int classCount = types.size();
        int methodCount = types.stream()
                .mapToInt(type -> type.methods().size())
                .sum();
        int attributeCount = types.stream()
                .mapToInt(type -> type.attributes().size())
                .sum();
        int methodsWithBody = types.stream()
                .flatMap(type -> type.methods().stream())
                .mapToInt(method -> method.bodyLines() > 0 ? 1 : 0)
                .sum();
        int bodyLines = types.stream()
                .flatMap(type -> type.methods().stream())
                .mapToInt(MethodInfo::bodyLines)
                .sum();

        double classDenominator = classCount == 0 ? 1.0 : classCount;
        double methodDenominator = methodsWithBody == 0 ? 1.0 : methodsWithBody;

        List<String> topMethods = topClasses(types, type -> type.methods().size());
        List<String> topAttributes = topClasses(types, type -> type.attributes().size());
        Set<String> topMethodSet = new HashSet<>(topMethods);
        List<String> intersection = topAttributes.stream()
                .filter(topMethodSet::contains)
                .sorted()
                .toList();

        Map<String, List<String>> topMethodsByClass = new LinkedHashMap<>();
        for (TypeInfo type : types.stream()
                .sorted(Comparator.comparing(TypeInfo::qualifiedName))
                .toList()) {
            topMethodsByClass.put(type.qualifiedName(), topMethods(type.methods()));
        }

        int maxParameters = types.stream()
                .flatMap(type -> type.methods().stream())
                .mapToInt(MethodInfo::parameterCount)
                .max()
                .orElse(0);
        List<String> methodsWithMaxParameters = types.stream()
                .flatMap(type -> type.methods().stream())
                .filter(method -> method.parameterCount() == maxParameters)
                .map(MethodInfo::id)
                .sorted()
                .toList();

        return new MetricsReport(
                classCount,
                facts.applicationLines(),
                methodCount,
                (int) types.stream().map(TypeInfo::packageName).distinct().count(),
                methodCount / classDenominator,
                bodyLines / methodDenominator,
                attributeCount / classDenominator,
                topMethods,
                topAttributes,
                intersection,
                types.stream()
                        .filter(type -> type.methods().size() > threshold)
                        .map(TypeInfo::qualifiedName)
                        .sorted()
                        .toList(),
                topMethodsByClass,
                maxParameters,
                methodsWithMaxParameters);
    }

    // Top 10 % : arrondi supérieur, égalités incluses, zéros exclus.
    private List<String> topClasses(List<TypeInfo> types, ToIntFunction<TypeInfo> value) {
        int candidateCount = types.size();
        if (candidateCount == 0) {
            return List.of();
        }

        int k = (candidateCount + 9) / 10;
        List<TypeInfo> positive = types.stream()
                .filter(type -> value.applyAsInt(type) > 0)
                .sorted(Comparator
                        .comparingInt(value).reversed()
                        .thenComparing(TypeInfo::qualifiedName))
                .toList();
        if (positive.isEmpty()) {
            return List.of();
        }

        TypeInfo boundaryType = positive.get(Math.min(k, positive.size()) - 1);
        int boundary = value.applyAsInt(boundaryType);
        return positive.stream()
                .filter(type -> value.applyAsInt(type) >= boundary)
                .map(TypeInfo::qualifiedName)
                .toList();
    }

    private List<String> topMethods(List<MethodInfo> methods) {
        int candidateCount = methods.size();
        if (candidateCount == 0) {
            return List.of();
        }

        int k = (candidateCount + 9) / 10;
        List<MethodInfo> positive = methods.stream()
                .filter(method -> method.bodyLines() > 0)
                .sorted(Comparator
                        .comparingInt(MethodInfo::bodyLines).reversed()
                        .thenComparing(MethodInfo::id))
                .toList();
        if (positive.isEmpty()) {
            return List.of();
        }

        int boundary = positive.get(Math.min(k, positive.size()) - 1).bodyLines();
        return positive.stream()
                .filter(method -> method.bodyLines() >= boundary)
                .map(MethodInfo::id)
                .toList();
    }
}
