package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import hai913i.tp1.graph.CallEdge;
import hai913i.tp1.graph.CallGraph;
import hai913i.tp1.metrics.MetricsCalculator;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.model.AttributeInfo;
import hai913i.tp1.model.MethodCallInfo;
import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeInfo;

// B2/B3 : tests du modèle sans JDT.
class ModelAnalysisTest {

    @Test
    void calculatesMetricsFromFactsOnly() {
        TypeInfo first = type("demo.First", List.of(
                method("demo.First#a()", "a", 0, 7),
                method("demo.First#b()", "b", 2, 3)), 2);
        TypeInfo second = type("demo.Second", List.of(
                method("demo.Second#c()", "c", 1, 0)), 1);

        MetricsReport report = new MetricsCalculator().calculate(
                new ProjectFacts(List.of(first, second), List.of(), 42), 1);

        assertEquals(2, report.classCount());
        assertEquals(42, report.applicationLines());
        assertEquals(3, report.methodCount());
        assertEquals(1.5, report.averageMethodsPerClass());
        assertEquals(5.0, report.averageBodyLinesPerMethod());
        assertEquals(1.5, report.averageAttributesPerClass());
        assertEquals(List.of("demo.First"), report.topClassesByMethods());
        assertEquals(List.of("demo.First"), report.classesAboveThreshold());
        assertEquals(2, report.maxParameters());
    }

    @Test
    void aggregatesGraphEdgesAndKeepsReverseQueries() {
        String first = "demo.First#a()";
        String second = "demo.Second#b()";
        ProjectFacts facts = new ProjectFacts(
                List.of(
                        type("demo.First", List.of(method(first, "a", 0, 1)), 0),
                        type("demo.Second", List.of(method(second, "b", 0, 1)), 0),
                        type("demo.Third", List.of(method("demo.Third#c()", "c", 0, 1)), 0)),
                List.of(
                        call(first, second, "INTERNAL"),
                        call(first, second, "INTERNAL"),
                        call(second, second, "INTERNAL"),
                        call(first, "java.lang.String#valueOf(char)", "EXTERNAL"),
                        new MethodCallInfo(first, "missing", null, null, 9, "UNRESOLVED")),
                3);

        CallGraph graph = CallGraph.from(facts);

        assertEquals(3, graph.nodes().size());
        assertEquals(2, graph.edges().size());
        assertEquals(3, graph.internalSiteCount());
        assertEquals(1, graph.externalSiteCount());
        assertEquals(1, graph.unresolvedSiteCount());
        assertEquals(1, graph.callees(first).size());
        assertEquals(new CallEdge(first, second, 2), graph.callees(first).get(0));
        assertTrue(graph.callers(second).stream().anyMatch(edge -> edge.caller().equals(first)));
    }

    private static TypeInfo type(String name, List<MethodInfo> methods, int attributes) {
        List<AttributeInfo> fields = java.util.stream.IntStream.range(0, attributes)
                .mapToObj(index -> new AttributeInfo("field" + index, "int", "private"))
                .toList();
        return new TypeInfo(name, "demo", "CLASS", List.of(), List.of(), fields, methods);
    }

    private static MethodInfo method(String id, String name, int parameters, int bodyLines) {
        return new MethodInfo(id, name, parameters, false, bodyLines);
    }

    private static MethodCallInfo call(String caller, String target, String resolution) {
        return new MethodCallInfo(caller, "call", "demo.Type", target, 1, resolution);
    }
}
