**HOW TO USE / TEST**

1.) Clone this repository or download the NewMotorPH v1.0 JAR file.

2.) Import the payrollsystem_db.sql file into MySQL Workbench to create the database and tables.

3.) Open the securedb.properties file and update the MySQL username and password to match your local setup:

securedb.url=jdbc:mysql://localhost:3306/payrollsystem_db

securedb.user=your_mysql_username

securedb.password=your_mysql_password

4.) You may use the following sample login credentials to test the system:

• Admin: manuel.garcia / Test123

• Manager: brad.sanjose / Test123

• Regular Employee: mark.bautista / Test123
        
5.) For additional test accounts and role information, you may refer to the user, role, and userrole tables in the database.

NOTE:

If the repository has been cloned, and decided to run the source code kindly update the securedb.properties at src/main/resources/config/securedb.properties.

If the NewMotorPH v1.0 JAR file has been downloaded from the Releases, kindly unzip the JAR file, edit the config/securedb.properties file inside to match your local setup.

Thank you so much! Have a blessed day!
