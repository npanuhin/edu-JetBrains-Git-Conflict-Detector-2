<h1 align="center">Git Conflict Detector</h1>

This project identifies files on a local branch (`branchB`) that have also been modified in the remote branch (`origin/branchA`) since the last common commit ancestor (merge base) using `git` and the GitHub API, without fetching the remote branch to the local machine.

The real-life purpose of this application is to help developers detect potential conflicts before merging changes, allowing them to resolve issues in advance.
