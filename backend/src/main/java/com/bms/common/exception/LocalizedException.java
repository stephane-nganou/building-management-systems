package com.bms.common.exception;

/**
 * An error whose wording belongs to the caller, not to us. It carries a message
 * code and its arguments rather than a finished sentence, so the same failure
 * can be reported in whichever language the request asked for.
 *
 * <p>The code doubles as the exception's own message, which is what shows up in
 * a stack trace or a log line.
 */
public abstract class LocalizedException extends RuntimeException {

    private final String code;
    private final transient Object[] args;

    protected LocalizedException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
