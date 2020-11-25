import java.io.IOException;
import java.lang.Runtime;

class Main {

    public static void main(String args[]) {
        try {
            Process p = Runtime.getRuntime().exec("python3 /Users/scottnerlino/projects/flare-misp-service/misp-trans-client-rest-service/scripts/misp_stix_ingest/test.py");
            System.out.println("Comp");
            p.waitFor();
            System.out.println("Comp2");
        } catch (IOException | InterruptedException e) {
            System.out.println("IO Err");
        }
    }
}