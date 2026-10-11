package hai913i.tp1.model;

// A2/B1 : une méthode et les informations utiles aux métriques.
public record MethodInfo(
        String id,
        String name,
        int parameterCount,
        boolean constructor,
        int bodyLines) {
}
