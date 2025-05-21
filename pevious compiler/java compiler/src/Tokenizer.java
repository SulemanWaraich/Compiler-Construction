import java.util.*;

public class Tokenizer {
    private static final Set<String> keywords = Set.of(
        "func", "number", "publish", "call", "suppose", "begin", "end"
        // Add other custom keywords here
    );

    public static List<Token> tokenize(String line) {
        List<Token> tokens = new ArrayList<>();

        // Split by space and common symbols (basic version)
        String[] parts = line.split("(?=[(){};,+\\-*/=])|(?<=[(){};,+\\-*/=])|\\s+");

        for (String part : parts) {
            if (part.isBlank()) continue;

            if (keywords.contains(part)) {
                tokens.add(new Token("KEYWORD", part));
            } else if (part.matches("[a-zA-Z_]\\w*")) {
                tokens.add(new Token("IDENTIFIER", part));
            } else if (part.matches("\\d+(\\.\\d+)?")) {
                tokens.add(new Token("NUMBER", part));
            } else if (part.matches("[(){};,+\\-*/=]")) {
                tokens.add(new Token("SYMBOL", part));
            } else {
                tokens.add(new Token("UNKNOWN", part));
            }
        }

        return tokens;
    }

    public static void main(String[] args) {
        String line = "func sum(number a, number b) {";
        List<Token> tokens = tokenize(line);
        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
