package com.app.core.logging;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Native Memory Appender for Log4j2.
 * Stores recent log events in a circular buffer for UI visualization.
 */
@Plugin(name = "MemoryAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class MemoryAppender extends AbstractAppender {

    private static final ConcurrentLinkedQueue<String> LOG_BUFFER = new ConcurrentLinkedQueue<>();
    private final int maxMessages;

    protected MemoryAppender(String name, Filter filter, Layout<? extends Serializable> layout,
            boolean ignoreExceptions, int maxMessages) {
        super(name, filter, layout, ignoreExceptions, null);
        this.maxMessages = maxMessages;
    }

    @PluginFactory
    public static MemoryAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("maxMessages") int maxMessages,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {

        if (name == null) {
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        if (maxMessages <= 0) {
            maxMessages = 100;
        }
        return new MemoryAppender(name, filter, layout, true, maxMessages);
    }

    @Override
    public void append(LogEvent event) {
        String message = new String(getLayout().toByteArray(event));
        LOG_BUFFER.add(message);
        while (LOG_BUFFER.size() > maxMessages) {
            LOG_BUFFER.poll();
        }
    }

    public static List<String> getRecentLogs() {
        return new ArrayList<>(LOG_BUFFER);
    }
}
