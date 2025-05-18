import java.io.*;

public class MainCompiler {
    public static void main(String[] args) throws IOException {
        File folder = new File("programs");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".ht"));

        if (files == null || files.length == 0) {
            System.out.println("No .ht files found in programs/ folder.");
            return;
        }

        for (File file : files) {
            System.out.println("\nProcessing file: " + file.getName());
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Lexer lexer = new Lexer();
            parser parser = new parser();

            String line;
            int lineNumber = 1;
            boolean insideMain = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.equals("") || parser.isComment(line)) {
                    lineNumber++;
                    continue;
                }

                if (parser.isMainStart(line)) {
                    insideMain = true;
                } else if (parser.isMainEnd(line)) {
                    insideMain = false;
                } else if (!parser.isValidLine(line)) {
                    System.out.println("Syntax Error on line " + lineNumber + ": Invalid statement → " + line);
                }

                if (insideMain && parser.isValidLine(line)) {
                    lexer.analyze(line, lineNumber);
                }

                lineNumber++;
            }

            reader.close();
        }
    }
}
