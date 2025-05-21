import java.io.*;

public class MainCompiler {
    public static void main(String[] args) {
        File folder = new File("programs");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".ht"));

        if (files == null || files.length == 0) {
            System.out.println("No .ht files found in 'programs/' folder.");
            return;
        }

        long totalStart = System.nanoTime();

        for (File file : files) {
            System.out.println("\n=== Processing file: " + file.getName() + " ===");
            long fileStart = System.nanoTime();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                Parser parser = new Parser();  // ✅ For syntax structure checks
                Lexer lexer = new Lexer();     // ✅ For logic and interpretation

                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Skip empty lines and comments
                    if (
                        line.isEmpty() ||
                        line.startsWith("//") ||
                        (line.startsWith("/\\") && line.endsWith("/\\"))
                    ) {
                        lineNumber++;
                        continue;
                    }

                    try {
                        parser.analyzeLine(line, lineNumber);  // ✅ Syntax check first
                        lexer.analyze(line, lineNumber);       // ✅ Then interpret logic
                    } catch (Exception e) {
                        System.out.printf("❌ Error at line %d in %s: %s%n", lineNumber, file.getName(), e.getMessage());
                    }

                    lineNumber++;
                }

            } catch (IOException e) {
                System.out.printf("❌ Failed to read file '%s': %s%n", file.getName(), e.getMessage());
            }

            long fileEnd = System.nanoTime();
            double fileElapsedMs = (fileEnd - fileStart) / 1_000_000.0;
            System.out.printf("✓ File '%s' processed in %.3f ms%n", file.getName(), fileElapsedMs);
        }

        long totalEnd = System.nanoTime();
        double totalElapsedMs = (totalEnd - totalStart) / 1_000_000.0;
        System.out.printf("\nTotal processing time for all files: %.3f ms%n", totalElapsedMs);
    }
}
