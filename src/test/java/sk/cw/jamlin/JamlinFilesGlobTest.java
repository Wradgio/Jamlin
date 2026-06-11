package sk.cw.jamlin;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JamlinFilesGlobTest {

    private static final String TESTDATA = System.getProperty("user.dir") + File.separator + "testdata";

    @Test
    public void listFilesMatchingGlob_findsFilesInWorkingDirectory() {
        List<String> matches = JamlinFiles.listFilesMatchingGlob(TESTDATA, "jamlin_demo*.html");
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().anyMatch(path -> path.endsWith("jamlin_demo.html")));
    }

    @Test
    public void listFilesMatchingGlob_findsFilesInSubdirectory() {
        List<String> matches = JamlinFiles.listFilesMatchingGlob(TESTDATA, "level 1/routes-*.html");
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().anyMatch(path -> path.contains("routes-en.html")));
    }

    @Test
    public void matchesGlobPath_matchesRelativePaths() {
        assertTrue(JamlinFiles.matchesGlobPath("jamlin_demo*.html", "jamlin_demo.html"));
        assertTrue(JamlinFiles.matchesGlobPath("level 1/routes-*.html", "level 1/routes-sk.html"));
        assertFalse(JamlinFiles.matchesGlobPath("test-*.html", "jamlin_demo.html"));
    }
}
