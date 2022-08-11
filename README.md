#### Table of Contents
1. [Features](#features)
2. [Object Model](#object-model)
3. [Demo](#demo)
4. [Issues](#issues)
5. [Details](#details)
6. [TODO](#todo)

<br>

### Features<a name="features"></a>
* Core
  * Multiple folders can be synchronized
    * Delete operations are recorded, and synchronized.
    * For files with identical hashes, drop the modified date of the newer file.
* Additional
  * Folders can be added to ignore List
* Technical
  * FS/OS agnostic (Java `FileChannel`)

<br>

### Object Model<a name="object-model"></a>
```
DataRoot                a data root
\_ SyncBundle :         a bundle of directories on the FS to be syncronized.
   \_ SyncDirectory :   a directory on the FS.
      \_ SyncFile :     a file on the FS.
```

<br>


### Flow
* `for` each `SyncBundle`
  * `for` each `SyncDirectory`
    * check what files were CRUD, 
    * propagate to other `SyncDirectory` of current `SyncBundle`.

<br>


### Demo<a name="demo"></a>
[![IMAGE ALT TEXT](http://img.youtube.com/vi/znR3jyM_4Ss/0.jpg)](https://youtu.be/znR3jyM_4Ss "ensync WIP Demo")

<br>

### Issues<a name="issues"></a>

##### Detection of Concurrent Changes

Ensync has a core loop.
The duration of this cycle is determined by # of files and pause. <br>
To correctly detect a create/delete operation on different instances of a file on requires at most 5 cyles.
```
  * BAD
    * cycle 1 : A creates
    * cycle 2 : A deletes / B sync creates
    * cycle 3 : A sync creates
  * GOOD
    * cycle 1 : A creates
    * cycle 2 :           / B sync creates
    * cycle 3 : A ignores sync create
    * cycle 4 : A deletes
    * cycle 5 :           / B sync deletes
```
This means as # of files grows, we must wait longer and longer between modifying the same file.

This was somewhat addressed by switching to FileChannel, and locking all the files. <br>
However more tests must follow.

For the best practice is not to modify different instances of the same file
before having executed a core loop.   

##### Lazy first Run (Design Choice)
If *ensync* initially runs on a non-empty directory it will consider the
existing files as "on record", thus not "created". <br>
Hence ensyc will not push the changes to the other directories. <br>
This avoids an accidental push of massive file set,
but means that you have to copy the files manually the first time.

<br>

### Details  <a name="details"></a>

#### Record
* Used for tracking of file deletions.
* Located in each `SyncDirectory\record.ensync`
* Contains `<last edited><separator><relative file path>` for each file in the SyncDirectory.

#### Core Loop
Sync files across directories.

![alt text](https://raw.githubusercontent.com/IO42630/ensync/master/doc/flow-n-instances.png "Hello!")
<br>
<br>

#### Package Contents

| Path         | Comment |
|---------------|-------------|
doc | Diagrams.
src.com.olexyn.ensync.artifacts | Data Model: Maps, Directories, Files. 
src.com.olexyn.ensync.Main          | Run from here.
src.com.olexyn.ensync.Flow      | Flow of the synchronization.
src.com.olexyn.ensync. | Low level helper methods.

<br>

### TODO <a name="todo"></a> 

- Add tests.
- Reduce disk access.
- Add error handling. (i.e. if a web-directory is not available)
- Track files that were modified during the loop.
    - currently `writeRecord` just takes from `find`
    - this means any changes made during the loop will be written to the `Record`
    - and created files are tracked by comparing `Record` (=old state) and `State` (=new state).
    - because of this it will appear as if the file created while the loop was running
    was already there.
    - thus the creation of said file will not be replicated to the other directories.
    - to solve this `writeRecord` should take the old `State` 
    and manually add every operation that was performed by the loop (!= user created file while the loop was running).
 - File is created in DirB
    - Sync creates the file in DirA
    - Sync creates the file in DirB 
      - this means the file in DirB is overwritten with `cp` for no reason.
      - implement a check to prevent this.
