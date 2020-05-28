package me.kingjan1999.fhdw.alphacamunda;


import me.kingjan1999.fhdw.alphacamunda.domain.Activity;
import me.kingjan1999.fhdw.alphacamunda.domain.Event;
import me.kingjan1999.fhdw.alphacamunda.domain.Log;
import me.kingjan1999.fhdw.alphacamunda.domain.Trace;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
    static Trace createTraceFromString(String log) {
        var events = log.chars()
                .mapToObj(x -> (char) x)
                .map(x -> new Event(Activity.getActivity(x.toString()), "ignore", Instant.now()))
                .collect(Collectors.toList());
        return new Trace(events);
    }

    static Log createLogFromStrings(String... logs) {
        List<Trace> traces = Arrays.stream(logs).map(Util::createTraceFromString).collect(Collectors.toList());
        return new Log(traces);
    }

}
