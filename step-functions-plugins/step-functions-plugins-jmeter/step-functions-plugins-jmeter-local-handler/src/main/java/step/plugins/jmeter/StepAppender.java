package step.plugins.jmeter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;

import java.io.ByteArrayOutputStream;

public class StepAppender extends OutputStreamAppender<ILoggingEvent> {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final Logger logger;

    public StepAppender(Logger logger) {
        super();
        this.logger = logger;

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(logger.getLoggerContext());
        encoder.setPattern("%d %p %c{1.}: %m%n");
        encoder.start();

        this.setName("STEP capture");
        this.setContext(logger.getLoggerContext());
        this.setEncoder(encoder);
        this.setOutputStream(out);

        this.start();
        logger.addAppender(this);
    }

    public void dispose() {
        this.stop();
        logger.detachAppender(this);
    }

    public byte[] getData() {
        return out.toByteArray();
    }
}
