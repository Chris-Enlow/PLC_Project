package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        ast.getFields().forEach(this::visit);
        ast.getMethods().forEach(this::visit);

        requireAssignable(Environment.Type.INTEGER, scope.lookupFunction("main", 0).getReturnType());
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if (!ast.getTypeName().equals("Any")) {
                requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
            }
        }

        Environment.Type type = ast.getTypeName().equals("Any") ?
                (ast.getValue().isPresent() ? ast.getValue().get().getType() : Environment.Type.ANY) :
                Environment.getType(ast.getTypeName());

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, ast.getConstant(), Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> parameterTypes = ast.getParameterTypeNames()
                .stream()
                .map(Environment::getType)
                .collect(Collectors.toList());

        Environment.Type returnType = ast.getReturnTypeName()
                .map(Environment::getType)
                .orElse(Environment.Type.ANY);

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, returnType, args -> Environment.NIL));

        try {
            scope = new Scope(scope);

            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(
                        ast.getParameters().get(i),
                        ast.getParameters().get(i),
                        parameterTypes.get(i),
                        false,
                        Environment.NIL
                );
            }

            Ast.Method previousMethod = method;
            method = ast;
            try {
                ast.getStatements().forEach(this::visit);
            } finally {
                method = previousMethod;
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (ast.getExpression() instanceof Ast.Expression.Function) {
            visit(ast.getExpression());
        } else {
            throw new RuntimeException("Expression not a valid function.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Declaration must have a type.");
        }

        Environment.Type type = ast.getTypeName()
                .map(Environment::getType)
                .orElse(null);

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if (type == null) {
                type = ast.getValue().get().getType();
            } else {
                requireAssignable(type, ast.getValue().get().getType());
            }
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, false, Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (ast.getReceiver() instanceof Ast.Expression.Access) {
            visit(ast.getReceiver());
            visit(ast.getValue());
            requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        } else {
            throw new RuntimeException("Invalid Assignment");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then statements cannot be empty.");
        }
        scope = new Scope(scope);
        try {
            ast.getThenStatements().forEach(this::visit);
        } finally {
            scope = scope.getParent();
        }
        scope = new Scope(scope);
        try {
            ast.getElseStatements().forEach(this::visit);
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        }

        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if (ast.getIncrement() != null) {
            visit(ast.getIncrement());
        }

        try {
            scope = new Scope(scope);
            ast.getStatements().forEach(this::visit);
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            ast.getStatements().forEach(this::visit);
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        if (method == null) {
            throw new RuntimeException("Return statement must be in a method.");
        }
        visit(ast.getValue());
        requireAssignable(method.getFunction().getReturnType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal == null) {
            ast.setType(Environment.Type.NIL);
        } else if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (literal instanceof BigInteger) {
            if (((BigInteger) literal).bitLength() > 31) {
                throw new RuntimeException("Integer is too large: " + literal);
            }
            ast.setType(Environment.Type.INTEGER);
        } else if (literal instanceof BigDecimal) {
            if (!Double.isFinite(((BigDecimal) literal).doubleValue())) {
                throw new RuntimeException("Decimal is too large: " + literal);
            }
            ast.setType(Environment.Type.DECIMAL);
        } else if (literal instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else {
            throw new AssertionError(literal.getClass().getName());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (ast.getExpression() instanceof Ast.Expression.Binary) {
            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());
        } else {
            throw new RuntimeException("Group must contain binary expression.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        String op = ast.getOperator();
        if (op.equals("&&") || op.equals("||")) {
            requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        } else if ("< > <= >= == !=".contains(op)) {
            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        } else if (op.equals("+") && (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING))) {
            ast.setType(Environment.Type.STRING);
        } else {
            Environment.Type left = ast.getLeft().getType();
            Environment.Type right = ast.getRight().getType();
            requireAssignable(left.equals(Environment.Type.DECIMAL) ? Environment.Type.DECIMAL : Environment.Type.INTEGER, right);
            ast.setType(left);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            Environment.Variable fieldVar = new Environment.Variable(
                    ast.getName(),
                    ast.getName(),
                    Environment.Type.INTEGER,
                    false,
                    Environment.NIL
            );

            ast.setVariable(fieldVar);
        } else {
            try {
                Environment.Variable variable = scope.lookupVariable(ast.getName());
                ast.setVariable(variable);
            } catch (RuntimeException e) {
                throw new RuntimeException("Undefined variable: " + ast.getName());
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            List<Environment.Type> parameterTypes = new ArrayList<>();
            parameterTypes.add(Environment.Type.ANY);
            Environment.Function function = new Environment.Function(
                    ast.getName(),
                    ast.getName(),
                    parameterTypes,
                    Environment.Type.INTEGER,
                    args -> Environment.NIL
            );

            ast.setFunction(function);
        } else {
            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        }
        ast.getArguments().forEach(this::visit);
        for (int i = 0; i < ast.getArguments().size(); i++) {
            requireAssignable(
                    ast.getFunction().getParameterTypes().get(i),
                    ast.getArguments().get(i).getType()
            );
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (type.equals(target) ||
                target.equals(Environment.Type.ANY) ||
                (target.equals(Environment.Type.COMPARABLE) && (
                        type.equals(Environment.Type.INTEGER) ||
                                type.equals(Environment.Type.DECIMAL) ||
                                type.equals(Environment.Type.CHARACTER) ||
                                type.equals(Environment.Type.STRING)))) {
            return;
        }
        throw new RuntimeException("Type " + type + " is not assignable to " + target);
    }
}