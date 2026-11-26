package edu.bu.met.wildmancs763.discussions.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {

    public String clean(String input) {
        return Jsoup.clean(input, Safelist.none()).trim();
    }
}
