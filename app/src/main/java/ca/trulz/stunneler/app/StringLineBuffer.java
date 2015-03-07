package ca.trulz.stunneler.app;

import java.util.LinkedList;

public class StringLineBuffer {
    private LinkedList<String> lines;
    private int lineLimit;

    public StringLineBuffer(int lineMax) {
        lineLimit = lineMax;
        lines = new LinkedList<String>();
    }

    public void appendLine(String s) {
        lines.addLast(s);
        if (lines.size() > lineLimit)
            lines.removeLast();
    }

    public String buildString() {
        StringBuilder builder = new StringBuilder();
        for (String s : lines) {
            builder.append(s).append("\n");
        }
        return builder.toString();
    }
}
