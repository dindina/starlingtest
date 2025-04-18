package com.test.dto;

import java.time.LocalDate;

public class StatementRequest {
    private LocalDate start;
    private LocalDate end;

    public StatementRequest(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "StatementRequest{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}