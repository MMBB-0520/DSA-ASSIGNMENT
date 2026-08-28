HOTEL MANAGEMENT SYSTEM
DSA ASSIGNMENT
==============

A Java console-based Hotel Management System built with custom
Abstract Data Types (ADTs) and a 3-Tier Layered Architecture.

1. HOW TO RUN THE APPLICATION
   =============================

## PREREQUISITES

* JDK 17 or higher must be installed.
* Gson 2.10.1 library is required.
* The Gson library is included in the lib/ folder:

  lib/gson-2.10.1.jar

## OPTION 1: RUN VIA IDE (RECOMMENDED)

VS CODE

1. Open the project folder "DSA-ASSIGNMENT" in VS Code.

2. In the Explorer sidebar, locate "JAVA PROJECTS".

3. Under "Referenced Libraries", click the "+" button.

4. Select:

   lib/gson-2.10.1.jar

5. Open:

   src/Main.java

6. Click "Run" or press F5.

7. Follow the instructions displayed in the console.

INTELLIJ IDEA / NETBEANS / ECLIPSE

1. Open the "DSA-ASSIGNMENT" project.

2. Add:

   lib/gson-2.10.1.jar

   to the project libraries or build path.

3. Open:

   src/Main.java

4. Run the main() method.

5. Follow the instructions displayed in the console.

## OPTION 2: RUN VIA TERMINAL / COMMAND LINE

Make sure the terminal working directory is the project root directory:

DSA-ASSIGNMENT

WINDOWS POWERSHELL

1. Compile all Java source files:

javac -cp "lib/gson-2.10.1.jar" -d bin (Get-ChildItem -Recurse src*.java).FullName

2. Run the application:

java -cp "bin;lib/gson-2.10.1.jar" Main

WINDOWS COMMAND PROMPT (CMD)

1. Compile all Java source files:

javac -cp "lib/gson-2.10.1.jar" -d bin src\Main.java src\adt*.java src\boundary*.java src\control*.java src\dao*.java src\dao\impl*.java src\data*.java src\entity*.java src\util*.java

2. Run the application:

java -cp "bin;lib/gson-2.10.1.jar" Main

MACOS / LINUX TERMINAL

1. Compile all Java source files:

find src -name "*.java" | xargs javac -cp "lib/gson-2.10.1.jar" -d bin

2. Run the application:

java -cp "bin:lib/gson-2.10.1.jar" Main

2. IMPORTANT NOTES
   ==================

* Make sure JDK 17 or higher is installed and configured correctly.
* Make sure gson-2.10.1.jar is available in the lib/ folder.
* Do not remove the JSON data files from the src/data/ folder.
* The application is operated through the console.
* When using the terminal, run the commands from the project root
  directory so that the relative file paths can be accessed correctly.

3. KEY MODULES AND CUSTOM ADTs
   ===============================

Walk-In Registration

* Uses CustomQueue<T>.
* Implements FIFO (First-In, First-Out) queue processing.

Housekeeping Task Log

* Uses ArrayStack<T> for LIFO (Last-In, First-Out) undo operations.
* Uses CustomLinkedList<T> for housekeeping task records.

Front-Desk Service

* Uses MyBinarySearchTree<T> for booking searches.
* Supports O(log N) booking search and O(N) BST rebalancing.

4. MAIN PROJECT COMPONENTS
   ===========================

Main application entry point:

src/Main.java

Custom ADTs:

src/adt/

User Interface:

src/boundary/

Business Logic:

src/control/

Data Access Objects:

src/dao/

JSON Data and Data Management:

src/data/

Entity Classes:

src/entity/

Utility Classes:

src/util/

5. DATA FILES
   ==============

The application uses JSON files for persistent data storage:

src/data/bookings.json
src/data/guests.json
src/data/rooms.json

6. CONTRIBUTORS
   ================

Ng Yuen Qi
Pang Jia Yie
Hu Qiao Feng
