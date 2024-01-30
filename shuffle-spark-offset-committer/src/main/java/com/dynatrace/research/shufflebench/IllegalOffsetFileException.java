package com.dynatrace.research.shufflebench;

public class IllegalOffsetFileException extends RuntimeException {


    public IllegalOffsetFileException(String message) {
        super(message);
    }

    public IllegalOffsetFileException(String message, Throwable cause) {
        super(message, cause);
    }

}
