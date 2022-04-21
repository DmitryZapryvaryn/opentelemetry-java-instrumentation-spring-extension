package org.zaprydz.instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import com.google.gson.Gson;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.internal.SpanKey;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.matcher.ElementMatcher;

public class SpringControllerInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return isAnnotatedWith(
        namedOneOf(
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Controller"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(isPublic()).and(not(takesNoArguments())),
        this.getClass().getName() + "$ControllerMethodAdvice");
  }

  @SuppressWarnings("unused")
  public static class ControllerMethodAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Origin Method originMethod,
        @Advice.Local("otelMethod") Method method,
        @Advice.AllArguments(typing = Typing.DYNAMIC) Object[] args) {

      // Every usage of @Advice.Origin Method is replaced with a call to Class.getMethod, copy it
      // to local variable so that there would be only one call to Class.getMethod.
      method = originMethod;

      Context context = Java8BytecodeBridge.currentContext();
      if (context == null) {
        System.out.println("------------------!ALARM!-----------------------------");
        System.out.println("Context is null");
        System.out.println("------------------!ALARM!-----------------------------");
        return;
      }

      Span span = SpanKey.SERVER.fromContextOrNull(context);
      if (span == null) {
        System.out.println("------------------!ALARM!-----------------------------");
        System.out.println("Span is null");
        System.out.println("------------------!ALARM!-----------------------------");
        return;
      }

      Parameter[] parameters = method.getParameters();
      if (parameters.length != args.length) {
        System.out.println("------------------!ALARM!-----------------------------");
        System.out.println("Params lengths are not the same");
        System.out.println("------------------!ALARM!-----------------------------");

        return;
      }

      for (int i = 0; i < args.length; ++i) {
        Parameter parameter = parameters[i];
        Object arg = args[i];
        String paramName = "param." + i;
        if (parameter.isNamePresent()) {
          paramName = "param." + parameter.getName();
        }
        span.setAttribute(paramName, new Gson().toJson(arg));
      }
    }
  }
}
