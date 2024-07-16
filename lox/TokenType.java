package craftinginterpreter.lox;

enum TokenType {
    // Sing-character token
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // One or two character token
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, CLASS, ELSE, FOR, WHILE, THIS, SUPER, IF, NIL,
    OR, PRINT, RETURN, VAR, FALSE, TRUE, FUN,

    EOF
}
