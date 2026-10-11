package hai913i.tp1.graph;

// B3 : arc du graphe, avec le nombre de sites d'appel.
public record CallEdge(String caller, String callee, int weight) {

    public CallEdge {
        if (weight <= 0) {
            throw new IllegalArgumentException("Le poids d'un arc doit être positif");
        }
    }
}
