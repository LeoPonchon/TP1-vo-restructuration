package hai913i.tp1.model;

import java.util.List;

public record ProjectFacts(
        List<TypeInfo> types,
        List<MethodCallInfo> calls,
        int applicationLines) {

    public ProjectFacts {
        types = List.copyOf(types);
        calls = List.copyOf(calls);
        if (applicationLines < 0) {
            throw new IllegalArgumentException("Le nombre de lignes ne peut pas être négatif");
        }
    }
}
