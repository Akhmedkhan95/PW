import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.*;

public class FileManager {
    private static Path currentPath = Paths.get("").toAbsolutePath();
    private static final Scanner scanner = new Scanner(System.in);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("File Manager (java.io)");
        System.out.println("Current directory: " + currentPath);
        System.out.println("Type 'help' for list of commands");

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts = input.split(" ");
            String command = parts[0];
            String[] arguments = Arrays.copyOfRange(parts, 1, parts.length);

            try {
                switch (command) {
                    case "ls":
                        listFiles(arguments);
                        break;
                    case "cd":
                        changeDirectory(arguments);
                        break;
                    case "mkdir":
                        createDirectory(arguments);
                        break;
                    case "rm":
                        removeFileOrDirectory(arguments);
                        break;
                    case "mv":
                        moveOrRename(arguments);
                        break;
                    case "cp":
                        copyFile(arguments);
                        break;
                    case "finfo":
                        fileInfo(arguments);
                        break;
                    case "find":
                        findFile(arguments);
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "exit":
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Unknown command: " + command);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void listFiles(String[] args) throws IOException {
        boolean detailed = false;
        if (args.length > 0 && args[0].equals("-i")) {
            detailed = true;
        }

        File dir = currentPath.toFile();
        File[] files = dir.listFiles();

        if (files == null) {
            System.out.println("No files in directory");
            return;
        }

        for (File file : files) {
            if (detailed) {
                System.out.printf("%-30s %10d bytes %20s %s%n",
                        file.getName(),
                        file.length(),
                        dateFormat.format(new Date(file.lastModified())),
                        file.isDirectory() ? "[DIR]" : "");
            } else {
                System.out.println(file.getName() + (file.isDirectory() ? "/" : ""));
            }
        }
    }

    private static void changeDirectory(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: cd [path]");
            return;
        }

        Path newPath;
        if (args[0].equals("..")) {
            newPath = currentPath.getParent();
            if (newPath == null) {
                System.out.println("Already at root directory");
                return;
            }
        } else {
            newPath = currentPath.resolve(args[0]);
        }

        File file = newPath.toFile();
        if (!file.exists()) {
            System.out.println("Directory does not exist: " + newPath);
            return;
        }
        if (!file.isDirectory()) {
            System.out.println("Not a directory: " + newPath);
            return;
        }

        currentPath = newPath.normalize().toAbsolutePath();
        System.out.println("Current directory: " + currentPath);
    }

    private static void createDirectory(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: mkdir [name]");
            return;
        }

        Path newDir = currentPath.resolve(args[0]);
        File dir = newDir.toFile();
        if (dir.exists()) {
            System.out.println("Directory already exists: " + newDir);
            return;
        }

        if (dir.mkdir()) {
            System.out.println("Directory created: " + newDir);
        } else {
            System.out.println("Failed to create directory: " + newDir);
        }
    }

    private static void removeFileOrDirectory(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: rm [filename]");
            return;
        }

        Path path = currentPath.resolve(args[0]);
        File file = path.toFile();

        if (!file.exists()) {
            System.out.println("File or directory does not exist: " + path);
            return;
        }

        if (file.isDirectory()) {
            deleteDirectory(file);
            System.out.println("Directory removed: " + path);
        } else {
            if (file.delete()) {
                System.out.println("File removed: " + path);
            } else {
                System.out.println("Failed to remove file: " + path);
            }
        }
    }

    private static void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException("Failed to delete file: " + file);
                    }
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete directory: " + directory);
        }
    }

    private static void moveOrRename(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: mv [source] [destination] [-f]");
            return;
        }

        boolean force = args.length > 2 && args[2].equals("-f");
        Path source = currentPath.resolve(args[0]);
        Path destination = currentPath.resolve(args[1]);

        if (!source.toFile().exists()) {
            System.out.println("Source does not exist: " + source);
            return;
        }

        if (destination.toFile().exists() && !force) {
            System.out.println("Destination already exists: " + destination);
            System.out.println("Use -f to force overwrite");
            return;
        }

        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Moved: " + source + " -> " + destination);
    }

    private static void copyFile(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: cp [source] [destination] [-f]");
            return;
        }

        boolean force = args.length > 2 && args[2].equals("-f");
        Path source = currentPath.resolve(args[0]);
        Path destination = currentPath.resolve(args[1]);

        if (!source.toFile().exists()) {
            System.out.println("Source does not exist: " + source);
            return;
        }

        if (source.toFile().isDirectory()) {
            System.out.println("Cannot copy directories (use mv instead)");
            return;
        }

        if (destination.toFile().exists() && !force) {
            System.out.println("Destination already exists: " + destination);
            System.out.println("Use -f to force overwrite");
            return;
        }

        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Copied: " + source + " -> " + destination);
    }

    private static void fileInfo(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: finfo [filename]");
            return;
        }

        Path path = currentPath.resolve(args[0]);
        File file = path.toFile();

        if (!file.exists()) {
            System.out.println("File does not exist: " + path);
            return;
        }

        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        System.out.println("Name: " + file.getName());
        System.out.println("Path: " + path);
        System.out.println("Size: " + file.length() + " bytes");
        System.out.println("Last modified: " + dateFormat.format(new Date(file.lastModified())));
        System.out.println("Type: " + (file.isDirectory() ? "Directory" : "File"));
        System.out.println("Hidden: " + file.isHidden());
        System.out.println("Readable: " + file.canRead());
        System.out.println("Writable: " + file.canWrite());
        System.out.println("Executable: " + file.canExecute());
    }

    private static void findFile(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: find [filename]");
            return;
        }

        String fileName = args[0];
        System.out.println("Searching for '" + fileName + "' in " + currentPath + "...");

        List<Path> foundFiles = new ArrayList<>();
        Files.walkFileTree(currentPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().contains(fileName)) {
                    foundFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.getFileName().toString().contains(fileName)) {
                    foundFiles.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        if (foundFiles.isEmpty()) {
            System.out.println("No files found");
        } else {
            System.out.println("Found files:");
            for (Path file : foundFiles) {
                System.out.println(file);
            }
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  ls [-i]                - list files in current directory (-i for details)");
        System.out.println("  cd [path]              - change directory");
        System.out.println("  mkdir [name]          - create new directory");
        System.out.println("  rm [name]             - remove file or directory");
        System.out.println("  mv [src] [dest] [-f]  - move/rename file (-f to force)");
        System.out.println("  cp [src] [dest] [-f]  - copy file (-f to force)");
        System.out.println("  finfo [name]          - show file info");
        System.out.println("  find [name]          - search for file in current directory and subdirectories");
        System.out.println("  help                 - show this help");
        System.out.println("  exit                 - exit file manager");
    }
}