package com.googlecode.yadic;

import com.googlecode.totallylazy.functions.Function1;
import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Sequence;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.totallylazy.Arrays.empty;
import static com.googlecode.totallylazy.functions.Callables.callThrows;
import static com.googlecode.totallylazy.reflection.Methods.genericParameterTypes;
import static com.googlecode.totallylazy.reflection.Methods.genericReturnType;
import static com.googlecode.totallylazy.reflection.Methods.modifier;
import static com.googlecode.totallylazy.Option.none;
import static com.googlecode.totallylazy.Option.some;
import static com.googlecode.totallylazy.predicates.Predicates.not;
import static com.googlecode.totallylazy.predicates.Predicates.where;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.reflection.Types.matches;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

public class TypeCoercer implements Resolver<Object> {
    private final Object instance;

    public TypeCoercer(Object instance) {
        this.instance = instance;
    }

    public Object resolve(Type type) throws Exception {
        Class concreteClass = instance.getClass();
        Sequence<Method> methods = sequence(concreteClass.getMethods()).
                filter(modifier(PUBLIC).and(
                        not(modifier(STATIC))).and(
                        where(genericReturnType(), matches(type))).and(
                        where(genericParameterTypes(), empty())));

        if (methods.isEmpty()) {
            throw new ContainerException(concreteClass.getName() + " does not have any public instance methods that return " + type);
        }

        final List<Exception> exceptions = new ArrayList<Exception>();
        return methods.tryPick(firstSatisfiableMethod(exceptions)).
                getOrElse(callThrows(new ContainerException(concreteClass.getName() + " does not have any callable public instance method that return " + type, exceptions)));
    }

    private Function1<Method, Option<Object>> firstSatisfiableMethod(final List<Exception> exceptions) {
        return method -> {
            try {
                return some(method.invoke(instance));
            } catch (Exception e) {
                exceptions.add(e);
                return none();
            }
        };
    }
}
