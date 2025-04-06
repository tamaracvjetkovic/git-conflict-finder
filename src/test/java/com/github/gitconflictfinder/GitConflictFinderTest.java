package com.github.gitconflictfinder;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class GitConflictFinderTest {
    @Test
    public void testFindConflicts() {
        GitConflictFinder finder = new GitConflictFinder();
        try {
            ArrayList<String> conflicts = finder.findConflicts("owner", "repo", "token", "/path/to/repo", "branchA", "branchB");
            assertNotNull(conflicts);
        } catch (Exception e) {
            fail("Exception should not be thrown during test: " + e.getMessage());
        }
    }
}
