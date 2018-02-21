# GH Extractor
---

<h3><u>How to use</u>:</h3>

<strong>Note 1:</strong>
<i>
	The primary files needed in this repository are contained within the `	lib/` and `src/` directories.
</i>
>`lib` - contains the ```gson``` which is used to pretty-JSONify the 	responses from GitHub's API, as well as the Oracle SQL driver if 	interaction with a database is needed. 

>`src` - contains the `GHExtractor.java` class as well as a `Tests.java` 	class for running tests within the repository itself.

<strong>Note 2:</strong> 
<i>As this tool is used to read and download raw content from files on github, an internet connection is required to run it without any errors. 
</i>

<strong>Note 3:</strong> 
<i>In it's current form, this tool <u>cannot</u> read files who's raw data exists in binary form as it is primarily used to read `*.ddl`, `*.sql`, `*.js`, `*.sh` or `*.py` scripts currently.</i>

<strong> Note 4:</strong>
<i> Currently, in order to properly query for and extract files by filename only, a `.directorymap` is required to be placed within the directory root of a given repository. This requirement should be resolved soon as it is a pretty cumbersome solution. 

--

<strong>The `GHExtractor` class can be instantiated using the following 3 parameters:</strong>
<p />

	1. tRepo (String): 
	------------------
	Target repository name to pull file(s) from.
___
	2. uName (String): 
	------------------ 
	GitHub username; specifically the username the repository is published under.
___
	3. authT (String): 
	------------------ 
	Authentication token to access the repository, if any is required. Otherwise, use an empty string {""}. 
--
<p />
<strong>
Once instantiated, files can be downloaded by calling the `GetFileFromGithub()` method with the following 2 parameters:
</strong>
--
	1. fileName (String): 
	--------------------- 
	The specific filename, including extension, to search the repository for and download. If {"*"} is used here, GHExtractor will download everything listed within the .directorymap file.
___
	2. outDirectory (String): 
	------------------------- 
	The target directory to download the file to, from the reference-point of the project root directory. If left as an empty string, or if the provided directory doesn't exist, this will download to the root of the project using GHExtractor instead.
--
### Usage Example:
--
```java
public static void main(String[] args) {
	
	try {
		GHExtractor extractor = new GHExtractor("GHExtractor", "jsrj", "");
		extractor.GetFileFromGithub("*", "files-from-github");
	}
	catch (Exception e) {
			e.printStackTrace()
	}        
}
```
--
### .directorymap template:
```text
# ...Structure...
# ..."#"      = Comment line.
# ...FILENAME = Name of file within repo or directory.
# ..."=>"     = Symbol that associates a file to a route.
# ..."Route"  = The associated path within a repo directory to a specific file.
#
# E.G.   [ FILENAME  ] => /[  ROUTE   ]/

demo.ddl => /test-directory/
demo.sql => /test-directory/
```