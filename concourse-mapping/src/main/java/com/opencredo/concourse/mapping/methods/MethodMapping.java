package com.opencredo.concourse.mapping.methods;

import com.opencredo.concourse.data.tuples.Tuple;
import com.opencredo.concourse.data.tuples.TupleKey;
import com.opencredo.concourse.data.tuples.TupleKeyValue;
import com.opencredo.concourse.data.tuples.TupleSchema;
import com.opencredo.concourse.domain.AggregateId;
import com.opencredo.concourse.domain.StreamTimestamp;
import com.opencredo.concourse.domain.VersionedName;
import com.opencredo.concourse.domain.events.Event;
import com.opencredo.concourse.domain.events.EventType;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class MethodMapping {

    public static MethodMapping forMethod(Method method) {
        checkNotNull(method, "method must not be null");

        Class<?> klass = method.getDeclaringClass();

        final String aggregateType = EventInterfaceReflection.getAggregateType(klass);
        final VersionedName eventName = EventInterfaceReflection.getEventName(method);

        ParameterArgs parameterArgs = ParameterArgs.forMethod(method);
        TupleSchema schema = parameterArgs.getTupleSchema(EventType.of(aggregateType, eventName).toString());
        TupleKey[] tupleKeys = parameterArgs.getTupleKeys(schema);

        return new MethodMapping(
                aggregateType,
                eventName,
                schema,
                tupleKeys);
    }

    private final String aggregateType;
    private final VersionedName eventName;
    private final TupleSchema tupleSchema;
    private final TupleKey[] tupleKeys;

    private MethodMapping(String aggregateType, VersionedName eventName, TupleSchema tupleSchema, TupleKey[] tupleKeys) {
        this.aggregateType = aggregateType;
        this.eventName = eventName;
        this.tupleSchema = tupleSchema;
        this.tupleKeys = tupleKeys;
    }

    public Event mapArguments(Object[] args) {
        checkNotNull(args, "args must not be null");
        checkArgument(args.length == tupleKeys.length + 2,
                "Expected %s args, received %s", tupleKeys.length +2, args.length);

        return Event.of(
                AggregateId.of(aggregateType, (UUID) args[1]),
                (StreamTimestamp) args[0],
                eventName,
                makeTupleFromArgs(args)
        );
    }

    public EventType getEventType() {
        return EventType.of(aggregateType, eventName);
    }

    private Tuple makeTupleFromArgs(Object[] args) {
        return tupleSchema.make(IntStream.range(0, tupleKeys.length)
                .mapToObj(getValueFrom(args))
                .toArray(TupleKeyValue[]::new));
    }

    public Object[] mapEvent(Event event) {
        checkNotNull(event, "event must not be null");

        Object[] args = new Object[tupleKeys.length + 2];
        args[0] = event.getEventTimestamp();
        args[1] = event.getAggregateId().getId();

        populateArgsFromTuple(event, args);

        return args;
    }

    private void populateArgsFromTuple(Event event, Object[] args) {
        IntStream.range(0, tupleKeys.length).forEach(i ->
            args[i + 2] = event.getParameters().get(tupleKeys[i]));
    }

    @SuppressWarnings("unchecked")
    private IntFunction<TupleKeyValue> getValueFrom(Object[] args) {
        return i -> tupleKeys[i].of(args[i + 2]);
    }
}