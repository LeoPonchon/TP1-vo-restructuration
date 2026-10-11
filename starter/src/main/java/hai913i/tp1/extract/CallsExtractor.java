package hai913i.tp1.extract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import hai913i.tp1.model.MethodCallInfo;
import hai913i.tp1.parse.JdtParser.ParsedFile;

// A3 : extraction des appels et de leur résolution.
public class CallsExtractor {

    public List<MethodCallInfo> extract(List<ParsedFile> files) {
        Set<String> projectMethods = new HashSet<>();

        for (ParsedFile file : files) {
            for (Object element : file.unit().types()) {
                if (element instanceof AbstractTypeDeclaration type) {
                    collectMethods(type, projectMethods);
                }
            }
        }

        List<MethodCallInfo> calls = new ArrayList<>();

        for (ParsedFile file : files) {
            for (Object element : file.unit().types()) {
                if (element instanceof AbstractTypeDeclaration type) {
                    extractCalls(type, file.unit(), projectMethods, calls);
                }
            }
        }

        calls.sort(Comparator
                .comparing(MethodCallInfo::caller)
                .thenComparingInt(MethodCallInfo::line)
                .thenComparing(MethodCallInfo::calledName));

        return calls;
    }

    private void collectMethods(
            AbstractTypeDeclaration type,
            Set<String> projectMethods) {

        if (type instanceof AnnotationTypeDeclaration) {
            return;
        }

        ITypeBinding owner = type.resolveBinding();

        for (Object member : type.bodyDeclarations()) {
            if (member instanceof MethodDeclaration method) {
                projectMethods.add(MethodIds.of(method, owner));
            } else if (member instanceof AbstractTypeDeclaration nestedType) {
                collectMethods(nestedType, projectMethods);
            }
        }
    }

    private void extractCalls(
            AbstractTypeDeclaration type,
            CompilationUnit unit,
            Set<String> projectMethods,
            List<MethodCallInfo> calls) {

        if (type instanceof AnnotationTypeDeclaration) {
            return;
        }

        ITypeBinding owner = type.resolveBinding();

        for (Object member : type.bodyDeclarations()) {
            if (member instanceof MethodDeclaration method) {
                extractCallsFromMethod(
                        method, owner, unit, projectMethods, calls);
            } else if (member instanceof AbstractTypeDeclaration nestedType) {
                extractCalls(nestedType, unit, projectMethods, calls);
            }
        }
    }

    private void extractCallsFromMethod(
            MethodDeclaration method,
            ITypeBinding owner,
            CompilationUnit unit,
            Set<String> projectMethods,
            List<MethodCallInfo> calls) {

        if (method.getBody() == null) {
            return;
        }

        String caller = MethodIds.of(method, owner);

        method.getBody().accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation invocation) {
                calls.add(createCall(
                        caller,
                        invocation.getName().getIdentifier(),
                        invocation.getExpression(),
                        invocation.resolveMethodBinding(),
                        owner,
                        unit,
                        invocation,
                        false,
                        projectMethods));
                return true;
            }

            @Override
            public boolean visit(SuperMethodInvocation invocation) {
                calls.add(createCall(
                        caller,
                        invocation.getName().getIdentifier(),
                        null,
                        invocation.resolveMethodBinding(),
                        owner,
                        unit,
                        invocation,
                        true,
                        projectMethods));
                return true;
            }
        });
    }

    private MethodCallInfo createCall(
            String caller,
            String calledName,
            Expression expression,
            IMethodBinding targetBinding,
            ITypeBinding owner,
            CompilationUnit unit,
            ASTNode node,
            boolean superCall,
            Set<String> projectMethods) {

        String receiverType = receiverType(
                expression, targetBinding, owner, superCall);

        if (targetBinding == null) {
            return new MethodCallInfo(
                    caller,
                    calledName,
                    receiverType,
                    null,
                    unit.getLineNumber(node.getStartPosition()),
                    "UNRESOLVED");
        }

        String target = MethodIds.of(targetBinding);
        String resolution = projectMethods.contains(target)
                ? "INTERNAL"
                : "EXTERNAL";

        return new MethodCallInfo(
                caller,
                calledName,
                receiverType,
                target,
                unit.getLineNumber(node.getStartPosition()),
                resolution);
    }

    private String receiverType(
            Expression expression,
            IMethodBinding target,
            ITypeBinding owner,
            boolean superCall) {

        if (superCall) {
            return typeName(owner == null ? null : owner.getSuperclass());
        }

        if (expression != null) {
            return typeName(expression.resolveTypeBinding());
        }

        if (target != null && Modifier.isStatic(target.getModifiers())) {
            return typeName(target.getDeclaringClass());
        }

        return typeName(owner);
    }

    private String typeName(ITypeBinding binding) {
        if (binding == null) {
            return null;
        }
        return binding.getErasure().getQualifiedName();
    }

}
