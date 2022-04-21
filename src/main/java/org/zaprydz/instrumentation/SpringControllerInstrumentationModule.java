package org.zaprydz.instrumentation;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher.Junction;

@AutoService(InstrumentationModule.class)
public class SpringControllerInstrumentationModule extends InstrumentationModule {

  public SpringControllerInstrumentationModule() {
    super("spring-controller", "spring-controller-1.0");
  }

  @Override
  public int order() {
    return 1;
  }

  @Override
  public Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed(
        "org.springframework.web.bind.annotation.RestController",
        "org.springframework.stereotype.Controller");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new SpringControllerInstrumentation());
  }
}
