public class parser {

    // Comment lines
    public boolean isComment(String line) {
        return line.startsWith("/\\") && line.endsWith("/\\");
    }

    // Main block detection
    public boolean isMainStart(String line) {
        return line.equals("begin main {");
    }

    public boolean isMainEnd(String line) {
        return line.equals("}");
    }

    // Valid syntax structures
public boolean isValidLine(String line) {
    return line.startsWith("Alphan") || line.startsWith("print") || line.startsWith("publish") || line.startsWith("number");
}

    }
    
