import javax.print.attribute.HashDocAttributeSet;
import java.sql.*;
import java.util.ArrayList;

public class Model {
    private static final String CONNECTION_URL = "jdbc:mysql://localhost:3306/infom";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "root";

    private Connection connection;

    private int currentStudentID = 1;
    private int currentAdminID = 1;

    public Model() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(
                    CONNECTION_URL,
                    DB_USERNAME,
                    DB_PASSWORD
            );

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public boolean loginAdmin(String idNumber) {
        try {
            String query =  "SELECT ID " +
                            "FROM admins " +
                            "WHERE admin_ID = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, idNumber);

            ResultSet rs = stmt.executeQuery();
            if (!rs.isBeforeFirst()) {
                return false;
            }

            rs.next();
            this.currentAdminID = rs.getInt("ID");
            return true;
        } catch (SQLException e) {}

        return false;
    }

    public boolean loginStudent(String idNumber) {
        try {
            String query =  "SELECT ID " +
                            "FROM students " +
                            "WHERE student_ID = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, idNumber);

            ResultSet rs = stmt.executeQuery();
            if (!rs.isBeforeFirst()) {
                return false;
            }

            rs.next();
            this.currentStudentID = rs.getInt("ID");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean createStudent(Student newStudent) {
        try {
            String query =  "INSERT INTO students (student_ID, first_name, middle_name, last_name) " +
                            "VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, newStudent.idNumber());
            stmt.setString(2, newStudent.firstName());
            stmt.setString(3, newStudent.middleName());
            stmt.setString(4, newStudent.lastName());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }


    /**
     * For admins when creating a new course (COURSE, NOT COURSE SECTION).
     * @param newCourse Course record with info, leave prereq a blank string if no prerequisite.
     * @return true if successful, false otherwise
     */
    public boolean createCourse(Course newCourse) {
         try {
             // FIRST GET THE ID OF THE PREREQUISITE COURSE IF ANY
             String idQuery = "SELECT ID FROM Courses WHERE code = ?";
             PreparedStatement idStmt = connection.prepareStatement(idQuery);
             idStmt.setString(1, newCourse.prereqCode());

             int prereqID = 0;
             ResultSet rs = idStmt.executeQuery();
             if (rs.isBeforeFirst()) {
                 rs.next();
                 prereqID = rs.getInt("ID");
             }

             // THEN PREPARE THE INSERT QUERY
             String query;
             if (prereqID == 0) {
                 query = "INSERT INTO courses (code, name) " +
                         "VALUES (?, ?)";
             } else {
                 query = "INSERT INTO courses (code, name, prerequisite_ID) " +
                         "VALUES (?, ?, ?)";
             }

             PreparedStatement stmt = connection.prepareStatement(query);
             stmt.setString(1, newCourse.code());
             stmt.setString(2, newCourse.name());
             if (prereqID != 0) {
                 stmt.setInt(3, prereqID);
             }

             stmt.executeUpdate();
             return true;
         } catch (SQLException e) {
             return false;
         }
    }

    /**
     * For admins, creates COURSE SECTION. Creates a course section based on the course code passed in and schedule.
     * @param newCourseSection CourseSection that holds the data used in creating new coursesection
     * @return true if successful, false otherwise
     */
    public boolean createCourseSection(CourseSection newCourseSection) {
        try {
            // FIRST GET THE ID OF THE COURSE USING THE SUPPLIED CODE
            int courseID = getCourseID(newCourseSection.code());

            // if the course ID was not found
            if (courseID == 0) {
                return false;
            }

            // PREPARE INSERT STATEMENT
            String query =  "INSERT INTO course_section (course_ID, schedule, name) " +
                            "VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, courseID);
            stmt.setString(2, newCourseSection.schedule());
            stmt.setString(3, newCourseSection.name());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean createHomework(Homework newHomework) {
        try {
            int courseID = getCourseID(newHomework.courseCode());

            if (courseID == 0) {
                return false;
            }

            // PREPARE THE QUERY
            String query =  "INSERT INTO Homeworks (description, deadline, course_ID)" +
                            "VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, newHomework.description());
            stmt.setDate(2, Date.valueOf(newHomework.deadline()));
            stmt.setInt(3, courseID);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Enrolls a student to a course. The student that will be enrolled is the student that is logged in.
     * @return true if successful, false otherwise
     */
    public boolean createStudentCourse(CourseSection courseSection) {
        try {
            // CHECK FIRST IF STUDENT HAS PREREQUISITE COMPLETED
            int prerequisiteID = getPrerequisiteID(courseSection.code());

            // check if there is even a prerequisite
            if (prerequisiteID != 0) {
                String prereqQuery = "SELECT ID " +
                        "FROM student_completed " +
                        "WHERE student_ID = ? " +
                        "AND course_ID = ?";
                PreparedStatement prereqStmt = connection.prepareStatement(prereqQuery);
                prereqStmt.setInt(1, this.currentStudentID);
                prereqStmt.setInt(2, prerequisiteID);

                ResultSet prereqRs = prereqStmt.executeQuery();
                if (!prereqRs.isBeforeFirst()) {
                    return false;
                }
            }

            // GET THE SECTION ID
            String sectionIDQuery = "SELECT ID " +
                    "FROM course_section " +
                    "WHERE course_ID = ? " +
                    "AND schedule = ?";
            PreparedStatement secIDStmt = connection.prepareStatement(sectionIDQuery);
            secIDStmt.setInt(1, getCourseID(courseSection.code()));
            secIDStmt.setString(2, courseSection.schedule());

            int sectionID = 0;
            ResultSet rs = secIDStmt.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.next();
                sectionID = rs.getInt("ID");
            }

            // if section id was not found
            if (sectionID == 0) {
                return false;
            }

            // PREPARE THE INSERT STATEMENT
            String query =  "INSERT INTO student_course (student_ID, section_ID, grade) " +
                            "VALUES (?, ?, 0)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, this.currentStudentID);
            stmt.setInt(2, sectionID);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Creates a submission for a particular homework. Checks first if there is a submission already.
     * If there is a submission, returns false, and does not go through with saving.
     * @param homework Homework to create a submission for
     * @param filepath filepath of file to submit
     * @return true if successful, false otherwise
     */
    public boolean createSubmission(Homework homework, String filepath) {
        try {
            int homeworkID = getHomeworkID(homework);

            if (homeworkID == 0) {
                return false;
            }

            // CHECK IF THERE IS A SUBMISSION ALREADY
            String checkQuery = "SELECT *" +
                                "FROM submissions " +
                                "WHERE student_ID = ? AND homework_ID = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            checkStmt.setInt(1, this.currentStudentID);
            checkStmt.setInt(2, homeworkID);

            ResultSet checkRS = checkStmt.executeQuery();
            if (checkRS.isBeforeFirst()) {
                return false;
            }

            String query =  "INSERT INTO submissions (student_ID, homework_ID, status, file) " +
                            "VALUES (?, ?, 'Completed', ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, this.currentStudentID);
            stmt.setInt(2, homeworkID);
            stmt.setString(3, filepath);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }

    }

    public Admin getCurrentAdmin() {
        Admin admin = null;

        try {
            String query =  "SELECT admin_ID, first_name, middle_name, last_name " +
                    "FROM admins " +
                    "WHERE ID = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, this.currentAdminID);

            ResultSet rs = stmt.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.next();
                admin = new Admin(
                        rs.getString("admin_ID"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name")
                );
            }
        } catch (SQLException e) {}

        return admin;
    }

    public Student getCurrentStudent() {
        Student student = null;

        try {
            String query =  "SELECT student_ID, first_name, middle_name, last_name " +
                            "FROM STUDENTS " +
                            "WHERE ID = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, this.currentStudentID);

            ResultSet rs = stmt.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.next();
                student = new Student(
                        rs.getString("student_ID"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name")
                );
            }
        } catch (SQLException e) {}

        return student;
    }

    public ArrayList<Student> getStudents() {
        ArrayList<Student> students = new ArrayList<>();

        try {
            String query = "SELECT * FROM students";
            PreparedStatement stmt = connection.prepareStatement(query);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Student student = new Student(
                        rs.getString("student_ID"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name")
                );

                students.add(student);
            }

        } catch (SQLException e) {}

        return students;
    }

    /**
     * This is for viewing courses and their prerequisites.
     * @return ArrayList of courses
     */
    public ArrayList<Course> getCourses() {
        ArrayList<Course> courses = new ArrayList<>();

        try {
            String query = "SELECT c.code AS code, c.name AS name, p.code AS prerequisite " +
                    "FROM courses AS c " +
                    "LEFT JOIN courses AS p " +
                    "ON p.ID = c.prerequisite_ID";
            PreparedStatement stmt = connection.prepareStatement(query);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("prerequisite")
                );

                courses.add(course);
            }
        } catch (SQLException e) {}

        return courses;
    }

    /**
     * This is for viewing the available sections for enrollment. Gets all Course Sections
     * @return ArrayList of CourseSections
     */
    public ArrayList<CourseSection> getCourseSections() {
        ArrayList<CourseSection> courseSections = new ArrayList<>();

        try {
            String query =  "SELECT c.code AS code, cs.name AS name, cs.schedule AS schedule " +
                            "FROM courses AS c, course_section AS cs " +
                            "WHERE c.ID = cs.course_ID";
            PreparedStatement stmt = connection.prepareStatement(query);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                CourseSection cs = new CourseSection(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("schedule")
                );

                courseSections.add(cs);
            }

        } catch (SQLException e) {}

        return courseSections;
    }

    public ArrayList<Homework> getHomeworks() {
        ArrayList<Homework> homeworks = new ArrayList<>();

        try {
            String query =  "SELECT c.code AS code, h.description AS description, h.deadline AS deadline " +
                            "FROM homeworks AS h, courses as c " +
                            "WHERE h.course_ID = c.ID";
            PreparedStatement stmt = connection.prepareStatement(query);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Homework homework = new Homework(
                        rs.getString("code"),
                        rs.getString("description"),
                        rs.getDate("deadline").toString()
                );

                homeworks.add(homework);
            }
        } catch (SQLException e) {}

        return homeworks;
    }

    /**
     * Gets the current student's homeworks. GETS homework from ALL enrolled courses.
     * @return homeworks ArrayList
     */
    public ArrayList<Homework> getStudentHomeworks() {
        ArrayList<Homework> homeworks = new ArrayList<>();

        try {
            String query =  "SELECT c.code, h.description, h.deadline " +
                            "FROM Homeworks AS h, Courses AS c " +
                            "WHERE h.course_ID in ( " +
                                "SELECT course_ID " +
                                "FROM course_section " +
                                "WHERE ID in ( " +
                                    "SELECT section_ID " +
                                    "FROM student_course " +
                                    "WHERE student_ID = ? " +
                                ") " +
                            ") " +
                            "AND h.course_ID = c.ID";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, this.currentStudentID);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Homework homework = new Homework(
                        rs.getString("code"),
                        rs.getString("description"),
                        rs.getDate("deadline").toString()
                );

                homeworks.add(homework);
            }
        } catch (SQLException e) {}

        return homeworks;
    }

    public ArrayList<Homework> getStudentHomeworks(String courseCode) {
        ArrayList<Homework> homeworks = new ArrayList<>();

        try {
            String query =  "SELECT c.code, h.description, h.deadline " +
                            "FROM Homeworks AS h, Courses AS c " +
                            "WHERE h.course_ID = ? " +
                            "AND h.course_ID = c.ID";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, getCourseID(courseCode));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Homework homework = new Homework(
                        rs.getString("code"),
                        rs.getString("description"),
                        rs.getDate("deadline").toString()
                );

                homeworks.add(homework);
            }
        } catch (SQLException e) {}

        return homeworks;
    }

    /**
     * This is for viewing a student's enrolled courses. Gets the logged in student's courses.
     * @return ArrayList of course sections
     */
    public ArrayList<CourseSection> getStudentCourseSections() {
        ArrayList<CourseSection> courseSections = new ArrayList<>();

        try {
            String query =  "SELECT c.code AS code, cs.name AS name, cs.schedule AS schedule " +
                            "FROM course_section AS cs, student_course AS sc, courses AS c " +
                            "WHERE sc.student_ID = ? " +
                            "AND cs.ID = sc.section_ID " +
                            "AND cs.course_ID = c.ID";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, this.currentStudentID);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                CourseSection cs = new CourseSection(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("schedule")
                );

                courseSections.add(cs);
            }
        } catch (SQLException e) {}

        return courseSections;
    }

    /**
     * Grabs all submissions from all courses.
     * @return
     */
    public ArrayList<Submission> getSubmissions() {
        ArrayList<Submission> submissions = new ArrayList<>();

        try {
            String query =  "SELECT " +
                                "hc.code, "+
                                "CONCAT(st.first_name, ' ', st.middle_name, ' ', st.last_name) AS full_name, " +
                                "DATE_FORMAT(su.submitted_on, '%Y-%m-%d') AS submitted_on, " +
                                "su.file " +
                            "FROM submissions AS su " +
                            "LEFT JOIN ( " +
                                "SELECT h.ID AS ID, c.code AS code " +
                                "FROM homeworks AS h, courses AS c " +
                                "WHERE c.ID = h.course_ID " +
                            ") AS hc ON hc.ID = su.homework_ID " +
                            "LEFT JOIN students AS st ON su.student_ID = st.ID";
            PreparedStatement stmt = connection.prepareStatement(query);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Submission submission = new Submission(
                        rs.getString("code"),
                        rs.getString("full_name"),
                        rs.getString("submitted_on"),
                        rs.getString("file")
                );

                submissions.add(submission);
            }
        } catch (SQLException e) {}

        return submissions;
    }

    public ArrayList<Submission> getStudentSubmissions() {
        ArrayList<Submission> submissions = new ArrayList<>();

        try {
            String query =  "SELECT " +
                    "hc.code, "+
                    "CONCAT(st.first_name, ' ', st.middle_name, ' ', st.last_name) AS full_name, " +
                    "DATE_FORMAT(su.submitted_on, '%Y-%m-%d') AS submitted_on, " +
                    "su.file " +
                    "FROM submissions AS su " +
                    "LEFT JOIN ( " +
                    "SELECT h.ID AS ID, c.code AS code " +
                    "FROM homeworks AS h, courses AS c " +
                    "WHERE c.ID = h.course_ID " +
                    ") AS hc ON hc.ID = su.homework_ID " +
                    "LEFT JOIN students AS st ON su.student_ID = st.ID " +
                    "WHERE st.ID = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, this.currentStudentID);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Submission submission = new Submission(
                        rs.getString("code"),
                        rs.getString("full_name"),
                        rs.getString("submitted_on"),
                        rs.getString("file")
                );

                submissions.add(submission);
            }
        } catch (SQLException e) {}

        return submissions;
    }

    private int getCourseID(String courseCode) {
        try {
            // FIRST GET THE ID OF THE COURSE USING THE SUPPLIED CODE
            String idQuery = "SELECT ID FROM Courses WHERE code = ?";
            PreparedStatement idStmt = connection.prepareStatement(idQuery);
            idStmt.setString(1, courseCode);

            int courseID = 0;
            ResultSet rs = idStmt.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.next();
                courseID = rs.getInt("ID");
            }

            return courseID;
        } catch (SQLException e) {
            return 0;
        }
    }

    private int getHomeworkID(Homework homework) {
        try {
            String query =  "SELECT ID " +
                            "FROM homeworks " +
                            "WHERE description = ? " +
                                "AND deadline = ? " +
                                "AND course_ID = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, homework.description());
            stmt.setDate(2, Date.valueOf(homework.deadline()));
            stmt.setInt(3, getCourseID(homework.courseCode()));

            int homeworkID = 0;
            ResultSet rs = stmt.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.next();
                homeworkID = rs.getInt("ID");
            }

            return homeworkID;
        } catch (SQLException e) {
            return 0;
        }
    }

    private int getPrerequisiteID(String courseCode) {
        try {
            // FIRST GET THE ID OF THE COURSE USING THE SUPPLIED CODE
            String idQuery = "SELECT prerequisite_ID FROM Courses WHERE code = ?";
            PreparedStatement idStmt = connection.prepareStatement(idQuery);
            idStmt.setString(1, courseCode);

            int prereqID = 0;
            ResultSet rs = idStmt.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.next();
                prereqID = rs.getInt("prerequisite_ID");
            }

            return prereqID;
        } catch (SQLException e) {
            return 0;
        }
    }
}
