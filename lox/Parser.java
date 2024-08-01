package craftinginterpreter.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static craftinginterpreter.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    List<Stmt> statements = new ArrayList<>();
    
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /*
    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }
    */
    
    List<Stmt> parse() {
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt.Class classDeclaration() {
        Token name = consume(IDENTIFIER, "Expected class name.");
        consume(LEFT_BRACE, "Expected '{' after class name.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expected '}' after class body.");

        return new Stmt.Class(name, methods);
    }

     private Stmt.Function function(String kind) {
        // Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        // Challenge from Functions
        Token name = new Token(FUN, "fun", null, 0);
        if (check(IDENTIFIER))
            name = advance();

        consume(LEFT_PAREN, "Expected '(' after function name.");
        List<Token> parameters = new ArrayList<>();
        if(!check(RIGHT_PAREN)) {
            do {
            if (parameters.size() >= 255) {
                error(peek(), "Can't have more than 255 parameters.");
            }
            parameters.add(consume(IDENTIFIER, "Expected parameter name."));
            } while(match(COMMA));
        }

        consume(RIGHT_PAREN, "Exptect ')' after parameters.");

        consume(LEFT_BRACE, "Exptect '{' before " + kind + " body.");
        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
    }
    
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected ';' after variable declaration.");
        return new Stmt.Var(name , initializer);
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(RETURN)) return returnStatement();
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expected '(' after for.");
        Stmt initializer;
        if (match(SEMICOLON)) initializer = null;
        else if (match(VAR)) initializer = varDeclaration();
        else initializer = expressionStatement();
        
        Expr condition = null;
        if (!check(SEMICOLON)) condition = expression();
        consume(SEMICOLON, "Expected ';' after for loop condition.");

        Expr increment = null;
        if (!check(RIGHT_BRACE)) increment = expression();
        consume(RIGHT_PAREN, "Expected ')' after for loop condition.");

        Stmt body = statement();
        if (increment != null) body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) body = new Stmt.Block(Arrays.asList(initializer, body));

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expected '(' after if.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expected ';' after return value.");
        return new Stmt.Return(keyword, value);
    }
    
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expecteed ';' at the end.");
        return new Stmt.Print(value);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expecteed '(' after while.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expecteed '>' after while condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }
    
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expected '}' after block");
        return statements;
    }
    
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expecteed ';' at the end.");
        return new Stmt.Expression(expr);
    }
    
    private Expr expression() {
        return assignment();
    }
    
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equal = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name =  ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equal, "Invalid assignment target.");
        }
        
        return expr;
    }
    
    private Expr or() {
        Expr expr = and();

        if (match(OR)) {
            Token operator = previous();
            Expr right = and();
            return new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality(); // change it equality to reduce the stress:)!.

        if (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            return new Expr.Logical(expr, operator, right);
        }

        return expr;
    }
    
    // Below method is challenge 1 implemetation of (Parsing Representation).
    private Expr comma() {
        Expr expr = ternary();

        while(match(COMMA)) {
            Token token = previous();
            Expr right = ternary();
            expr = new Expr.Binary(expr, token, right);
        }

        return expr;
    }

    // Below method is challenge 2 implementation of (Parsing Representaion)
    private Expr ternary() {
        Expr expr = equality();

        if (!match(QUESTION)) return expr;
        Expr left = ternary();
        advance();
        Expr right = equality();
        expr = new Expr.Conditional(expr, left, right);

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(STAR, SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        // Challenge from (Functions) to implement lambdas
        if (expr instanceof Expr.Variable) {
            if (((Expr.Variable)expr).name.type == FUN) {
                statements.add(function("function"));
                return expr;
            }
        }
        
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expected property name afer '.'");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        // Challenge from (Functions)
        if (match(FUN)) return new Expr.Variable(previous());

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) return new Expr.This(previous());

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        
        // Challenge 3 of (Parsing Expressions).
        if (match(PLUS, STAR, SLASH)) {
            System.err.println(previous().lexeme + 
                    " Binary operation at the start of expression.");
            return primary();
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        throw error(peek(), "Except expression");
    }
    
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expected ')' after argument list.");

        return new Expr.Call(callee, paren, arguments);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

     private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current-1);
    }
    
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }
    
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                case FUN:
                case VAR:
                    return;
            }

            advance();
        }
    }
}
