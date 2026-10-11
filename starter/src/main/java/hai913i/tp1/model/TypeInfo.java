package hai913i.tp1.model;

import java.util.List;

// A2/B1 : un type du projet.
public record TypeInfo(
        String qualifiedName,
        String packageName,
        String kind,
        List<String> superclasses,
        List<String> interfaces,
        List<AttributeInfo> attributes,
        List<MethodInfo> methods) {
}
