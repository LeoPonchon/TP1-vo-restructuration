package hai913i.tp1.extract;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

// B1 : identifiant d'une méthode, y compris pour les surcharges.
final class MethodIds {

    private MethodIds() {
    }

    static String of(MethodDeclaration method, ITypeBinding owner) {
        IMethodBinding binding = method.resolveBinding();
        if (binding != null) {
            return of(binding);
        }

        String ownerName = owner == null ? "<unknown>" : typeName(owner);
        StringBuilder parameters = new StringBuilder();
        for (Object parameter : method.parameters()) {
            if (parameters.length() > 0) {
                parameters.append(",");
            }
            parameters.append(parameter);
        }

        return ownerName + "#" + method.getName().getIdentifier()
                + "(" + parameters + ")";
    }

    static String of(IMethodBinding binding) {
        IMethodBinding declaration = binding.getMethodDeclaration();
        String owner = typeName(declaration.getDeclaringClass());
        StringBuilder parameters = new StringBuilder();

        for (ITypeBinding parameter : declaration.getParameterTypes()) {
            if (parameters.length() > 0) {
                parameters.append(",");
            }
            parameters.append(typeName(parameter));
        }

        return owner + "#" + declaration.getName()
                + "(" + parameters + ")";
    }

    private static String typeName(ITypeBinding binding) {
        if (binding == null) {
            return "<unknown>";
        }
        return binding.getErasure().getQualifiedName();
    }
}
