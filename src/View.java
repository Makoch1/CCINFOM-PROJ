import java.util.Scanner;
import java.util.ArrayList;

public class View {
    private final Model model;
    private final Scanner scanner;

    public View(Model model) {
        this.model = model;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("Are you an admin or a student?");
        System.out.println("1. Admin");
        System.out.println("2. Student");
        System.out.print("Choose your role: ");
        int role = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        switch (role) {
            case 1 -> {
                System.out.print("Enter your admin ID: ");
                if (model.loginAdmin(scanner.nextLine())) {
                    displayAdminMenu();
                } else {
                    System.out.println("Invalid admin ID. Please restart and enter a valid student ID.");
                }
            }
            case 2 -> {
                System.out.print("Enter your student ID: ");
                if (model.loginStudent(scanner.nextLine())) {
                    displayStudentMenu();
                } else {
                    System.out.println("Invalid student ID. Please restart and enter a valid student ID.");
                }

            }
            default -> System.out.println("Invalid choice. Please restart and choose a valid role.");
        }
    }

    private void displayAdminMenu() {
        Admin admin = model.getCurrentAdmin();
        String fullName = String.format(
                "%s %s %s",
                admin.firstName(),
                admin.middleName(),
                admin.lastName()
        );

        while (true) {
            System.out.println("\nWelcome Admin [ID " + admin.adminID() + "] " + fullName );
            System.out.println("--- Admin Menu ---");
            System.out.println("1. Create Student");
            System.out.println("2. Create Course");
            System.out.println("3. Create Course Section");
            System.out.println("4. Create Homework");
            System.out.println("5. View Students");
            System.out.println("6. View Courses");
            System.out.println("7. View Course Sections");
            System.out.println("8. View Homeworks");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();  // Consume newline

            switch (choice) {
                case 1 -> createStudent();
                case 2 -> createCourse();
                case 3 -> createCourseSection();
                case 4 -> createHomework();
                case 5 -> viewStudents();
                case 6 -> viewCourses();
                case 7 -> viewCourseSections();
                case 8 -> viewHomeworks();
                case 0 -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void displayStudentMenu() {
        Student student = model.getCurrentStudent();
        String fullName = String.format(
                "%s %s %s",
                student.firstName(),
                student.middleName(),
                student.lastName()
        );

        while (true) {
            System.out.println("\nWelcome " + fullName + " [ID " + student.idNumber() + "]");
            System.out.println("--- Student Menu ---");
            System.out.println("1. Enroll in Course Section");
            System.out.println("2. Create Submission");
            System.out.println("3. View Courses");
            System.out.println("4. View Course Sections");
            System.out.println("5. View Homeworks");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();  // Consume newline

            switch (choice) {
                case 1 -> enrollStudentInCourseSection();
                case 2 -> createSubmission();
                case 3 -> viewCourses();
                case 4 -> viewStudentCourseSections();
                case 5 -> viewStudentHomeworks();
                case 0 -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void createStudent() {
        System.out.print("Enter student ID: ");
        String id = scanner.nextLine();
        System.out.print("Enter first name: ");
        String firstName = scanner.nextLine();
        System.out.print("Enter middle name: ");
        String middleName = scanner.nextLine();
        System.out.print("Enter last name: ");
        String lastName = scanner.nextLine();

        Student newStudent = new Student(id, firstName, middleName, lastName);
        if (model.createStudent(newStudent)) {
            System.out.println("Student created successfully.");
        } else {
            System.out.println("Failed to create student.");
        }
    }

    private void createCourse() {
        System.out.print("Enter course code: ");
        String code = scanner.nextLine();
        System.out.print("Enter course name: ");
        String name = scanner.nextLine();

        // Print the list of courses for ease of use
        System.out.println("Here are the courses currently in the system: ");
        this.viewCourses();

        System.out.print("\nEnter prerequisite course code (leave blank if none): ");
        String prereqCode = scanner.nextLine();

        Course newCourse = new Course(code, name, prereqCode);
        if (model.createCourse(newCourse)) {
            System.out.println("Course created successfully.");
        } else {
            System.out.println("Failed to create course.");
        }
    }

    private void createCourseSection() {
        // print out the courses available
        System.out.println("Here are the courses currently in the system: ");
        this.viewCourses();

        System.out.print("\nEnter course code: ");
        String code = scanner.nextLine();

        System.out.print("Enter section name (e.g. XX22): ");
        String name = scanner.nextLine();

        // Split out the schedule, and stitch together
        System.out.print("Enter section's day code (e.g. M for Monday, etc.): ");
        Character day = scanner.nextLine().toUpperCase().charAt(0);

        System.out.print("Enter class's start time: ");
        int start = scanner.nextInt();
        scanner.nextLine(); // eat newline

        System.out.print("Enter class's end time: ");
        int end = scanner.nextInt();
        scanner.nextLine(); // eat newline

        // build schedule string
        String schedule = String.format("%c %d-%d", day, start, end);

        CourseSection newCourseSection = new CourseSection(code, name, schedule);
        if (model.createCourseSection(newCourseSection)) {
            System.out.println("Course section created successfully.");
        } else {
            System.out.println("Failed to create course section.");
        }
    }

    private void createHomework() {
        // check first if there are courses to make homework for
        if (model.getCourses().isEmpty()) {
            System.out.println("No courses yet! Make sure there are courses to make homework for.");
            return;
        }

        System.out.println("Here are the courses available in the system: ");
        this.viewCourses();

        System.out.print("\nEnter course code of the course you wish to make homework for: ");
        String courseCode = scanner.nextLine();
        System.out.print("Enter description: ");
        String description = scanner.nextLine();
        System.out.print("Enter deadline (YYYY-MM-DD): ");
        String deadline = scanner.nextLine();

        Homework newHomework = new Homework(courseCode, description, deadline);
        if (model.createHomework(newHomework)) {
            System.out.println("Homework created successfully.");
        } else {
            System.out.println("Failed to create homework.");
        }
    }

    private void enrollStudentInCourseSection() {
        ArrayList<CourseSection> courseSections = model.getCourseSections();

        if (courseSections.isEmpty()) {
            System.out.println("No course sections available.");
            return;
        }

        System.out.println("Available Course Sections:");
        for (int i = 0; i < courseSections.size(); i++) {
            CourseSection cs = courseSections.get(i);
            System.out.printf("%d. Code: %s, Name: %s, Schedule: %s%n", i + 1, cs.code(), cs.name(), cs.schedule());
        }

        System.out.print("Choose a course section by number: ");
        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        if (choice < 1 || choice > courseSections.size()) {
            System.out.println("Invalid choice.");
            return;
        }

        CourseSection selectedCourseSection = courseSections.get(choice - 1);
        if (model.createStudentCourse(selectedCourseSection)) {
            System.out.println("Student enrolled in course section successfully.");
        } else {
            System.out.println("Failed to enroll student in course section.");
        }
    }

    private void createSubmission() {
        ArrayList<Homework> homeworks = model.getStudentHomeworks();

        if (homeworks.isEmpty()) {
            System.out.println("No homeworks available.");
            return;
        }

        System.out.println("Available Homeworks:");
        for (int i = 0; i < homeworks.size(); i++) {
            Homework hw = homeworks.get(i);
            System.out.printf("%d. Course: %s, Description: %s, Deadline: %s%n", i + 1, hw.courseCode(), hw.description(), hw.deadline());
        }

        System.out.print("Choose a homework by number: ");
        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        if (choice < 1 || choice > homeworks.size()) {
            System.out.println("Invalid choice.");
            return;
        }

        Homework selectedHomework = homeworks.get(choice - 1);
        System.out.print("Enter file path: ");
        String filePath = scanner.nextLine();

        if (model.createSubmission(selectedHomework, filePath)) {
            System.out.println("Submission created successfully.");
        } else {
            System.out.println("Failed to create submission.");
        }
    }

    private void viewStudents() {
        ArrayList<Student> students = model.getStudents();
        if (students.isEmpty()) {
            System.out.println("No students in the system!");
            return;
        }

        // headers
        System.out.println("\n--- Students ---");
        System.out.println("ID NUMBER | NAME");

        for (Student student : students) {
            String formatted = String.format(
                    "%-9s | %s %s %s",
                    student.idNumber(),
                    student.firstName(),
                    student.middleName(),
                    student.lastName()
            );
            System.out.println(formatted);
        }
    }

    private void viewCourses() {
        ArrayList<Course> courses = model.getCourses();
        if (courses.isEmpty()) {
            System.out.println("No courses in the system!");
            return;
        }

        // headers
        System.out.println("\n--- Courses ---");
        System.out.println("CODE    | PREREQ  | NAME");

        for (Course course : courses) {
            String prereq = course.prereqCode() == null ? "N/A" : course.prereqCode();
            String formatted = String.format(
                    "%s | %-7s | %s",
                    course.code(),
                    prereq,
                    course.name()
            );
            System.out.println(formatted);
        }
    }

    private void viewCourseSections() {
        ArrayList<CourseSection> courseSections = model.getCourseSections();
        if (courseSections.isEmpty()) {
            System.out.println("No sections in the system!");
            return;
        }

        System.out.println("\n--- Course Sections ---");
        System.out.println("CODE    | NAME | SCHEDULE");

        for (CourseSection courseSection : courseSections) {
            String formatted = String.format(
                    "%s | %s | %s",
                    courseSection.code(),
                    courseSection.name(),
                    courseSection.schedule()
            );
            System.out.println(formatted);
        }
    }

    private void viewStudentCourseSections() {
        ArrayList<CourseSection> courseSections = model.getStudentCourseSections();
        if (courseSections.isEmpty()) {
            System.out.println("No sections in the system!");
            return;
        }

        System.out.println("\n--- Your enrolled courses include: ---");
        System.out.println("CODE    | NAME | SCHEDULE");

        for (CourseSection courseSection : courseSections) {
            String formatted = String.format(
                    "%s | %s | %s",
                    courseSection.code(),
                    courseSection.name(),
                    courseSection.schedule()
            );
            System.out.println(formatted);
        }
    }

    private void viewHomeworks() {
        ArrayList<Homework> homeworks = model.getHomeworks();
        if (homeworks.isEmpty()) {
            System.out.println("No homeworks available!");
            return;
        }

        System.out.println("\n--- Homeworks ---");
        System.out.println("COURSE  | DEADLINE   | DESCRIPTION");

        for (Homework homework : homeworks) {
            String formatted = String.format(
                    "%s | %s | %s",
                    homework.courseCode(),
                    homework.deadline(),
                    homework.description()
            );
            System.out.println(formatted);
        }
    }

    private void viewStudentHomeworks() {
        ArrayList<Homework> homeworks = model.getStudentHomeworks();
        if (homeworks.isEmpty()) {
            System.out.println("No homeworks available!");
            return;
        }

        System.out.println("\n--- Homeworks ---");
        System.out.println("COURSE  | DEADLINE   | DESCRIPTION");

        for (Homework homework : homeworks) {
            String formatted = String.format(
                    "%s | %s | %s",
                    homework.courseCode(),
                    homework.deadline(),
                    homework.description()
            );
            System.out.println(formatted);
        }
    }
}
