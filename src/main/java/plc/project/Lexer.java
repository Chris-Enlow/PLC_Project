package plc.project;
import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> statement = new ArrayList<>();
        while (chars.has(0)) {
            if (peek("[ \b\n\r\t]")) {
                chars.advance();
                chars.skip();
            }
            else {
                statement.add(lexToken());
            }
        }
        return statement;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        }
        else if (peek("[+-]", "[0-9]") || peek("[0-9]")){
            return lexNumber();
        }
        else if (peek("'")){
            return lexCharacter();
        }
        else if (peek("\"") || peek("\\\\", "\"")){
            return lexString();
        }
        else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        if(!peek("[A-Za-z_]")){
            throw new UnsupportedOperationException();
        }
        match("[A-Za-z_]");
        while(peek("[A-Za-z0-9_-]")){
                match("[A-Za-z0-9_-]");
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        // Track if the number starts with a negative sign
        boolean isNegative = false;

        if (peek("[+-]")) {
            isNegative = peek("-");
            match("[+-]");
        }

        if (peek("0")) {
            match("0");

            if (!peek("[0-9]")) {
                return chars.emit(Token.Type.INTEGER);
            }

            if (peek("[0-9]") && !peek("0", "\\.")) {
                throw new ParseException("Leading 0", chars.index);
            }
        }

        // Match remaining digits
        while (match("[0-9]")) ;

        // Check for decimal
        if (peek("\\.")) {
            match("\\.");
            if (peek("[0-9]")) {
                while (match("[0-9]")) ;
                return chars.emit(Token.Type.DECIMAL);
            } else {
                throw new ParseException("Missing digits after decimal", chars.index);
            }
        }
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        match("'");
        if (peek("'")){
            throw new ParseException("Missing character", chars.index);
        }
        if (peek("\\\\")){
            if (peek("\\\\", "[\\\\'\"bnrt]"))
                match("\\\\", "[\\\\'\"bnrt]");
            else {
                throw new ParseException("Incorrect Escape", chars.index);
            }
        }
        else { match("[^'\\\\\\n\\r]");}
        if (peek("'")) {
            match("'");
            return chars.emit(Token.Type.CHARACTER);
        }
        throw new ParseException("Missing closing single quote", chars.index);
    }

    public Token lexString() {
        if (peek("\"")) {
            match("\"");
        }
        if (peek("\\\\","\"")) {
            match("\\\\","\"");
        }
        while (peek(".")) {
            if (peek("\"") || peek("\\\\","\"")){
                break;
            }
            else if (peek("\\\\")) {
                if (peek("\\\\", "[\\\\'bnrt]"))
                    match("\\\\", "[\\\\'bnrt]");
                else {
                    throw new ParseException("Incorrect Escape", chars.index);
                }
            }
            else  {
                match(".");
            }
        }
        if (peek("\"")){
            match("\"");
        }
        else if (peek("\\\\","\"")) {
            match("\\\\","\"");
        }
        else {
            throw new ParseException("Missing closing double quote", chars.index);
        }
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        if (peek("\\\\")){
            chars.advance();
            chars.skip();
        }
        else {
            throw new ParseException("Incorrect Escape", chars.index);
        }
    }

    public Token lexOperator() {
        if (match("!","=") || match("=","=") || match("<","=") || match(">","=") ||
                match("\\|","\\|") || match("&","&")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        else if (match("[^\\w\\s]")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        throw new ParseException("Not a valid Operator", chars.index);
    }
    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i<patterns.length; i++){
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }
    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i=0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
