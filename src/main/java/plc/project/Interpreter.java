package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for(Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for(Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        ArrayList<Environment.PlcObject> args = new ArrayList<Environment.PlcObject>();
        return scope.lookupFunction("main", 0).invoke(args);
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject val = Environment.NIL;
        if (ast.getValue().isPresent()) {
            val = visit(ast.getValue().get());
        }
        scope.defineVariable(ast.getName(), ast.getValue().isPresent(), val);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope functionScope = new Scope(scope); // New scope for function parameters
            try {
                // Set parameters in the new scope
                for (int i = 0; i < args.size(); i++) {
                    functionScope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                }
                // Set the scope to functionScope while executing the function body
                scope = functionScope;
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement); // Visit each statement in the function
                }
            } catch (Return returnValue) {
                return returnValue.value; // Handle return statements
            } finally {
                scope = scope.getParent(); // Restore the parent scope afterward
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject value = ast.getValue().map(this::visit).orElse(Environment.NIL);

        // Define the variable in the current scope with the evaluated value
        scope.defineVariable(ast.getName(), true, value);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        Environment.PlcObject newValue = visit(ast.getValue()); // Evaluate the new value

        if (receiver instanceof Ast.Expression.Access) {
            Ast.Expression.Access access = (Ast.Expression.Access) receiver;

            if (access.getReceiver().isPresent()) {
                // If there's a receiver, handle it as a field or property access
                visit(access.getReceiver().get()).setField(access.getName(), newValue);
            } else {
                // Otherwise, it's a variable in the scope chain
                scope.lookupVariable(access.getName()).setValue(newValue);
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Environment.PlcObject condition = visit(ast.getCondition());
        requireType(Boolean.class, condition);

        // Create a new scope to handle the local variables within the if-else statements
        scope = new Scope(scope);
        try {
            if (condition.getValue().equals(Boolean.TRUE)) {
                ast.getThenStatements().forEach(this::visit);
            } else {
                ast.getElseStatements().forEach(this::visit);
            }
        } finally {
            // Ensure the scope returns to the parent after the if-else block
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        visit(ast.getInitialization());

        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            // Create a new loop scope for each iteration to prevent variable leakage
            Scope loopScope = new Scope(scope);
            scope = loopScope;
            try {
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } finally {
                // Return to the parent scope after each loop iteration
                scope = scope.getParent();
            }

            visit(ast.getIncrement());
        }

        return Environment.NIL;
 }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            // Create a new scope for the loop body to isolate its variables
            scope = new Scope(scope);
            try {
                ast.getStatements().forEach(this::visit);
            } finally {
                // Ensure the scope returns to the parent after each iteration
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Interpreter.Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        } else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());

        switch (ast.getOperator()) {
            case "&&":
                if (!requireType(Boolean.class, left)) {
                    return Environment.create(false);
                }
                return Environment.create(requireType(Boolean.class, visit(ast.getRight())));

            case "||":
                if (requireType(Boolean.class, left)) {
                    return Environment.create(true);
                }
                return Environment.create(requireType(Boolean.class, visit(ast.getRight())));

            case "<":
                return Environment.create(requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), visit(ast.getRight()))) < 0);
            case ">":
                return Environment.create(requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), visit(ast.getRight()))) > 0);
            case "<=":
                return Environment.create(requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), visit(ast.getRight()))) <= 0);
            case ">=":
                return Environment.create(requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), visit(ast.getRight()))) >= 0);
            case "==":
                return Environment.create(left.getValue().equals(visit(ast.getRight()).getValue()));
            case "!=":
                return Environment.create(!left.getValue().equals(visit(ast.getRight()).getValue()));
            case "+":
                Environment.PlcObject right = visit(ast.getRight());
                if (left.getValue() instanceof String || right.getValue() instanceof String) {
                    return Environment.create("" + left.getValue() + right.getValue());
                } else if (left.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).add(requireType(BigInteger.class, right)));
                } else if (left.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).add(requireType(BigDecimal.class, right)));
                } else {
                    throw new RuntimeException("Left side invalid.");
                }
            case "-":
                if (left.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).subtract(requireType(BigInteger.class, visit(ast.getRight()))));
                } else if (left.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).subtract(requireType(BigDecimal.class, visit(ast.getRight()))));
                } else {
                    throw new RuntimeException("Left side invalid.");
                }
            case "*":
                if (left.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).multiply(requireType(BigInteger.class, visit(ast.getRight()))));
                } else if (left.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).multiply(requireType(BigDecimal.class, visit(ast.getRight()))));
                } else {
                    throw new RuntimeException("Left side invalid.");
                }
            case "/":
                if (left.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).divide(requireType(BigInteger.class, visit(ast.getRight()))));
                } else if (left.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).divide(requireType(BigDecimal.class, visit(ast.getRight())), RoundingMode.HALF_EVEN));
                } else {
                    throw new RuntimeException("Left side invalid.");
                }
            case "^":
                BigInteger exponent = requireType(BigInteger.class, visit(ast.getRight()));
                if (left.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).pow(exponent.intValue()));
                } else if (left.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).pow(exponent.intValue()));
                } else {
                    throw new RuntimeException("Left side invalid.");
                }
            default:
                throw new AssertionError("Invalid operator: " + ast.getOperator());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        } else {
            // Lookup the variable in the current scope; this ensures we access the nearest scope-defined variable
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> args = new ArrayList<>();
        for (Ast.Expression argument : ast.getArguments()) {
            args.add(visit(argument));
        }

        if (!ast.getReceiver().isPresent()) {
            Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            return function.invoke(args);
        } else {
            Environment.PlcObject o = visit(ast.getReceiver().get());
            return o.callMethod(ast.getName(), args);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
