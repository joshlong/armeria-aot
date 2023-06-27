package com.example.armeriaftw;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.logging.AccessLogWriter;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.spring.ArmeriaServerConfigurator;
import com.linecorp.armeria.spring.HealthCheckServiceConfigurator;
import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import sun.misc.Unsafe;

import java.util.stream.Stream;

@SpringBootApplication
@ImportRuntimeHints(ArmeriaftwApplication.LineRuntimeHintsRegistrar.class)
@RegisterReflectionForBinding(
        classes = {

                UnsafeAccess.class,
                Unsafe.class,
                com.linecorp.armeria.internal.shaded.jctools.maps.NonBlockingHashMap.class})
public class ArmeriaftwApplication {


    public static void main(String[] args) {
        SpringApplication.run(ArmeriaftwApplication.class, args);
    }

    static class LineRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

        private final Logger log = LoggerFactory.getLogger(getClass());

        static void registerFor(String pkg, Class<?> parent, RuntimeHints hints) throws Exception {
            new Reflections(pkg).getSubTypesOf(parent).forEach(c -> hints.reflection().registerType(c, MemberCategory.values()));
        }

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            try {
                var caffeinePackage = "com.linecorp.armeria.internal.shaded.caffeine";
                registerFor(caffeinePackage, Class.forName("com.linecorp.armeria.internal.shaded.caffeine.cache.BoundedLocalCache"), hints);
                registerFor(caffeinePackage, Class.forName("com.linecorp.armeria.internal.shaded.caffeine.cache.NodeFactory"), hints);
                Stream.of(
                                "io.micrometer.prometheus.PrometheusMeterRegistry" ,
                                "io.prometheus.client.CollectorRegistry" ,
                                HealthCheckServiceConfigurator.class.getName() ,
                                "com.codahale.metrics.json.MetricsModule" ,
                                "com.codahale.metrics.MetricRegistry",
                                io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry.class.getName() ,
                                Thread.class.getName(),
                                "com.linecorp.armeria.internal.shaded.caffeine.cache.StripedBuffer",
                                "com.linecorp.armeria.internal.shaded.caffeine.cache.BLCHeader$DrainStatusRef")
                        .forEach(t -> hints.reflection().registerType(TypeReference.of(t), MemberCategory.values()));
            } //
            catch (Exception e) {
                log.error("couldn't process the hints ", e);
            }
        }
    }

    @Bean
    ArmeriaServerConfigurator armeriaServerConfigurator() {
        // Customize the server using the given ServerBuilder. For example:
        return builder -> {
            // Add DocService that enables you to send Thrift and gRPC requests
            // from web browser.
//            builder.serviceUnder("/docs", new DocService());

            builder.service("/hello", (ctx, req) -> HttpResponse.of("Hello world!"));
            builder.decorator(LoggingService.newDecorator());
            builder.accessLogWriter(AccessLogWriter.combined(), false);

            // You can also bind asynchronous RPC services such as Thrift and gRPC:
            // builder.service(THttpService.of(...));
            // builder.service(GrpcService.builder()...build());
        };
    }
}