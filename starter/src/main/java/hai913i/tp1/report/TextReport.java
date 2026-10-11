package hai913i.tp1.report;

import java.util.Locale;

import hai913i.tp1.graph.CallEdge;
import hai913i.tp1.graph.CallGraph;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.model.MethodCallInfo;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeInfo;

// Affichage des résultats.
public final class TextReport {

    private TextReport() {
    }

    // A2 : détail de la structure extraite.
    public static void structure(ProjectFacts facts) {
        System.out.println("// A2 — Structure");
        System.out.println("Types détectés : " + facts.types().size());
        for (TypeInfo type : facts.types()) {
            System.out.println(type.qualifiedName() + " [" + type.kind() + "] paquetage="
                    + printablePackage(type.packageName()));
            System.out.println("  superclasses : " + type.superclasses());
            System.out.println("  interfaces   : " + type.interfaces());
            System.out.println("  attributs    : " + type.attributes());
            System.out.println("  méthodes     : " + type.methods());
        }
    }

    // A3 : appels avec receveur, cible, ligne et état de résolution.
    public static void calls(ProjectFacts facts) {
        System.out.println("// A3 — Appels");
        for (MethodCallInfo call : facts.calls()) {
            System.out.println(call);
        }
        long internal = facts.calls().stream().filter(call -> call.resolution().equals("INTERNAL")).count();
        long external = facts.calls().stream().filter(call -> call.resolution().equals("EXTERNAL")).count();
        long unresolved = facts.calls().stream().filter(call -> call.resolution().equals("UNRESOLVED")).count();
        System.out.println("Appels détectés : " + facts.calls().size());
        System.out.println("Appels internes : " + internal);
        System.out.println("Appels externes : " + external);
        System.out.println("Appels non résolus : " + unresolved);
    }

    // B2 : métriques calculées à partir de ProjectFacts.
    public static void metrics(MetricsReport report, int threshold) {
        System.out.println("// B2 — Métriques");
        System.out.println("Q1 Classes/types : " + report.classCount());
        System.out.println("Q2 Lignes physiques Java : " + report.applicationLines());
        System.out.println("Q3 Méthodes : " + report.methodCount());
        System.out.println("Q4 Paquetages : " + report.packageCount());
        System.out.printf(Locale.ROOT, "Q5 Méthodes/classe : %.2f%n", report.averageMethodsPerClass());
        System.out.printf(Locale.ROOT, "Q6 Lignes de corps/méthode avec corps : %.2f%n",
                report.averageBodyLinesPerMethod());
        System.out.printf(Locale.ROOT, "Q7 Attributs/classe : %.2f%n", report.averageAttributesPerClass());
        System.out.println("Q8 Top classes par nombre de méthodes : " + report.topClassesByMethods());
        System.out.println("Q9 Top classes par nombre d'attributs : " + report.topClassesByAttributes());
        System.out.println("Q10 Intersection des deux tops : " + report.classesInBothTop10());
        System.out.println("Q11 Classes avec plus de " + threshold + " méthodes : "
                + report.classesAboveThreshold());
        System.out.println("Q12 Top méthodes par corps :");
        report.topMethodsByClass().forEach((className, methods) ->
                System.out.println("  " + className + " : " + methods));
        System.out.println("Q13 Nombre maximal de paramètres : " + report.maxParameters());
        System.out.println("    Méthodes concernées : " + report.methodsWithMaxParameters());
    }

    // B3 : graphe interne et poids des arcs.
    public static void graph(CallGraph graph, String callersQuery, String calleesQuery) {
        System.out.println("// B3 — Graphe d'appels");
        System.out.println("Nœuds : " + graph.nodes().size());
        System.out.println("Arcs distincts : " + graph.edges().size());
        System.out.println("Sites internes : " + graph.internalSiteCount());
        System.out.println("Sites externes : " + graph.externalSiteCount());
        System.out.println("Sites non résolus : " + graph.unresolvedSiteCount());
        for (CallEdge edge : graph.edges()) {
            System.out.println(edge.caller() + " -> " + edge.callee() + " [" + edge.weight() + "]");
        }
        if (callersQuery != null) {
            System.out.println("Appelants de " + callersQuery + " : " + graph.callers(callersQuery));
        }
        if (calleesQuery != null) {
            System.out.println("Cibles de " + calleesQuery + " : " + graph.callees(calleesQuery));
        }
    }

    private static String printablePackage(String packageName) {
        return packageName.isBlank() ? "(défaut)" : packageName;
    }
}
