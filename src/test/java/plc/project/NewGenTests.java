package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.swing.text.html.Option;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class NewGenTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Hello, World!",
                        // FUN main(): Integer DO
                        //     print("Hello, World!");
                        //     RETURN 0;
                        // END
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Statement.Expression(init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Multiple Fields & Methods",
                        // LET x: Integer;
                        // LET y: Decimal;
                        // LET z: String;
                        // DEF f(): Integer DO RETURN x; END
                        // DEF g(): Decimal DO RETURN y; END
                        // DEF h(): String DO RETURN z; END
                        // DEF main(): Integer DO END
                        new Ast.Source(
                                Arrays.asList(
                                        init(new Ast.Field("x", true, Optional.empty()),
                                                ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, true, Environment.NIL))),
                                        init(new Ast.Field("y", true, Optional.empty()),
                                                ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, true, Environment.NIL))),
                                        init(new Ast.Field("z", true, Optional.empty()),
                                                ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, true, Environment.NIL)))
                                ),
                                Arrays.asList(
                                        init(new Ast.Method("f", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(
                                                        init(new Ast.Expression.Access(Optional.empty(), "x"),
                                                                ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, true, Environment.NIL)))))
                                        ), ast -> ast.setFunction(new Environment.Function("f", "f", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))),
                                        init(new Ast.Method("g", Arrays.asList(), Arrays.asList(), Optional.of("Decimal"), Arrays.asList(
                                                new Ast.Statement.Return(
                                                        init(new Ast.Expression.Access(Optional.empty(), "y"),
                                                                ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, true, Environment.NIL)))))
                                        ), ast -> ast.setFunction(new Environment.Function("g", "g", Arrays.asList(), Environment.Type.DECIMAL, args -> Environment.NIL))),
                                        init(new Ast.Method("h", Arrays.asList(), Arrays.asList(), Optional.of("String"), Arrays.asList(
                                                new Ast.Statement.Return(
                                                        init(new Ast.Expression.Access(Optional.empty(), "z"),
                                                                ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, true, Environment.NIL)))))
                                        ), ast -> ast.setFunction(new Environment.Function("h", "h", Arrays.asList(), Environment.Type.STRING, args -> Environment.NIL))),
                                        init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList()),
                                                ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    int x;",
                                "    double y;",
                                "    String z;",
                                "",
                                "    int f() {",
                                "        return x;",
                                "    }",
                                "",
                                "    double g() {",
                                "        return y;",
                                "    }",
                                "",
                                "    String h() {",
                                "        return z;",
                                "    }",
                                "",
                                "    int main() {",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethod(String test, Ast.Method ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Square",
                        // DEF square(num: Decimal): Decimal DO
                        //     RETURN num * num;
                        // END
                        init(new Ast.Method(
                                "square",
                                Arrays.asList("num"), // Parameter names
                                Arrays.asList("Decimal"), // Parameter type names
                                Optional.of("Decimal"), // Return type
                                Arrays.asList(
                                        new Ast.Statement.Return(
                                                new Ast.Expression.Binary("*",
                                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                                        new Ast.Expression.Access(Optional.empty(), "num")
                                                )
                                        )
                                )
                        ), ast -> ast.setFunction(new Environment.Function(
                                "square", "square", Arrays.asList(Environment.Type.DECIMAL), Environment.Type.DECIMAL, args -> Environment.NIL))),
                        String.join(System.lineSeparator(),
                                "double square(double num) {",
                                "    return num * num;",
                                "}"
                        )
                ),
                Arguments.of("Multiple Statements",
                        // DEF func(x: Integer, y: Decimal, z: String) DO
                        //     print(x);
                        //     print(y);
                        //     print(z);
                        // END
                        init(new Ast.Method(
                                "func",
                                Arrays.asList("x", "y", "z"), // Parameter names
                                Arrays.asList("Integer", "Decimal", "String"), // Parameter type names
                                Optional.empty(), // No return type
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                        init(new Ast.Expression.Access(Optional.empty(), "x"),
                                                                ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, false, Environment.NIL)))
                                                )), func -> func.setFunction(
                                                        new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)
                                                ))
                                        ),
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                        init(new Ast.Expression.Access(Optional.empty(), "y"),
                                                                ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, false, Environment.NIL)))
                                                )), func -> func.setFunction(
                                                        new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)
                                                ))
                                        ),
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                        init(new Ast.Expression.Access(Optional.empty(), "z"),
                                                                ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, false, Environment.NIL)))
                                                )), func -> func.setFunction(
                                                        new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)
                                                ))
                                        )
                                )
                        ), ast -> ast.setFunction(new Environment.Function(
                                "func", "func", Arrays.asList(Environment.Type.INTEGER, Environment.Type.DECIMAL, Environment.Type.STRING), Environment.Type.NIL, args -> Environment.NIL))),
                        String.join(System.lineSeparator(),
                                "void func(int x, double y, String z) {",
                                "    System.out.println(x);",
                                "    System.out.println(y);",
                                "    System.out.println(z);",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentStatement(String test, Ast.Statement.Assignment ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Variable Assignment",
                        // variable = 1;
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "variable"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        "variable = 1;"
                ),
                Arguments.of("Field Assignment",
                        // object.field = 1;
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "field"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        "object.field = 1;"
                )
        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL))),
                        "int name;"
                ),
                Arguments.of("Initialization",
                        // LET name = 1.0;
                        init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, true, Environment.NIL))),
                        "double name = 1.0;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        // IF expr DO
                        //     stmt;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt"), ast -> ast.setVariable(new Environment.Variable("stmt", "stmt", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt;",
                                "}"
                        )
                ),
                Arguments.of("Else",
                        // IF expr DO
                        //     stmt1;
                        // ELSE
                        //     stmt2;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt1"), ast -> ast.setVariable(new Environment.Variable("stmt1", "stmt1", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt2"), ast -> ast.setVariable(new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true, Environment.NIL)))))
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt1;",
                                "} else {",
                                "    stmt2;",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testForStatement(String test, Ast.Statement.For ast, String expected) {
        test(ast, expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhileStatement(String test, Ast.Statement.While ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("Empty Statements",
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "cond"),
                                Arrays.asList()
                        ),
                        "while (cond) {}" // Updated expected output
                ),
                Arguments.of("Multiple Statements",
                        // WHILE num < 10 DO
                        //     print(num + "\n");
                        //     num = num + 1;
                        // END
                        new Ast.Statement.While(
                                init(new Ast.Expression.Binary("<",
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Literal(BigInteger.TEN),
                                                ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                        init(new Ast.Expression.Binary("+",
                                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                                init(new Ast.Expression.Literal("\n"),
                                                                        ast -> ast.setType(Environment.Type.STRING))
                                                        ), ast -> ast.setType(Environment.Type.STRING))
                                                )), func -> func.setFunction(
                                                        new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)
                                                ))
                                        ),
                                        new Ast.Statement.Assignment(
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Binary("+",
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                        init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                ast -> ast.setType(Environment.Type.INTEGER)
                                                        )
                                                ), ast -> ast.setType(Environment.Type.INTEGER))
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "while (num < 10) {",
                                "    System.out.println(num + \"\\n\");",
                                "    num = num + 1;",
                                "}"
                        )
                )


        );
    }


    private static Stream<Arguments> testForStatement() {
        return Stream.of(
                Arguments.of("For",
                        // for (num = 0; num < 5; num = num + 1)
                        //     print(num);
                        // END
                        new Ast.Statement.For(
                                new Ast.Statement.Assignment(
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Literal(BigInteger.valueOf(0)),
                                                ast -> ast.setType(Environment.Type.INTEGER))),
                                init(new Ast.Expression.Binary("<",
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                                        ast -> ast.setType(Environment.Type.INTEGER))),
                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
                                new Ast.Statement.Assignment(
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Binary("+",
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                        init(new Ast.Expression.Literal(BigInteger.valueOf(1)),
                                                                ast -> ast.setType(Environment.Type.INTEGER))),
                                                ast -> ast.setType(Environment.Type.INTEGER))),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))))),
                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "for ( num = 0; num < 5; num = num + 1 ) {",
                                "    System.out.println(num);",
                                "}"
                        )
                ),
                Arguments.of("Missing Signature",
                        // for (; num < 5;)
                        //     print(num);
                        // END
                        new Ast.Statement.For(
                                null,
                                init(new Ast.Expression.Binary("<",
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                                        ast -> ast.setType(Environment.Type.INTEGER))),
                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
                                null,
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))))),
                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Statement.Assignment(
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Binary("+",
                                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                                init(new Ast.Expression.Literal(BigInteger.valueOf(1)),
                                                                        ast -> ast.setType(Environment.Type.INTEGER))),
                                                        ast -> ast.setType(Environment.Type.INTEGER)
                                                )
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "for ( ; num < 5; ) {",
                                "    System.out.println(num);",
                                "    num = num + 1;",
                                "}"
                        )
                )
        );
    }

    // for (; num < 5;)
    //     print(num);
    //     num = num + 1;
    // END

//                "for ( num = 0; num < 5; num = num + 1 ) {",
//        "    System.out.println(num);",
//                "END"


    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, Ast.Expression.Binary ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        // TRUE && FALSE
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Literal(false)
                        ),
                        "true && false"
                ),
                Arguments.of("Concatenation",
                        // "Ben " + 10
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("Ben "),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        "\"Ben \" + 10"
                ),
                Arguments.of("Comparison",
                        // 1 > 10
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        "1 > 10"
                ),
                Arguments.of("Addition",
                        // 1 + 10
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        "1 + 10"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroupExpression(String test, Ast.Expression.Group ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Group Binary",
                        // (1 + 10)
                        init(new Ast.Expression.Group(
                                init(new Ast.Expression.Binary("+",
                                        init(new Ast.Expression.Literal(BigInteger.ONE), literal -> literal.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expression.Literal(BigInteger.TEN), literal -> literal.setType(Environment.Type.INTEGER))
                                ), group -> group.setType(Environment.Type.INTEGER))
                        ), group -> group.setType(Environment.Type.INTEGER)),
                        "(1 + 10)"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, Ast.Expression.Function ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Print",
                        // print("Hello, World!")
                        init(new Ast.Expression.Function(
                                Optional.empty(), // Receiver
                                "print",          // Name
                                Arrays.asList(
                                        init(new Ast.Expression.Literal("Hello, World!"), literal -> literal.setType(Environment.Type.STRING))
                                )
                        ), func -> func.setFunction(
                                new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)
                        )),
                        "System.out.println(\"Hello, World!\")"
                ),
                Arguments.of("Zero Arguments",
                        // function()
                        init(new Ast.Expression.Function(
                                Optional.empty(), // Receiver
                                "function",        // Name
                                Arrays.asList()    // Arguments
                        ), func -> func.setFunction(
                                new Environment.Function("function", "function", Arrays.asList(), Environment.Type.ANY, args -> Environment.NIL)
                        )),
                        "function()"
                ),
                Arguments.of("String Slice",
                        // "string".slice(1, 5)
                        init(new Ast.Expression.Function(
                                Optional.of(
                                        init(new Ast.Expression.Literal("string"), literal -> literal.setType(Environment.Type.STRING))
                                ), // Receiver
                                "slice",         // Name
                                Arrays.asList(
                                        init(new Ast.Expression.Literal(BigInteger.ONE), literal -> literal.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expression.Literal(BigInteger.valueOf(5)), literal -> literal.setType(Environment.Type.INTEGER))
                                )
                        ), func -> func.setFunction(
                                new Environment.Function("slice", "substring", Arrays.asList(Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.STRING, args -> Environment.NIL)
                        )),
                        "\"string\".substring(1, 5)"
                )
        );
    }



    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static void test(Ast ast, String expected) {
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
