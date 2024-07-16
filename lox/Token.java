package craftinginterpreter.lox;

import static craftinginterpreter.lox.TokenType.*;

class Token {
    final TokenType type;
    final String lexeme;
    final Object literal; // Using Object allows flexibility in what kind of value can be stored
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
