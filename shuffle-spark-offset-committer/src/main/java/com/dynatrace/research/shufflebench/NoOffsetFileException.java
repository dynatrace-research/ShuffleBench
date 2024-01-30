package com.dynatrace.research.shufflebench;

public class NoOffsetFileException extends RuntimeException {


    public NoOffsetFileException(String message) {
        super(message);
    }

    public NoOffsetFileException(String message, Throwable cause) {
        super(message, cause);
    }

}
