package pl.org.radical.alarms.aop;

import pl.org.radical.alarms.AlarmSender;

import javax.annotation.Resource;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * This is an AspectJ aspect that will intercept unhandled runtime exceptions in any method or class
 * annotated with @AlarmOnException, sending an alarm with the exception class, message and optionally
 * a partial or full stack trace.
 * 
 * @author Enrique Zamudio
 */
@Aspect
public class AlarmAspect {

	private String message = "Exception thrown!";
	private AlarmSender alarm;
	private int includeStack = 0;

	/**
	 * You can set if the alarm message should include the stack trace, in number of lines
	 * (0 means don't include the stack trace, -1 means include the whole thing).
	 */
	public void setIncludeStackTrace(final int lines) {
		includeStack = lines;
	}

	/**
	 * Specifies the message to be used in the alarm. The alarm will have this message, then the
	 * exception class, the exception message (if any) and the stack trace if specified.
	 */
	public void setMessage(final String value) {
		message = value;
	}

	@Resource
	public void setAlarmSender(final AlarmSender value) {
		alarm = value;
	}

	private void appendStackTrace(final StringBuilder buf, final Throwable t, final int lines) {
		if (lines == -1) {
			for (final StackTraceElement elem : t.getStackTrace()) {
				buf.append('\n');
				buf.append(elem.toString());
			}
		} else if (lines > 0) {
			int count = 0;
			final StackTraceElement[] stack = t.getStackTrace();
			while (count < lines && count < stack.length) {
				buf.append('\n');
				buf.append(stack[count++].toString());
			}
		}
	}

	@Pointcut("@within(com.solab.alarms.aop.AlarmOnException)")
	public void classPointcut() {
	}

	@Pointcut("@annotation(com.solab.alarms.aop.AlarmOnException)")
	public void methodPointcut() {
	}

	@AfterThrowing(pointcut = "com.solab.alarms.aop.AlarmAspect.classPointcut() && @within(alarmOnException)", throwing = "ex")
	public void sendClassAlarm(final RuntimeException ex, final AlarmOnException alarmOnException) {
		sendMethodAlarm(ex, alarmOnException);
	}

	@AfterThrowing(pointcut = "com.solab.alarms.aop.AlarmAspect.methodPointcut() && @annotation(alarmOnException)", throwing = "ex")
	public void sendMethodAlarm(final RuntimeException ex, final AlarmOnException alarmOnException) {
		final String ann_msg = alarmOnException != null && alarmOnException.message() != null && alarmOnException.message().length() > 0 ? alarmOnException
		        .message() : null;
		final int lineas = alarmOnException != null ? alarmOnException.stack() : includeStack;
		final StringBuilder buf = new StringBuilder(ann_msg == null ? message : ann_msg);
		buf.append(' ').append(ex.getClass().getName());
		if (ex.getMessage() != null) {
			buf.append(": ").append(ex.getMessage());
		}
		if (ex.getCause() == null) {
			appendStackTrace(buf, ex, lineas);
		} else {
			final Throwable cause = ex.getCause();
			buf.append("(Caused by ").append(cause.getClass().getName());
			if (cause.getMessage() != null) {
				buf.append(": ").append(cause.getMessage());
			}
			buf.append(')');
			appendStackTrace(buf, cause, lineas);
		}
		final String src = alarmOnException != null && alarmOnException.source() != null && alarmOnException.source().length() > 0 ? alarmOnException
		        .source() : null;
		alarm.sendAlarm(buf.toString(), src);
	}

}
