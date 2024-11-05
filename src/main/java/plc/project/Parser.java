package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        try {
            List<Ast.Field> fields = new ArrayList<>();
            List<Ast.Method> methods = new ArrayList<>();
            while (tokens.has(0)) {
                if (match("LET")) {
                    fields.add(parseField());
                } else if (match("DEF")) {
                    methods.add(parseMethod());
                }
            }
            return new Ast.Source(fields, methods);
        } catch (ParseException ex) {
            throw new ParseException(ex.getMessage(), ex.getIndex());
        }
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        // Expect the 'LET' keyword
        boolean constValue = false;
        if(peek("CONST")){
            constValue = true;
            match("CONST");
        }
        if (!match(Token.Type.IDENTIFIER)) {
            throw error("Expected identifier.");
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> temp = Optional.empty();
        if (match("=")) {
            temp = Optional.of(parseExpression());
        }
        if (!match(";")) {
            throw error("Expected semicolon.");
        }
        return new Ast.Statement.Field(name, constValue, temp);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (!match(Token.Type.IDENTIFIER)) {
            throw error("Expected identifier.");
        }
        String name = tokens.get(-1).getLiteral();

        // Parsing parameters
        List<String> parameters = new ArrayList<>();
        if (!match("(")) {
            throw error("Expected opening parenthesis `(`.");
        }
        if (!peek(")")) {
            do {
                if (!match(Token.Type.IDENTIFIER)) {
                    throw error("Expected parameter name.");
                }
                parameters.add(tokens.get(-1).getLiteral());
            } while (match(","));
        }
        if (!match(")")) {
            throw error("Expected closing parenthesis `)`.");
        }

        // Parsing the method body
        if (!match("DO")) {
            throw error("Expected DO.");
        }
        List<Ast.Statement> statements = new ArrayList<>();
        while (!match("END") && tokens.has(0)) {
            statements.add(parseStatement());
        }
        if (!tokens.get(-1).getLiteral().equals("END")) {
            throw error("Missing END.");
        }

        return new Ast.Method(name, parameters, statements);    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("FOR")) {
            return parseForStatement();
        } else if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else {
            Ast.Expression ex = parseExpression();
            Ast.Statement statement;
            if (match("=")) {
                Ast.Expression temp = parseExpression();
                statement = new Ast.Statement.Assignment(ex, temp);
            } else {
                statement =  new Ast.Statement.Expression(ex);
            }
            if (!match(";")) {
                throw error("Expected semicolon `;`");
            }
            return statement;
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        if (!match(Token.Type.IDENTIFIER)) {
            throw error("Expected identifier.");
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> temp = Optional.empty();
        if (match("=")) {
            temp = Optional.of(parseExpression());
        }
        if (!match(";")) {
            throw error("Expected semicolon.");
        }
        return new Ast.Statement.Declaration(name, temp);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");
        Ast.Expression condition = parseExpression();
        if (match("DO")) {
            List<Ast.Statement> thens = new ArrayList<>();
            List<Ast.Statement> elses = new ArrayList<>();
            boolean isElse = false;

            while (!match("END") && tokens.has(0)) {
                if (match("ELSE")) {
                    isElse = true;
                }
                if (isElse) {
                    elses.add(parseStatement());
                } else {
                    thens.add(parseStatement());
                }
            }

            if (!tokens.get(-1).getLiteral().equals("END")) {
                throw new ParseException("Missing END", tokens.get(-1).getIndex());
            }

            return new Ast.Statement.If(condition, thens, elses);
        }
        throw error("Expected DO");
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        Ast.Statement stmt1;
        Ast.Statement stmt2;
        match("(");
        stmt1 = parseStatement();
        if (!match(";")){
            throw error("Expected semicolon.");
        }
        Ast.Expression condition = parseExpression();
        if (!match(";")){
            throw error("Expected semicolon.");
        }
        stmt2 = parseStatement();
        if (!match(";")){
            throw error("Expected semicolon.");
        }

        List<Ast.Statement> statements = new ArrayList<>();
        while (!match("END") && tokens.has(0)) {
            statements.add(parseStatement());
        }

        if (!tokens.get(-1).getLiteral().equals("END")) {
            throw error("Expected END.");
        }
        return new Ast.Statement.For(stmt1, condition, stmt2, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expression condition = parseExpression();
        if (!match("DO")) {
            throw error("Expected DO.");
        }

        List<Ast.Statement> statements = new ArrayList<>();
        while (!match("END") && tokens.has(0)) {
            statements.add(parseStatement());
        }

        if (!tokens.get(-1).getLiteral().equals("END")) {
            throw error("Expected END.");
        }

        return new Ast.Statement.While(condition, statements);    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expression temp = parseExpression();
        if (!match(";")) {
            throw error("Expected semicolon.");
        }
        return new Ast.Statement.Return(temp);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the logical-expression rule, which handles "&&" and "||" operators.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression first = parseComparisonExpression();
        while (match("&&") || match("||")) {
            String type = tokens.get(-1).getLiteral();
            Ast.Expression second = parseComparisonExpression();
            first = new Ast.Expression.Binary(type, first, second);
        }
        return first;
    }

    /**
     * Parses the comparison-expression rule, which handles comparison operators like "<", ">", "==", "!=".
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression first = parseAdditiveExpression();
        while (match("<") || match(">") || match("==") || match("!=")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression second = parseAdditiveExpression();
            first = new Ast.Expression.Binary(operator, first, second);
        }
        return first;
    }

    /**
     * Parses the additive-expression rule, which handles "+" and "-" operators.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression first= parseMultiplicativeExpression();
        while (match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression second = parseMultiplicativeExpression();
            first= new Ast.Expression.Binary(operator, first, second);
        }
        return first;
    }

    /**
     * Parses the multiplicative-expression rule, which handles "*", "/", and "^" operators.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression first= parseSecondaryExpression();
        while (match("*") || match("/") || match("^")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression second = parseSecondaryExpression();
            first= new Ast.Expression.Binary(operator, first, second);
        }
        return first;
    }
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression first= parsePrimaryExpression();
        // Loop to handle repeated field accesses or function calls
        while (match(".")) {
            if (!match(Token.Type.IDENTIFIER)) {
                throw error("Expected identifier after `.`.");
            }
            String identifier = tokens.get(-1).getLiteral();
            if (match("(")) {
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(")")) {
                    do {
                        arguments.add(parseExpression());
                    } while (match(","));
                }
                if (!match(")")) {
                    throw error("Expected closing parenthesis ')'");
                }
                first= new Ast.Expression.Function(Optional.of(first), identifier, arguments);
            } else {
                // It's a field access
                first= new Ast.Expression.Access(Optional.of(first), identifier);
            }
        }

        return first;
    }


    /**
     * Parses the primary-expression rule, handling literals, variables, and function calls.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        } else if (match("TRUE") || match("FALSE")) {
            return new Ast.Expression.Literal(Boolean.valueOf(tokens.get(-1).getLiteral().toLowerCase()));
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            String literal = tokens.get(-1).getLiteral();
            return new Ast.Expression.Literal(unescape(literal.substring(1, literal.length() - 1)).charAt(0));
        } else if (match(Token.Type.STRING)) {
            String literal = tokens.get(-1).getLiteral();
            return new Ast.Expression.Literal(unescape(literal.substring(1, literal.length() - 1)));
        } else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();

            // Handle function calls
            if (match("(")) {
                List<Ast.Expression> arguments = new ArrayList<>();
                while (!match(")")) {
                    arguments.add(parseExpression());
                    if (!peek(")")) {
                        if (!match(",")) {
                            throw error("Expected comma `,` or closing parenthesis `)`.");
                        } else if (peek(")")) {
                            throw error("Trailing comma `,` is not allowed.");
                        }
                    }
                }
                return new Ast.Expression.Function(Optional.empty(), name, arguments);

                // Handle array access (which was already there)
            } else if (match("[")) {
                Ast.Expression first= parseExpression();
                if (!match("]")) {
                    throw error("Expected closing bracket `]`.");
                }
                return new Ast.Expression.Access(Optional.of(first), name);
            } else {
                return new Ast.Expression.Access(Optional.empty(), name);
            }
        } else if (match("(")) {
            Ast.Expression first= parseExpression();
            if (!match(")")) {
                throw error("Expected closing parenthesis `)`.");
            }
            return new Ast.Expression.Group(first);
        } else {
            throw error("Invalid expression.");
        }
    }

    /**
     * Helper method to throw a ParseException with a given error message.
     */
    private ParseException error(String message) {
            return new ParseException(message, tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }

    /**
     * Unescapes a string or character literal.
     */
    private String unescape(String literal) {
        return literal
                .replace("\\b", "\b")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\'", "\'")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: "
                        + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++)
                tokens.advance();
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
