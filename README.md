# README

# GitConflictFinder Library 📚

A library that detects conflicted files between two branches in a GitHub repository.

It is assumed that:
1) there is a local clone of a GitHub repo,
2) the branchA is present in both: remote and local repositories,
3) the local branchA is not necessarily synchronized with the remote branchA,
4) the local branchB is created from branchA locally,
5) there is only one merge base between the branches, the same remotely and locally

# Using the Library ⚙️

To use this library, follow the next steps:

1) clone this repo,
2) open the terminal in `/git-conflict-finder` folder,
3) run the command `mvn clean package`
- this command will produce two jars in the `/git-conflict-finder/target` folder - `git-conflict-finder-1.0.0` and `git-conflict-finder-1.0.0-fat`
- the `git-conflict-finder-1.0.0-fat` includes the the dependencies as well, since GitConflictFinder uses multiple libraries
4) copy the `git-conflict-finder-1.0.0-fat` to your project
- you can paste it into a `/lib` folder
- if your project is in a folder named MyProject, you can put the jar into `/MyProject/lib/git-conflict-finder-1.0.0-fat`
- the folder MyProject is the root that includes src, target, pom etc..
5) add the following dependency in the pom.xml:
```
<dependencies>
    <dependency>
        <groupId>com.github.gitconflictfinder</groupId>
        <artifactId>git-conflict-finder</artifactId>
        <version>1.0.0</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/lib/git-conflict-finder-1.0.0-fat.jar</systemPath>
    </dependency>
</dependencies>
```
and sync the project! :) 

# Demo code (fast example) 🕹️

You can use this demo code to test the library:

```
public class Main {
    public static void main(String[] args) {
        String ownerName = "repo_owner";
        String repoName = "repo_name";
        String accessToken = "access_token"; // check down below how to get the access token fast :)

        String localRepoPath = "C:\\Users\\my_username\\Desktop\\my_folder\\Random\\git-test-project";

        String branchA = "main";
        String branchB = "dev";

        try {
            ArrayList<String> conflictedFiles = GitConflictFinder.findConflicts(ownerName, repoName, accessToken, localRepoPath, branchA, branchB);
            if (!conflictedFiles.isEmpty()) {
                for (String file : conflictedFiles) {
                    System.out.println(file);
                }
            } else {
                System.out.println("No conflicts found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

# Helpful 💡

How to get the GitHub **access** **token**? 🔑
- In the upper-right corner of any page on GitHub, click your profile photo, then click Settings.
- In the left sidebar, click Developer settings.
- In the left sidebar, under Personal access tokens, click Tokens (classic).
- Select Generate new token, then click Generate new token (classic).
