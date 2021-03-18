package gov.dhs.cisa.flare.misp.util;

import gov.dhs.cisa.flare.misp.Config;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

public class MapSoreUtil {

    public static void sortByKeyUsingTreeMap(Map<String, String> unSortedMap) {
        Map<String, String> sortedMap = new TreeMap<String, String>(unSortedMap);
        System.out.println("11111111111111"+sortedMap);
        //Map<String, String> reverseSortedMap = new TreeMap<String, String>(Collections.reverseOrder());
        //System.out.println("22222222222"+reverseSortedMap);

        //return reverseSortedMap.putAll(unSortedMap);
    }

    public static void main(String[] args) {

        HashMap<String, String> map = new HashMap<>();
        map.put("Controller","refreshConfig");
        map.put("Step 1 of 5","Attempt to STOP Quartz Jobs...");
        map.put("Step 2 of 5","Attempt to STOP Quartz Scheduler...");
        map.put("Step 3 of 5","Attempt to reload Configuration Properties...");
        map.put("Step 4 of 5 ","Reset Begin/End TimeStamps. Will be initialized via Configuration Properties...");
        map.put("Step 5 of 5","Attempt to ReSTART the Quartz Scheduler...");

        System.out.println(map);
        sortByKeyUsingTreeMap(map);
    }


}
