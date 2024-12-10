package plc.project;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Optional;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;
    private boolean shouldPrintSemi = true;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        writer.println("public class Main {");
        newline(0);

        writer.println("    public static void main(String[] args) {");
        writer.println("        System.exit(new Main().main());");
        writer.println("    }");

        for (Ast.Method method : ast.getMethods()) {
            visit(method);
            newline(0);
        }

        newline(0);
        writer.print("}");
        writer.flush();
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(Environment.getType(ast.getTypeName()).getJvmName());
        print(" ", ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            print(ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        String returnType = getJavaType(ast.getReturnTypeName().orElse("Any"));
        String methodSignature = String.format("%s %s()", returnType, ast.getName());
        newline(indent);
        writer.print("    " + methodSignature + " {");

        indent++;
        indent++;
        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;
        indent--;

        newline(indent);
        writer.write("    }");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String type;
        if (!ast.getTypeName().isEmpty()) {
            type = getJavaType(ast.getTypeName().get());
        } else if (ast.getValue().isPresent()) {
            Ast.Expression exp = ast.getValue().get();
            type = exp.getType().getJvmName();
        } else {
            // Fallback to void if no type is specified
            type = "Object";
        }

        writer.write(type + " " + ast.getName());

        if (ast.getValue().isPresent()) {
            writer.write(" = ");
            visit(ast.getValue().get());
        }

        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        writer.write(" = ");
        visit(ast.getValue());
        if(shouldPrintSemi){
            writer.write(";");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        writer.write("if (");
        visit(ast.getCondition());
        writer.write(") {");

        indent++;
        for (Ast.Statement stmt : ast.getThenStatements()) {
            newline(indent);
            visit(stmt);
        }
        indent--;

        if (!ast.getElseStatements().isEmpty()) {
            newline(indent);
            writer.write("} else {");
            indent++;
            for (Ast.Statement stmt : ast.getElseStatements()) {
                newline(indent);
                visit(stmt);
            }
            indent--;
        }

        newline(indent);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        shouldPrintSemi = false;
        writer.write("for ( ");

        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        }
        writer.write("; ");

        if(ast.getCondition() != null){
            visit(ast.getCondition());
            writer.write("; ");
        }

        if (ast.getIncrement() != null) {
            visit(ast.getIncrement());
            writer.write(" ");
        }
        shouldPrintSemi = true;

        writer.write(") {");

        indent++;
        for (Ast.Statement stmt : ast.getStatements()) {
            newline(indent);
            visit(stmt);
        }
        indent--;

        newline(indent);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        writer.write("while ( ");
        visit(ast.getCondition());
        writer.write(") {");

        indent++;
        if (ast.getStatements().isEmpty()) {
            // Handle empty while loop
            newline(indent);
        } else {
            for (Ast.Statement stmt : ast.getStatements()) {
                newline(indent);
                visit(stmt);

                // Ensure assignments end with a semicolon
                if (stmt instanceof Ast.Statement.Assignment) {
                    writer.write(";");
                }
            }
        }
        indent--;

        newline(indent);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        writer.write("return ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal instanceof String) {
            writer.write("\"" + literal + "\"");
        } else {
            writer.write(literal.toString());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        writer.write("(");
        visit(ast.getExpression());
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        writer.write(" " + ast.getOperator() + " ");
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        writer.write(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());

        // Generate an opening parenthesis
        print("(");

        // Generate a comma-separated list of the argument expressions
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i < ast.getArguments().size() - 1) {
                print(", ");
            }
        }

        // Generate a closing parenthesis
        print(")");

        return null;
    }

    private String getJavaType(String type) {
        switch (type) {
            case "Integer": return "int";
            case "Decimal": return "double";
            case "String": return "String";
            case "Boolean": return "boolean";
            default: return "void";
        }
    }
}