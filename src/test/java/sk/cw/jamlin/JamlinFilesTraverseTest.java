package sk.cw.jamlin;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class JamlinFilesTraverseTest {

    private static final String TESTDATA = System.getProperty("user.dir") + File.separator + "testdata";

    @Test
    public void listValidFiles_withoutTraverse_listsOnlyTopLevelFiles() {
        List<String> files = JamlinFiles.listValidFiles(
                new File(TESTDATA + File.separator + "level 1"),
                Arrays.asList("html"),
                false);

        assertTrue(files.size() > 0);
        for (String file : files) {
            assertTrue(file.contains("level 1" + File.separator));
            assertFalseContainsNestedLevel(file, "level 2");
        }
    }

    @Test
    public void listValidFiles_withTraverse_includesNestedFiles() {
        List<String> shallow = JamlinFiles.listValidFiles(
                new File(TESTDATA + File.separator + "level 1"),
                Arrays.asList("html"),
                false);
        List<String> deep = JamlinFiles.listValidFiles(
                new File(TESTDATA + File.separator + "level 1"),
                Arrays.asList("html"),
                true);

        assertTrue(deep.size() > shallow.size());
    }

    private static void assertFalseContainsNestedLevel(String file, String nestedDir) {
        String nestedSegment = "level 1" + File.separator + nestedDir;
        if (file.contains(nestedSegment)) {
            throw new AssertionError("Expected no nested file but found: " + file);
        }
    }
}
