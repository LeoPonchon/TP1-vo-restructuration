package hai913i.tp1.model;

// A3/B3 : un appel trouvé dans une méthode.
public record MethodCallInfo(
        String caller,
        String calledName,
        String receiverType,
        String target,
        int line,
        String resolution) {
}
