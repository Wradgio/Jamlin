package sk.cw.jamlin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class TestResources {

    static final Path TESTDATA = Paths.get("testdata").toAbsolutePath().normalize();

    private TestResources() {
    }

    static String path(String relativePath) {
        return TESTDATA.resolve(relativePath).toString();
    }

    static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(TESTDATA.resolve(relativePath)), StandardCharsets.UTF_8);
    }

    static File file(String relativePath) {
        return TESTDATA.resolve(relativePath).toFile();
    }
}
