package hai913i.tp1.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import hai913i.tp1.model.MethodCallInfo;
import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeInfo;

// B3 : graphe des appels internes construit à partir des faits.
public final class CallGraph {

    private final Set<String> nodes;
    private final List<CallEdge> edges;
    private final Map<String, List<CallEdge>> outgoing;
    private final Map<String, List<CallEdge>> incoming;
    private final int internalSiteCount;
    private final int externalSiteCount;
    private final int unresolvedSiteCount;

    private CallGraph(
            Set<String> nodes,
            List<CallEdge> edges,
            int internalSiteCount,
            int externalSiteCount,
            int unresolvedSiteCount) {
        this.nodes = Collections.unmodifiableSet(new TreeSet<>(nodes));
        this.edges = List.copyOf(edges);
        this.internalSiteCount = internalSiteCount;
        this.externalSiteCount = externalSiteCount;
        this.unresolvedSiteCount = unresolvedSiteCount;

        Map<String, List<CallEdge>> outgoingMutable = new HashMap<>();
        Map<String, List<CallEdge>> incomingMutable = new HashMap<>();
        for (CallEdge edge : edges) {
            outgoingMutable.computeIfAbsent(edge.caller(), key -> new ArrayList<>()).add(edge);
            incomingMutable.computeIfAbsent(edge.callee(), key -> new ArrayList<>()).add(edge);
        }
        this.outgoing = copyLists(outgoingMutable);
        this.incoming = copyLists(incomingMutable);
    }

    // B3 : les nœuds sont toutes les méthodes, les arcs regroupent les sites
    // d'appel internes ayant le même couple (appelant, cible).
    public static CallGraph from(ProjectFacts facts) {
        Set<String> nodes = new HashSet<>();
        for (TypeInfo type : facts.types()) {
            for (MethodInfo method : type.methods()) {
                nodes.add(method.id());
            }
        }

        Map<EdgeKey, Integer> weights = new HashMap<>();
        int internal = 0;
        int external = 0;
        int unresolved = 0;

        for (MethodCallInfo call : facts.calls()) {
            switch (call.resolution()) {
                case "INTERNAL" -> {
                    internal++;
                    if (nodes.contains(call.caller()) && call.target() != null
                            && nodes.contains(call.target())) {
                        weights.merge(new EdgeKey(call.caller(), call.target()), 1, Integer::sum);
                    }
                }
                case "EXTERNAL" -> external++;
                case "UNRESOLVED" -> unresolved++;
                default -> throw new IllegalArgumentException(
                        "Résolution inconnue : " + call.resolution());
            }
        }

        List<CallEdge> edges = weights.entrySet().stream()
                .map(entry -> new CallEdge(
                        entry.getKey().caller(), entry.getKey().callee(), entry.getValue()))
                .sorted(Comparator.comparing(CallEdge::caller).thenComparing(CallEdge::callee))
                .toList();

        return new CallGraph(nodes, edges, internal, external, unresolved);
    }

    public Set<String> nodes() {
        return nodes;
    }

    public List<CallEdge> edges() {
        return edges;
    }

    public List<CallEdge> callees(String methodId) {
        return outgoing.getOrDefault(methodId, List.of());
    }

    public List<CallEdge> callers(String methodId) {
        return incoming.getOrDefault(methodId, List.of());
    }

    public int internalSiteCount() {
        return internalSiteCount;
    }

    public int externalSiteCount() {
        return externalSiteCount;
    }

    public int unresolvedSiteCount() {
        return unresolvedSiteCount;
    }

    private Map<String, List<CallEdge>> copyLists(Map<String, List<CallEdge>> source) {
        Map<String, List<CallEdge>> result = new HashMap<>();
        for (Map.Entry<String, List<CallEdge>> entry : source.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private record EdgeKey(String caller, String callee) {
    }
}
