package lexer;

import java.util.*;


/**
 *  The Lexer class is responsible for scanning the source file
 *  which is a stream of characters and returning a stream of 
 *  tokens; each token object will contain the string (or access
 *  to the string) that describes the token along with an
 *  indication of its location in the source program to be used
 *  for error reporting; we are tracking line numbers; white spaces
 *  are space, tab, newlines
*/
public class Lexer {
    private boolean atEOF = false;
    private char ch;     // next character to process
    private static SourceReader source;
    private final static Queue<Token> sourceList = new LinkedList<>(); // stores tokens
    // positions in line of current token
    private int startPosition, endPosition, lineNum; 
    boolean flag;

    public Lexer(String sourceFile) throws Exception {
        new TokenType();  // init token table
        source = new SourceReader(sourceFile);
        ch = source.read();
    }


    public static void main(String args[]) {
        Token tok;
        Scanner input = new Scanner(System.in);
        System.out.println("Enter filename: ");
        String fn = input.nextLine();
        try {
            Lexer lex = new Lexer(fn);
            while (true) {
                tok = lex.nextToken();
                sourceList.add(tok);
                String p = TokenType.tokens.get(tok.getKind()) + "";
                if ((tok.getKind() == Tokens.Identifier) ||
                    (tok.getKind() == Tokens.INTeger) ||
                    (tok.getKind() == Tokens.Float) ||
                    (tok.getKind() == Tokens.ScientificN) ||
                    (tok.getKind() == Tokens.Char)) {
                    p += tok.toString();
                }
                p +="\t" + "L: " + tok.getLeftPosition() +
                   " R: " + tok.getRightPosition() + "  " +
                   " line: " + tok.getLineNum();                
            }
        } catch (Exception e) {
          System.out.println("\n");      
        }
    }
   
 
/**
 *  newIdTokens are either ids or reserved words; new id's will be inserted
 *  in the symbol table with an indication that they are id's
 *  @param id is the String just scanned - it's either an id or reserved word
 *  @param startPosition is the column in the source file where the token begins
 *  @param endPosition is the column in the source file where the token ends
 *  @param lineNum is the line number in the source file where the token is located
 *  @return the Token; either an id or one for the reserved words
*/
    public Token newIdToken(String id,int startPosition,int endPosition, int lineNum) {
        return new Token(startPosition,endPosition,lineNum,Symbol.symbol(id,Tokens.Identifier));
    }

/**
 *  number tokens are inserted in the symbol table; we don't convert the 
 *  numeric strings to numbers until we load the bytecodes for interpreting;
 *  this ensures that any machine numeric dependencies are deferred
 *  until we actually run the program; i.e. the numeric constraints of the
 *  hardware used to compile the source program are not used
 *  @param number is the int String just scanned
 *  @param startPosition is the column in the source file where the intr begins
 *  @param endPosition is the column in the source file where the int ends
 *  @param lineNum is the line number in the source where the int is located
 *  @return the number Token
*/
    public Token newNumberToken(String number,int startPosition,int endPosition, int lineNum) {
            return new Token(startPosition,endPosition,lineNum,
                Symbol.symbol(number, Tokens.INTeger));
    }
    
    public Token newFloatToken(String number, int startPosition, int endPosition, int lineNum) {
            return new Token(startPosition,endPosition,lineNum,
                Symbol.symbol(number,Tokens.Float));        
    }
    
    public Token newSciNumToken(String number, int startPosition, int endPosition, int lineNum) {
        return new Token(startPosition,endPosition,lineNum,
                Symbol.symbol(number, Tokens.ScientificN));
    }
    
    public Token newCharToken(String ch, int startPosition, int endPosition, int lineNum) {
        return new Token(startPosition,endPosition,lineNum,
                Symbol.symbol(ch, Tokens.Char));
    }
    
    public Token newStrToken(String str, int startPosition, int endPosition, int lineNum) {
        return new Token(startPosition,endPosition,lineNum,
                Symbol.symbol(str, Tokens.String));
    }

/**
 *  build the token for operators (+ -) or separators (parens, braces)
 *  filter out comments which begin with two slashes
 *  @param s is the String representing the token
 *  @param startPosition is the column in the source file where the token begins
 *  @param endPosition is the column in the source file where the token ends
 *  @param lineNum is the line number in the source file where the token is located
 *  @return the Token just found
*/
    public Token makeToken(String s,int startPosition,int endPosition, int lineNum) {
        if (s.equals("//")) {  // filter comment
            try {
               int oldLine = source.getLineno();
               do {
                   ch = source.read();
               } while (oldLine == source.getLineno());
            } catch (Exception e) {
                    atEOF = true;
            }
            return nextToken();
        }
        Symbol sym = Symbol.symbol(s,Tokens.BogusToken); // be sure it's a valid token
        if (sym == null) {
             System.out.println("******** illegal character: " + s);
             atEOF = true;
             return nextToken();
        }
        return new Token(startPosition,endPosition,lineNum,sym);
        }

/**
 *  @return the next Token found in the source file
*/
    
    public Token nextToken() { // ch is always the next char to process
        if (atEOF) {
            if (source != null) {
                source.close();
                source = null;
            }
            return null;
        }
        try {
            while (Character.isWhitespace(ch)) {  // scan past whitespace
                ch = source.read();
            }
        } catch (Exception e) {
            atEOF = true;
            return nextToken();
        }
        startPosition = source.getPosition();
        endPosition = startPosition - 1;
        lineNum = source.getLineno();

        if (Character.isJavaIdentifierStart(ch)) {
            // return tokens for ids and reserved words
            String id = "";
            try {
                do {
                    endPosition++;
                    id += ch;
                    ch = source.read();
                } while (Character.isJavaIdentifierPart(ch));
            } catch (Exception e) {
                atEOF = true;
            }
            return newIdToken(id,startPosition,endPosition,lineNum);
        }
        if (Character.isDigit(ch)) {
            // return number tokens checking for floats, scientificN, and digits
            boolean fl = false, sciNum = false, digit = false;
            String number = "";
            char tempCh = ch;
            try {
                do {
                    endPosition++;
                    number += ch;
                    ch = source.read();
                    if (ch == '.' && fl == false) {
                        endPosition++;
                        number += ch;
                        ch = source.read();
                        fl = true; // float exists
                    }
                    if (ch == 'e' || ch == 'E') {
                        endPosition++;
                        ch = source.read();
                        if (ch == '+' || ch == '-' || (Character.isDigit(ch))) {
                            number += tempCh;
                            sciNum = true; // float with scientificN exists
                        } else if (ch != 'e' || ch != 'E') {
                            System.out.println("Digit error on line " + lineNum);
                            atEOF = true;
                            return nextToken();
                        }
                    }
                } while (Character.isDigit(ch) || ch == '+' || ch == '-');
            } catch (Exception e) {
                atEOF = true;
            }
            // check for scientific notation if r and +- operators followed by a digit
            // if character after r is not an +- or digit, then treat as separate tokens
            if (fl == true) {
                return newFloatToken(number,startPosition,endPosition,lineNum); // returns a float
            } else if (sciNum == true) {
                return newSciNumToken(number,startPosition,endPosition,lineNum); // returns scientificN
            } else {
            return newNumberToken(number,startPosition,endPosition,lineNum); // if no '.', then returns digit
            }
        }
        if (ch == '.') { // check for digit after '.'
            String number = ""; 
            boolean isFloat = false;
            try {
                do {
                    endPosition++;
                    number += ch;
                    ch = source.read();
                    if ((ch == 'e' || ch == 'E' || ch == '.' || ch == ' ') && isFloat == false) {
                        System.out.println("Float error on line: " + lineNum);
                        atEOF = true;
                        return nextToken();
                    } else {
                        isFloat = true; // float exists
                    }
                } while (Character.isDigit(ch));
            } catch (Exception e) {
                atEOF = true;
            }
            return newFloatToken(number,startPosition,endPosition,lineNum);
        }
        if (ch == '\'') { // checks for char token
            String tempChar = "";
            boolean isChar = false;
            try {
                do {
                    ch = source.read();
                    if (ch != '\'' && isChar == false) {
                        endPosition++;
                        tempChar += ch;
                        isChar = true; // char exists
                    } else if (ch == ' ') {
                        System.out.println("Missing single quote error on line: " + lineNum);
                        atEOF = true;
                        return nextToken();
                    } else if (isChar == true && ch != '\'' && ch != ' ') {
                        System.out.println("Multiple char error on line: " + lineNum);
                        atEOF = true;
                        return nextToken();
                    }
                } while (ch != '\'');
            } catch (Exception e) {
                atEOF = true;
            }
            ch = ' ';
            return newCharToken(tempChar,startPosition,endPosition,lineNum);
        }
        if (ch == '\"') { // checks for string token
            String str = "";
            boolean isStr = false;
            try {
                do {
                    ch = source.read();
                    if (ch != '\"' && isStr == false) {
                        endPosition++;
                        str += ch;
                        isStr = true; // string exists
                    } else if (ch == ' ') {
                        System.out.println("Missing double quote error on line: " + lineNum);
                        atEOF = true;
                        return nextToken();
                    }
                } while (ch != '\"');
            } catch (Exception e) {
                atEOF = true;
            }
            return newStrToken(str,startPosition,endPosition,lineNum); // returns a string
        }
        
        // At this point the only tokens to check for are one or two
        // characters; we must also check for comments that begin with
        // 2 slashes
        String charOld = "" + ch;
        String op = charOld;
        Symbol sym;
        try {
            endPosition++;
            ch = source.read();
            op += ch;
            // check if valid 2 char operator; if it's not in the symbol
            // table then don't insert it since we really have a one char
            // token
            sym = Symbol.symbol(op, Tokens.BogusToken); 
            if (sym == null) {  // it must be a one char token
                return makeToken(charOld,startPosition,endPosition,lineNum);
            }
            endPosition++;
            ch = source.read();
            return makeToken(op,startPosition,endPosition,lineNum);
        } catch (Exception e) {}
        atEOF = true;
        if (startPosition == endPosition) {
            op = charOld;
        }
        return makeToken(op,startPosition,endPosition,lineNum);
    }
}
