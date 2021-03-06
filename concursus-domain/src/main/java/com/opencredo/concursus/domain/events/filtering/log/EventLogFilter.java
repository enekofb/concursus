package com.opencredo.concursus.domain.events.filtering.log;

import com.opencredo.concursus.domain.events.filtering.Filters;
import com.opencredo.concursus.domain.events.logging.EventLog;

import java.util.Arrays;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface EventLogFilter extends UnaryOperator<EventLog> {

    static EventLogFilter compose(EventLogFilter...filters) {
        return Filters.compose(Arrays.asList(filters))::apply;
    }

}
