import java.util.concurrent.StructuredTaskScope;
import java.lang.reflect.Method;

public class Probe {
    public static void main(String[] args) {
        Class<?> cls = StructuredTaskScope.Joiner.class;
        System.out.println("Methods of StructuredTaskScope.Joiner:");
        for (Method m : cls.getMethods()) {
            System.out.println(" - " + m);
        }
    }
}
