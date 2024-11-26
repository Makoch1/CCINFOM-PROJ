public record Student(String idNumber, String firstName, String middleName, String lastName) {
    public Student {
        if (idNumber.length() != 8) {
            throw new IllegalArgumentException();
        }
    }
}
