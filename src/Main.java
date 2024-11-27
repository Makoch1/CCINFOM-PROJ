import java.util.ArrayList;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        Model md = new Model();

        View view = new View(md);
        view.start();
    }
}
