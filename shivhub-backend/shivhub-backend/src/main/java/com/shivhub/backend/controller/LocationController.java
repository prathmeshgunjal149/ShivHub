package com.shivhub.backend.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/locations")
public class LocationController {
    private static final Map<String,List<String>> DISTRICTS = Map.of(
        "Maharashtra", List.of("Ahilyanagar","Pune","Mumbai City","Mumbai Suburban","Nashik","Thane","Chhatrapati Sambhajinagar","Nagpur","Solapur","Kolhapur","Satara","Sangli","Jalgaon","Nanded","Amravati","Raigad","Palghar","Beed","Latur","Dharashiv","Parbhani","Hingoli","Jalna","Bhandara","Gondia","Chandrapur","Gadchiroli","Wardha","Yavatmal","Akola","Buldhana","Washim","Ratnagiri","Sindhudurg","Dhule","Nandurbar"),
        "Karnataka", List.of("Bengaluru Urban","Mysuru","Belagavi"),
        "Gujarat", List.of("Ahmedabad","Surat","Vadodara")
    );
    private static final Map<String,List<String>> CITIES = Map.of(
        "Maharashtra|Ahilyanagar", List.of("Sangamner","Ahmednagar","Shirdi","Rahata","Akole","Kopargaon","Shrirampur","Nevasa","Pathardi","Shevgaon","Karjat","Jamkhed","Parner","Rahuri","Nagar"),
        "Maharashtra|Pune", List.of("Pune","Pimpri-Chinchwad","Baramati","Chakan","Khed","Junnar","Talegaon Dabhade","Lonavala","Daund","Indapur","Saswad","Shirur","Haveli")
    );
    @GetMapping("/states") public List<String> states() { return List.of("Maharashtra","Karnataka","Gujarat","Goa","Madhya Pradesh","Rajasthan","Delhi"); }
    @GetMapping("/districts") public List<String> districts(@RequestParam String state) { return DISTRICTS.getOrDefault(state, List.of()); }
    @GetMapping("/cities") public List<String> cities(@RequestParam String state, @RequestParam String district) { List<String> values=CITIES.getOrDefault(state+"|"+district, List.of()); List<String> result=new ArrayList<>(values); result.add("Other"); return result; }
}
