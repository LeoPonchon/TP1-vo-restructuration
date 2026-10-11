package hai913i.tp1.extract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import hai913i.tp1.model.AttributeInfo;
import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.TypeInfo;
import hai913i.tp1.parse.JdtParser.ParsedFile;

// A2 : extraction des types, champs et méthodes.
public class StructureExtractor {

    public List<TypeInfo> extract(List<ParsedFile> files) {
        List<TypeInfo> result = new ArrayList<>();

        for (ParsedFile file : files) {
            CompilationUnit unit = file.unit();

            String packageName = unit.getPackage() == null
                    ? ""
                    : unit.getPackage().getName().getFullyQualifiedName();

            for (Object element : unit.types()) {
                if (element instanceof AbstractTypeDeclaration type) {
                    addType(type, packageName, unit, result);
                }
            }
        }

        result.sort(Comparator.comparing(TypeInfo::qualifiedName));
        return result;
    }

    private void addType(
            AbstractTypeDeclaration type,
            String packageName,
            CompilationUnit unit,
            List<TypeInfo> result) {

        if (type instanceof AnnotationTypeDeclaration) {
            return;
        }

        ITypeBinding binding = type.resolveBinding();

        List<String> superclasses = getSuperclasses(binding);
        List<String> interfaces = getInterfaces(binding);
        List<AttributeInfo> attributes = new ArrayList<>();
        List<MethodInfo> methods = new ArrayList<>();

        for (Object member : type.bodyDeclarations()) {
            if (member instanceof FieldDeclaration field) {
                for (Object fragment : field.fragments()) {
                    VariableDeclarationFragment variable =
                            (VariableDeclarationFragment) fragment;
                    attributes.add(new AttributeInfo(
                            variable.getName().getIdentifier(),
                            field.getType().toString(),
                            getVisibility(field.getModifiers())));
                }
            } else if (member instanceof MethodDeclaration method) {
                methods.add(new MethodInfo(
                        MethodIds.of(method, binding),
                        method.getName().getIdentifier(),
                        method.parameters().size(),
                        method.isConstructor(),
                        bodyLines(unit, method)));
            }
        }

        result.add(new TypeInfo(
                binding == null
                        ? type.getName().getIdentifier()
                        : binding.getQualifiedName(),
                packageName,
                getKind(type),
                superclasses,
                interfaces,
                attributes,
                methods));

        for (Object member : type.bodyDeclarations()) {
            if (member instanceof AbstractTypeDeclaration nestedType) {
                addType(nestedType, packageName, unit, result);
            }
        }
    }

    private int bodyLines(CompilationUnit unit, MethodDeclaration method) {
        Block body = method.getBody();
        if (body == null) {
            return 0;
        }

        int firstLine = unit.getLineNumber(body.getStartPosition());
        int lastLine = unit.getLineNumber(
                body.getStartPosition() + body.getLength() - 1);
        return lastLine - firstLine + 1;
    }

    private String getKind(AbstractTypeDeclaration type) {
        if (type instanceof EnumDeclaration) {
            return "ENUM";
        }

        if (type instanceof RecordDeclaration) {
            return "RECORD";
        }

        if (type instanceof TypeDeclaration declaration
                && declaration.isInterface()) {
            return "INTERFACE";
        }

        return "CLASS";
    }

    private List<String> getSuperclasses(ITypeBinding binding) {
        List<String> result = new ArrayList<>();

        if (binding == null) {
            return result;
        }

        ITypeBinding superclass = binding.getSuperclass();
        while (superclass != null) {
            String name = superclass.getErasure().getQualifiedName();
            if (!name.equals("java.lang.Object")) {
                result.add(name);
            }
            superclass = superclass.getSuperclass();
        }

        return result;
    }

    private List<String> getInterfaces(ITypeBinding binding) {
        List<String> result = new ArrayList<>();

        if (binding == null) {
            return result;
        }

        for (ITypeBinding anInterface : binding.getInterfaces()) {
            result.add(anInterface.getErasure().getQualifiedName());
        }

        return result;
    }

    private String getVisibility(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return "public";
        }
        if (Modifier.isProtected(modifiers)) {
            return "protected";
        }
        if (Modifier.isPrivate(modifiers)) {
            return "private";
        }
        return "package";
    }
}
